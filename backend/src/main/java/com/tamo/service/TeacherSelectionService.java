package com.tamo.service;

import com.google.gson.JsonObject;
import com.tamo.model.*;
import com.tamo.util.JsonUtil;
import java.util.Objects;
import java.util.Set;

/** One human decision creates an approval and a Section assignment draft. */
public class TeacherSelectionService {
    private final JsonDataService data;
    public TeacherSelectionService(JsonDataService data) { this.data=data; }

    public static boolean hasDecision(DataStore ds,String appId) {
        return ds.getReviewRecords().stream().anyMatch(r -> Objects.equals(appId,r.getApplicationId())
                && Set.of("approve","not_selected").contains(r.getAction()));
    }
    private static boolean maySelect(DataStore ds, User user, Application app) {
        Job job=ds.getJobs().stream().filter(j -> Objects.equals(j.getId(),app.getJobId())).findFirst().orElse(null);
        if(job==null || !ReviewAccess.can(user,"review.read")) return false;
        com.tamo.model.Module module=ReviewAccess.module(ds,job);
        return ReviewAccess.canWrite(user,module,"review.approve")
                && ReviewAccess.canWrite(user,module,ReviewAccess.MANAGE_ASSIGNMENTS);
    }

    public static void addCapabilities(JsonObject row,DataStore ds,User user,Application app) {
        row.addProperty("canSelectTa",maySelect(ds,user,app)
                && Set.of("applied","review","interview","shortlisted").contains(app.getStatus()==null?"":app.getStatus())
                && !hasDecision(ds,app.getId()) && !SectionAssignmentService.activeForApplication(ds,app.getId()));
        ds.getSectionAssignments().stream().filter(a -> Objects.equals(app.getId(),a.getApplicationId())
                && "active".equals(a.getStatus())).findFirst().ifPresent(a -> row.addProperty("selectionAssignmentId",a.getId()));
    }

    public JsonObject select(String userId,String applicationId,JsonObject body) {
        synchronized(data.requestLock()) {
            // Exercise the complete business workflow on a detached copy first. Expected
            // validation errors must not leave an approval behind, even outside HTTP.
            DataStore candidate=JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(data.getData()),DataStore.class);
            perform(JsonDataService.detached(candidate),userId,applicationId,body);
            return perform(data,userId,applicationId,body);
        }
    }
    private JsonObject perform(JsonDataService target,String userId,String applicationId,JsonObject body) {
        DataStore ds=target.getData();User user=ReviewAccess.user(ds,userId);
        Application app=ds.getApplications().stream().filter(a -> Objects.equals(applicationId,a.getId())).findFirst()
                .orElseThrow(() -> new QualificationService.Problem(404,"Application not found."));
        if(!maySelect(ds,user,app))
            throw new QualificationService.Problem(403,"TA selection requires decision and assignment access for this course.");
        if(hasDecision(ds,applicationId) || SectionAssignmentService.activeForApplication(ds,applicationId)
                || !Set.of("applied","review","interview","shortlisted").contains(app.getStatus()==null?"":app.getStatus()))
            throw new QualificationService.Problem(409,"This application already has a decision or is no longer awaiting selection. Review its current decision and assignments.");
        if(ds.getUsers().stream().noneMatch(u -> Objects.equals(app.getApplicantId(),u.getId()) && "applicant".equals(u.getRole()) && "active".equals(u.getStatus())))
            throw new QualificationService.Problem(409,"An active applicant account is required for TA selection.");
        String section=QualificationService.text(body,"sectionId",200,true);
        String note=QualificationService.text(body,"note",4000,true);
        JsonObject approval=new JsonObject();approval.addProperty("action","approve");approval.addProperty("sectionId",section);approval.addProperty("note",note);
        new ReviewService(target).record(userId,applicationId,approval);
        JsonObject assignment=body.deepCopy();assignment.addProperty("applicationId",applicationId);
        if("admin".equals(user.getRole())) assignment.addProperty("reason",note);
        try {
            SectionAssignment draft=new SectionAssignmentService(target).assign(userId,user.getRole(),assignment);
            JsonObject result=new JsonObject();result.addProperty("applicationId",applicationId);result.addProperty("assignmentId",draft.getId());result.addProperty("publicationStatus",draft.getPublicationStatus());return result;
        } catch(SectionAssignmentService.Problem e) {throw new QualificationService.Problem(e.status,e.getMessage());}
    }
}
