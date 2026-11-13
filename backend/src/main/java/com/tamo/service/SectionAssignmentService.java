package com.tamo.service;

import com.google.gson.*;
import com.tamo.model.*;
import com.tamo.model.Module;
import com.tamo.util.JsonUtil;
import java.time.Clock;
import java.util.*;

/** Mutations run inside the shared database transaction/advisory lock. */
public class SectionAssignmentService {
    private final JsonDataService data;
    private final Clock clock;
    public SectionAssignmentService(JsonDataService data) { this(data, Clock.systemUTC()); }
    public SectionAssignmentService(JsonDataService data, Clock clock) { this.data=data; this.clock=clock; }
    public static class Problem extends RuntimeException {
        public final int status;
        public Problem(int status,String message) { super(message); this.status=status; }
    }
    private void staff(String role) {
        if (!"admin".equals(role)) throw new Problem(403,"Only admin can manage assignments.");
    }
    private User reviewStaff(String user, String role) {
        User actor=data.getData().getUsers().stream().filter(u->Objects.equals(user,u.getId())&&"active".equals(u.getStatus())&&Objects.equals(role,u.getRole())).findFirst().orElseThrow(()->new Problem(403,"An active authorised staff account is required."));
        if (!ReviewAccess.can(actor,"review.read")) throw new Problem(403,"Current application review permission is required.");
        return actor;
    }
    private Module module(String user,String role,String id) {
        staff(role); User actor=reviewStaff(user,role);
        Module m=data.getData().getModules().stream().filter(x->Objects.equals(id,x.getId())).findFirst().orElseThrow(()->new Problem(404,"Course not found."));
        if (!ReviewAccess.canWrite(actor,m,ReviewAccess.MANAGE_ASSIGNMENTS)) throw new Problem(403,"Assignment management access is required for this course.");
        return m;
    }
    private CourseSection section(String id) {
        return data.getData().getCourseSections().stream().filter(s->Objects.equals(id,s.getId())).findFirst().orElseThrow(()->new Problem(404,"Section not found."));
    }
    private Job job(Application a) {
        return data.getData().getJobs().stream().filter(j->Objects.equals(a.getJobId(),j.getId())).findFirst().orElseThrow(()->new Problem(404,"Position not found."));
    }
    public static boolean activeForApplication(DataStore ds,String id) {
        return ds.getSectionAssignments().stream().anyMatch(a->"active".equals(a.getStatus()) && Objects.equals(id,a.getApplicationId()));
    }
    public static boolean activeForSection(DataStore ds,String id) {
        return ds.getSectionAssignments().stream().anyMatch(a->"active".equals(a.getStatus()) && Objects.equals(id,a.getSectionId()));
    }
    public static boolean hasHistory(DataStore ds,String collection,String id) {
        for(ReviewRecord review:ds.getReviewRecords()){
            if(collection.equals("users")&&Objects.equals(id,review.getActorId()))return true;
            Application app=ds.getApplications().stream().filter(a->Objects.equals(a.getId(),review.getApplicationId())).findFirst().orElse(null);
            if(app!=null&&(collection.equals("applications")&&id.equals(app.getId())||collection.equals("users")&&id.equals(app.getApplicantId())||collection.equals("jobs")&&id.equals(app.getJobId())||collection.equals("modules")&&ds.getJobs().stream().anyMatch(j->Objects.equals(j.getId(),app.getJobId())&&Objects.equals(j.getModuleId(),id))))return true;
        }

        return ds.getSectionAssignments().stream().anyMatch(a -> switch(collection) {
            case "applications" -> Objects.equals(id,a.getApplicationId());
            case "users" -> Objects.equals(id,a.getApplicantId())||Objects.equals(id,a.getAssignedBy())||Objects.equals(id,a.getPublishedBy())||Objects.equals(id,a.getCancelledBy());
            case "modules" -> Objects.equals(id,a.getSectionSnapshot().getModuleId());
            case "jobs" -> ds.getApplications().stream().anyMatch(app->Objects.equals(a.getApplicationId(),app.getId()) && Objects.equals(id,app.getJobId()));
            default -> false;
        });
    }
    public double weeklyHours(String applicant,String period) {
        return data.getData().getSectionAssignments().stream().filter(a->"active".equals(a.getStatus()) && Objects.equals(applicant,a.getApplicantId()) && Objects.equals(period,a.getPeriodId()))
                .mapToDouble(a->a.getSectionSnapshot().getHoursPerWeek()).sum();
    }
    public double acceptedWeeklyHours(String applicant,String period) {
        return data.getData().getSectionAssignments().stream().filter(a->"active".equals(a.getStatus()) && "accepted".equals(a.getResponseStatus()) && Objects.equals(applicant,a.getApplicantId()) && Objects.equals(period,a.getPeriodId()))
                .mapToDouble(a->a.getSectionSnapshot().getHoursPerWeek()).sum();
    }
    private double publishedWeeklyHours(String applicant,String period) {
        return data.getData().getSectionAssignments().stream().filter(a->"active".equals(a.getStatus()) && "published".equals(a.getPublicationStatus()) && Objects.equals(applicant,a.getApplicantId()) && Objects.equals(period,a.getPeriodId()))
                .mapToDouble(a->a.getSectionSnapshot().getHoursPerWeek()).sum();
    }
    private SectionAssignment assignment(String id) {
        return data.getData().getSectionAssignments().stream().filter(a->Objects.equals(id,a.getId())).findFirst().orElseThrow(()->new Problem(404,"Assignment not found."));
    }
    public SectionAssignment publish(String user,String role,String id,JsonObject body) {
        staff(role); SectionAssignment a=assignment(id);
        Module course=module(user,role,section(a.getSectionId()).getModuleId());
        if (!"active".equals(a.getStatus()) || !"draft".equals(a.getPublicationStatus())) throw new Problem(409,"Only an active draft can be published.");
        if(a.getApplicationId()!=null) {
            Application app=data.getData().getApplications().stream().filter(x->Objects.equals(a.getApplicationId(),x.getId())).findFirst().orElseThrow(()->new Problem(409,"Application no longer exists."));
            if (!JobSectionService.includes(job(app),section(a.getSectionId()))) throw new Problem(409,"This Section is not included in the TA posting.");
            if(!ReviewService.approvedForSection(data.getData(),app.getId(),a.getSectionId())) throw new Problem(409,"An authorized selection approval is required before assignment.");
            if (!"offer".equals(app.getStatus())) throw new Problem(409,"Application must still be at the offer stage.");
        } else {
            CourseSection section=section(a.getSectionId());
            requireActiveApplicant(a.getApplicantId());
            requireDirectAvailability(a.getApplicantId(),section,a.getManualConfirmationNote());
        }
        String recipientEmail=data.getData().getUsers().stream()
                .filter(u->Objects.equals(a.getApplicantId(),u.getId()))
                .map(User::getEmail).filter(email->email!=null&&!email.isBlank()).findFirst()
                .orElseThrow(()->new Problem(409,"Add an email address to the Applicant account before publishing."));
        String directReason=a.getDirectReason();
        a.setPublicationStatus("published");a.setPublishedBy(user);a.setPublishedAt(clock.instant().toString());
        data.getData().getOfferEmails().add(new OfferEmail(a.getId(),recipientEmail,
                Objects.toString(course.getCode(),""),Objects.toString(course.getName(),""),
                a.getSectionSnapshot().getName(),"/applicant/assignments.jsp?assignmentId="+a.getId(),
                a.getPublishedAt()));
        notifyUser(a.getApplicantId(),"assignment_published","TA assignment ready for your reply",
                "Your assignment for "+a.getSectionSnapshot().getName()+" has been published. Review its meetings and weekly hours in My TA Placement, then accept or decline.",a.getId());
        data.save();auditDirectAssignment(user,a,"Published",directReason);return a;
    }
    public SectionAssignment respond(String user,String role,String id,JsonObject body) {
        if (!"applicant".equals(role)) throw new Problem(403,"Only the assigned applicant can respond.");
        SectionAssignment a=assignment(id);
        // Unpublished rows are not visible to applicants, even if an ID is guessed.
        if (!Objects.equals(user,a.getApplicantId()) || !"published".equals(a.getPublicationStatus())) throw new Problem(404,"Published assignment not found.");
        if (!"active".equals(a.getStatus()) || !"pending".equals(a.getResponseStatus())) throw new Problem(409,"This assignment is no longer awaiting a reply.");
        String decision=text(body,"decision",true);
        if (!Set.of("accept","decline").contains(decision)) throw new Problem(400,"Choose accept or decline.");
        String reason="decline".equals(decision)?text(body,"reason",true):"";
        a.setResponseStatus("accept".equals(decision)?"accepted":"declined");a.setRespondedAt(clock.instant().toString());a.setResponseReason(reason);
        if ("decline".equals(decision)) {
            a.setStatus("cancelled");a.setCancelledBy(user);a.setCancelledAt(a.getRespondedAt());a.setCancellationReason(reason);
        }
        if(data.getData().getUsers().stream().anyMatch(u->Objects.equals(a.getPublishedBy(),u.getId())
                && "admin".equals(u.getRole()) && "active".equals(u.getStatus()))) {
            notifyUser(a.getPublishedBy(),"assignment_response","TA assignment "+a.getResponseStatus(),
                    "The Applicant "+a.getResponseStatus()+" the assignment for "+a.getSectionSnapshot().getName()+".",a.getId());
        }
        data.save();return a;
    }
    private void notifyUser(String user,String type,String title,String message,String relatedId) {
        Notification n=new Notification();n.setId("notification_"+UUID.randomUUID().toString().replace("-",""));n.setUserId(user);n.setType(type);
        n.setTitle(title);n.setMessage(message);n.setRelatedId(relatedId);n.setCreatedAt(clock.instant().toString());n.setRead(false);
        data.getData().getNotifications().add(n);
    }
    public JsonObject studentView(SectionAssignment a) { return row(a,false); }
    public JsonObject staffView(SectionAssignment a) { return row(a,true); }
    public long occupied(String section,String period) {
        return data.getData().getSectionAssignments().stream().filter(a->"active".equals(a.getStatus()) && Objects.equals(section,a.getSectionId()) && Objects.equals(period,a.getPeriodId())).count();
    }
    public SectionAssignment assign(String user,String role,JsonObject body) {
        staff(role);
        String appId=text(body,"applicationId",true), sectionId=text(body,"sectionId",true);
        Application app=data.getData().getApplications().stream().filter(a->appId.equals(a.getId())).findFirst().orElseThrow(()->new Problem(404,"Application not found."));
        CourseSection s=section(sectionId); Module m=module(user,role,s.getModuleId()); Job job=job(app);
        if (!JobSectionService.includes(job,s)) throw new Problem(409,"This Section is not included in the TA posting.");
        if(!Objects.equals(s.getPeriodId(),app.getPeriodId())&&!(s.getPeriodId()==null&&"period_legacy".equals(app.getPeriodId())))throw new Problem(409,"Section and application must belong to the same application cycle.");
        if (!Objects.equals(job.getModuleId(),s.getModuleId())) throw new Problem(403,"Application must belong to this course and its current Teacher.");
        if(!ReviewService.approvedForSection(data.getData(),app.getId(),s.getId()))throw new Problem(409,"An authorized selection approval is required before assignment.");
        if (!"offer".equals(app.getStatus())) throw new Problem(409,"Only offered applications can be assigned.");
        JsonObject matching=new SectionMatchingService().match(data.getData(),app,job,m.getOrganiserId());
        String status="pending";
        for (JsonElement row:matching.getAsJsonArray("sections")) {
            JsonObject match=row.getAsJsonObject();
            if(s.getId().equals(match.get("sectionId").getAsString())) status=match.get("status").getAsString();
        }
        return draft(user,app.getId(),app.getApplicantId(),app.getPeriodId(),s,status,body,false);
    }

