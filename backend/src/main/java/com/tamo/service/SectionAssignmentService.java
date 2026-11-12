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
        if (!Set.of("admin","organiser").contains(role == null ? "" : role)) throw new Problem(403,"Only staff can manage assignments.");
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
        module(user,role,section(a.getSectionId()).getModuleId());
        if (!"active".equals(a.getStatus()) || !"draft".equals(a.getPublicationStatus())) throw new Problem(409,"Only an active draft can be published.");
        Application app=data.getData().getApplications().stream().filter(x->Objects.equals(a.getApplicationId(),x.getId())).findFirst().orElseThrow(()->new Problem(409,"Application no longer exists."));
        if (!JobSectionService.includes(job(app),section(a.getSectionId()))) throw new Problem(409,"This Section is not included in the TA posting.");
        if(!ReviewService.approvedForSection(data.getData(),app.getId(),a.getSectionId())) throw new Problem(409,"An authorized selection approval is required before assignment.");
        if (!"offer".equals(app.getStatus())) throw new Problem(409,"Application must still be at the offer stage.");
        String delegationReason=delegationReason(role,body);
        a.setPublicationStatus("published");a.setPublishedBy(user);a.setPublishedAt(clock.instant().toString());
        notifyUser(a.getApplicantId(),"assignment_published","TA assignment ready for your reply",
                "Your assignment for "+a.getSectionSnapshot().getName()+" has been published. Review its meetings and weekly hours in My TA Placement, then accept or decline.",a.getId());
        data.save();auditDelegation(user,a,"Published",delegationReason);return a;
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
        Module course=data.getData().getModules().stream().filter(m->Objects.equals(a.getSectionSnapshot().getModuleId(),m.getId())).findFirst().orElse(null);
        String name=data.getData().getUsers().stream().filter(u->Objects.equals(user,u.getId())).map(User::getName).findFirst().orElse("Applicant");
        if (course!=null && course.getOrganiserId()!=null) notifyUser(course.getOrganiserId(),"assignment_response","TA assignment "+a.getResponseStatus(),
                name+" "+a.getResponseStatus()+" the assignment for "+a.getSectionSnapshot().getName()+". Review TA Assignments for details.",a.getId());
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
        if (!"active".equals(s.getStatus())) throw new Problem(409,"Restore the section before assigning a TA.");
        if (app.getPeriodId()==null || data.getData().getApplicationPeriods().stream().noneMatch(p->app.getPeriodId().equals(p.getId()))) throw new Problem(409,"Application period is missing.");
        if (data.getData().getSectionAssignments().stream().anyMatch(a->"active".equals(a.getStatus()) && s.getId().equals(a.getSectionId()) && app.getApplicantId().equals(a.getApplicantId()) && app.getPeriodId().equals(a.getPeriodId()))) throw new Problem(409,"This applicant is already assigned to this section in this period.");
        if (occupied(s.getId(),app.getPeriodId()) >= s.getTaPositions()) throw new Problem(409,"No TA positions remain for this section in this period.");
        JsonObject matching=new SectionMatchingService().match(data.getData(),app,job,m.getOrganiserId());
        JsonObject matched=null;
        for (JsonElement row:matching.getAsJsonArray("sections")) if (s.getId().equals(row.getAsJsonObject().get("sectionId").getAsString())) matched=row.getAsJsonObject();
        String status=matched==null ? "pending":matched.get("status").getAsString();
        if ("conflict".equals(status)) throw new Problem(409,"Submitted availability conflicts with this section. Choose a compatible section.");
        String note=text(body,"manualConfirmationNote",false);
        if ("pending".equals(status) && (!body.has("confirmPending") || !body.get("confirmPending").isJsonPrimitive() || !body.get("confirmPending").getAsJsonPrimitive().isBoolean() || !body.get("confirmPending").getAsBoolean() || note.length()<10)) throw new Problem(409,"Availability needs confirmation. Confirm it manually and provide a note of at least 10 characters.");
        for (SectionAssignment existing:data.getData().getSectionAssignments()) {
            if (!"active".equals(existing.getStatus()) || !app.getApplicantId().equals(existing.getApplicantId()) || !app.getPeriodId().equals(existing.getPeriodId())) continue;
            if (overlaps(s,existing.getSectionSnapshot())) throw new Problem(409,"Meeting times overlap another active assignment in this application period.");
        }
        String delegationReason=delegationReason(role,body);
        SectionAssignment a=new SectionAssignment();
        a.setId("assignment_"+UUID.randomUUID().toString().replace("-","")); a.setApplicationId(app.getId());
        a.setApplicantId(app.getApplicantId()); a.setSectionId(s.getId()); a.setPeriodId(app.getPeriodId());
        a.setStatus("active"); a.setAssignedBy(user); a.setAssignedAt(clock.instant().toString());
        a.setManualConfirmationNote("pending".equals(status)?note:"");
        a.setSectionSnapshot(JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(s),CourseSection.class));
        data.getData().getSectionAssignments().add(a); data.save(); auditDelegation(user,a,"Drafted",delegationReason); return a;
    }
    private String delegationReason(String role,JsonObject body) {
        if(!"admin".equals(role)) return null;
        try {
            return QualificationService.text(body,"reason",4000,true);
        } catch(QualificationService.Problem e) {
            throw new Problem(e.status,e.getMessage());
        }
    }
    private void auditDelegation(String user,SectionAssignment assignment,String action,String reason) {
        if(reason!=null) data.addLog("Delegated TA Assignment","info",null,user,
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
        if (!staffView) { row.remove("manualConfirmationNote"); row.remove("assignedBy"); row.remove("cancelledBy"); row.remove("publishedBy"); }
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
            result.add("periods",JsonUtil.getGson().toJsonTree(ds.getApplicationPeriods()));
        } else result.add("periods",JsonUtil.getGson().toJsonTree(ds.getApplicationPeriods().stream().filter(p->periods.contains(p.getId())).toList()));
        return result;
    }
}