    public SectionAssignment assignDirect(String user,String role,JsonObject body) {
        staff(role);
        CourseSection section=section(text(body,"sectionId",true));
        module(user,role,section.getModuleId());
        String applicantId=text(body,"applicantId",true);
        requireActiveApplicant(applicantId);
        if(section.getPeriodId()==null || !section.getPeriodId().equals(text(body,"periodId",true)))
            throw new Problem(409,"Choose a Section in the selected application period.");
        String status=directAvailability(applicantId,section);
        return draft(user,null,applicantId,section.getPeriodId(),section,status,body,true);
    }

    private void requireActiveApplicant(String id) {
        if(data.getData().getUsers().stream().noneMatch(u->Objects.equals(id,u.getId())
                && "applicant".equals(u.getRole()) && "active".equals(u.getStatus())))
            throw new Problem(409,"Choose an active Applicant account.");
    }

    private String directAvailability(String applicantId,CourseSection section) {
        WeeklyAvailability schedule=data.getData().getWeeklyAvailabilities().stream()
                .filter(a->Objects.equals(applicantId,a.getApplicantId())&&Objects.equals(section.getPeriodId(),a.getPeriodId()))
                .findFirst().orElse(null);
        return new SectionMatchingService().availabilityStatus(schedule,applicantId,section);
    }

    private void requireDirectAvailability(String applicantId,CourseSection section,String note) {
        String status=directAvailability(applicantId,section);
        if("conflict".equals(status)) throw new Problem(409,"Saved availability conflicts with this Section.");
        if("pending".equals(status)&&(note==null||note.length()<10))
            throw new Problem(409,"Availability needs confirmation. Cancel this draft and prepare a new one with a confirmation note.");
    }

    private SectionAssignment draft(String user,String applicationId,String applicantId,String periodId,
                                    CourseSection section,String status,JsonObject body,boolean direct) {
        DataStore ds=data.getData();
        if (!"active".equals(section.getStatus())) throw new Problem(409,"Restore the section before assigning a TA.");
        if (periodId==null || ds.getApplicationPeriods().stream().noneMatch(p->periodId.equals(p.getId())))
            throw new Problem(409,"Application period is missing.");
        if (occupied(section.getId(),periodId)>=section.getTaPositions()) throw new Problem(409,"No TA positions remain for this section in this period.");
        if ("conflict".equals(status)) throw new Problem(409,"Applicant availability conflicts with this section. Choose a compatible section.");
        String note=text(body,"manualConfirmationNote",false);
        boolean confirmed=body.has("confirmPending") && body.get("confirmPending").isJsonPrimitive()
                && body.getAsJsonPrimitive("confirmPending").isBoolean() && body.get("confirmPending").getAsBoolean();
        if ("pending".equals(status) && (!confirmed || note.length()<10))
            throw new Problem(409,"Availability needs confirmation. Confirm it manually and provide a note of at least 10 characters.");
        for (SectionAssignment existing:ds.getSectionAssignments()) {
            if (!"active".equals(existing.getStatus()) || !applicantId.equals(existing.getApplicantId()) || !periodId.equals(existing.getPeriodId())) continue;
            if(section.getId().equals(existing.getSectionId())) throw new Problem(409,"This applicant is already assigned to this section in this period.");
            if (overlaps(section,existing.getSectionSnapshot())) throw new Problem(409,"Meeting times overlap another active assignment in this application period.");
        }
        String reason=direct?text(body,"reason",true):null;
        SectionAssignment assignment=new SectionAssignment();
        assignment.setId("assignment_"+UUID.randomUUID().toString().replace("-",""));
        assignment.setApplicationId(applicationId);
        assignment.setDirectReason(reason);
        assignment.setApplicantId(applicantId);
        assignment.setSectionId(section.getId());
        assignment.setPeriodId(periodId);
        assignment.setStatus("active");
        assignment.setAssignedBy(user);
        assignment.setAssignedAt(clock.instant().toString());
        assignment.setManualConfirmationNote(confirmed?note:"");
        assignment.setSectionSnapshot(JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(section),CourseSection.class));
        ds.getSectionAssignments().add(assignment);
        data.save();
        auditDirectAssignment(user,assignment,"Drafted",reason);
        return assignment;
    }
    private void auditDirectAssignment(String user,SectionAssignment assignment,String action,String reason) {
        if(reason!=null) data.addLog("Direct TA Assignment","info",null,user,
                action+" assignment "+assignment.getId()+" for section "+assignment.getSectionId()+" | reason: "+reason,null);
    }
    private boolean overlaps(CourseSection a,CourseSection b) {
        if (b==null || b.getMeetingTimes()==null || a.getMeetingTimes()==null || !Objects.equals(a.getTimeZone(),b.getTimeZone())) throw new Problem(409,"Existing schedule needs review before another assignment can be made.");
        for (var x:a.getMeetingTimes()) for (var y:b.getMeetingTimes())
            if (x.getDayOfWeek()==y.getDayOfWeek() && x.getStartTime().compareTo(y.getEndTime())<0 && y.getStartTime().compareTo(x.getEndTime())<0) return true;
        return false;
    }
    public SectionAssignment cancel(String user,String role,String id,JsonObject body) {
        staff(role);
        SectionAssignment a=data.getData().getSectionAssignments().stream().filter(x->Objects.equals(id,x.getId())).findFirst().orElseThrow(()->new Problem(404,"Assignment not found."));
        module(user,role,section(a.getSectionId()).getModuleId());
        if (!"active".equals(a.getStatus())) throw new Problem(409,"This assignment has already been cancelled.");
        String reason=text(body,"reason",true);
        a.setStatus("cancelled"); a.setCancelledBy(user); a.setCancelledAt(clock.instant().toString()); a.setCancellationReason(reason);
        if ("published".equals(a.getPublicationStatus())) notifyUser(a.getApplicantId(),"assignment_cancelled","TA assignment cancelled",
                "Your assignment for "+a.getSectionSnapshot().getName()+" has been cancelled. Review My TA Placement for details.",a.getId());
        data.save(); return a;
    }
    private String text(JsonObject body,String key,boolean required) {
        if (body==null) throw new Problem(400,"Request details are required.");
        if (!body.has(key) && !required) return "";
        JsonElement value=body.get(key);
        if (value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new Problem(400,key+" must be text.");
        String s=value.getAsString().trim();
        if ((required && s.isEmpty()) || s.length()>1000) throw new Problem(400,key+" is required and must be at most 1000 characters.");
        return s;
    }
    private JsonObject row(SectionAssignment a, boolean staffView) {
        JsonObject row=JsonUtil.getGson().toJsonTree(a).getAsJsonObject();
        row.remove("responseDeadline");row.remove("expiredAt");
        row.addProperty("weeklyHoursTotal",staffView?weeklyHours(a.getApplicantId(),a.getPeriodId()):publishedWeeklyHours(a.getApplicantId(),a.getPeriodId()));
        row.addProperty("acceptedWeeklyHoursTotal",acceptedWeeklyHours(a.getApplicantId(),a.getPeriodId()));
        Module m=data.getData().getModules().stream().filter(x->Objects.equals(a.getSectionSnapshot().getModuleId(),x.getId())).findFirst().orElse(null);
        row.addProperty("moduleCode",m==null?"":m.getCode()); row.addProperty("moduleName",m==null?"":m.getName());
        data.getData().getUsers().stream().filter(u->Objects.equals(a.getApplicantId(),u.getId())).findFirst().ifPresent(u->row.addProperty("applicantName",u.getName()));
        if (!staffView) { row.remove("directReason"); row.remove("manualConfirmationNote"); row.remove("assignedBy"); row.remove("cancelledBy"); row.remove("publishedBy"); }
        return row;
    }
    public JsonObject list(String user,String role) {
        boolean student="applicant".equals(role);
        if(!student) staff(role);
        User actor=student?null:reviewStaff(user,role);
        DataStore ds=data.getData();
        Set<String> courses=new HashSet<>();
        ds.getModules().stream().filter(m->"admin".equals(role)||Objects.equals(user,m.getOrganiserId())).forEach(m->courses.add(m.getId()));
        JsonObject result=new JsonObject(); result.addProperty("serverNow",clock.instant().toString()); JsonArray rows=new JsonArray(); Set<String> periods=new HashSet<>();
        for (SectionAssignment a:ds.getSectionAssignments()) {
            if (student && !"published".equals(a.getPublicationStatus())) continue;
            if (student ? !Objects.equals(user,a.getApplicantId()) : !courses.contains(a.getSectionSnapshot().getModuleId())) continue;
            rows.add(row(a,!student)); periods.add(a.getPeriodId());
        }
        result.add("assignments",rows);
        if (!student) {
            result.add("manageableCourseIds",JsonUtil.getGson().toJsonTree(ds.getModules().stream()
                    .filter(m->ReviewAccess.canWrite(actor,m,ReviewAccess.MANAGE_ASSIGNMENTS)).map(Module::getId).toList()));
            result.add("modules",JsonUtil.getGson().toJsonTree(ds.getModules().stream().filter(m->courses.contains(m.getId())).toList()));
            JsonArray sections=new JsonArray();
            for (CourseSection s:ds.getCourseSections()) if (courses.contains(s.getModuleId())) sections.add(JsonUtil.getGson().toJsonTree(s));
            result.add("sections",sections);
            JsonArray candidates=new JsonArray();
            for (Application a:ds.getApplications()) {
                if (!"offer".equals(a.getStatus())) continue;
                Job j=ds.getJobs().stream().filter(x->Objects.equals(a.getJobId(),x.getId())).findFirst().orElse(null);
                if (j==null || !courses.contains(j.getModuleId())) continue;
                Module m=ds.getModules().stream().filter(x->x.getId().equals(j.getModuleId())).findFirst().orElseThrow();
                JsonObject c=new JsonObject(); c.addProperty("applicationId",a.getId());c.addProperty("applicantId",a.getApplicantId());
                c.addProperty("periodId",a.getPeriodId());c.addProperty("moduleId",j.getModuleId());
                ds.getUsers().stream().filter(u->Objects.equals(a.getApplicantId(),u.getId())).findFirst().ifPresent(u->c.addProperty("name",u.getName()));
                c.addProperty("weeklyHoursTotal",weeklyHours(a.getApplicantId(),a.getPeriodId()));
                c.addProperty("acceptedWeeklyHoursTotal",acceptedWeeklyHours(a.getApplicantId(),a.getPeriodId()));
                c.add("matching",new SectionMatchingService().match(ds,a,j,m.getOrganiserId())); candidates.add(c);
            }
            result.add("candidates",candidates);
            JsonArray applicants=new JsonArray();
            ds.getUsers().stream().filter(u->"applicant".equals(u.getRole())&&"active".equals(u.getStatus())).forEach(u->{
                JsonObject row=new JsonObject();row.addProperty("id",u.getId());row.addProperty("name",u.getName());
                row.addProperty("email",u.getEmail());applicants.add(row);
            });
            result.add("directApplicants",applicants);
            result.add("periods",JsonUtil.getGson().toJsonTree(ds.getApplicationPeriods()));
        } else result.add("periods",JsonUtil.getGson().toJsonTree(ds.getApplicationPeriods().stream().filter(p->periods.contains(p.getId())).toList()));
        return result;
    }
}
