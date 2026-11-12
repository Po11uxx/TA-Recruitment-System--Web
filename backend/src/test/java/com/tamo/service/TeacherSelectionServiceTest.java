package com.tamo.service;

import com.google.gson.*;
import com.tamo.model.*;
import com.tamo.util.JsonUtil;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TeacherSelectionServiceTest {
    DataStore ds; JsonDataService data; Application app; CourseSection section;
    @BeforeEach void setup() {
        ds=TestDataFactory.createTestDataStore();data=JsonDataService.detached(ds);app=ds.getApplications().get(0);
        section=new CourseSection();section.setId("direct_section");section.setModuleId("m001");section.setPeriodId("period_test");section.setName("Lab A");section.setStatus("active");section.setTaPositions(1);section.setHoursPerWeek(8);section.setTimeZone("America/Los_Angeles");
        var time=new CourseSection.MeetingTime();time.setDayOfWeek(1);time.setStartTime("10:00");time.setEndTime("12:00");section.setMeetingTimes(List.of(time));ds.getCourseSections().add(section);
        app.setAvailabilitySnapshot(JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(ds.getWeeklyAvailabilities().get(0)),WeeklyAvailability.class));
        var slot=new WeeklyAvailability.Slot();slot.setDayOfWeek(1);slot.setStartTime("00:00");slot.setEndTime("24:00");app.getAvailabilitySnapshot().setAvailableSlots(List.of(slot));app.getAvailabilitySnapshot().setConflictSlots(List.of());
    }
    JsonObject body(){var b=new JsonObject();b.addProperty("sectionId",section.getId());b.addProperty("note","Reviewed experience and confirmed course needs.");return b;}
    JsonObject select(String user,JsonObject body) {
        return new TeacherSelectionService(data).select(user,app.getId(),body);
    }
    void denied(int status,String user,JsonObject b){String before=JsonUtil.getGson().toJson(ds);var e=assertThrows(QualificationService.Problem.class,()->select(user,b));assertEquals(status,e.status);assertEquals(before,JsonUtil.getGson().toJson(ds),"Failed selection must not leave any mutations");}
    @Test void defaultTeacherHasDecisionPermission(){assertTrue(ReviewAccess.defaults("organiser").contains("review.approve"));assertFalse(ReviewAccess.defaults("reviewer").contains("review.approve"));}
    @Test void selectRecordsDecisionAndDraftWithoutPublishing(){var result=select("u005",body());assertEquals("draft",result.get("publicationStatus").getAsString());assertEquals(app.getId(),result.get("applicationId").getAsString());assertTrue(ReviewService.approvedForSection(ds,app.getId(),section.getId()));assertEquals("offer",app.getStatus());assertEquals(1,ds.getSectionAssignments().size());var assignment=ds.getSectionAssignments().get(0);assertEquals("u005",assignment.getAssignedBy());assertEquals("draft",assignment.getPublicationStatus());assertEquals(assignment.getId(),result.get("assignmentId").getAsString());assertEquals("u005",ds.getReviewRecords().get(0).getActorId());assertNotNull(ds.getReviewRecords().get(0).getCriteriaSnapshot());assertTrue(ds.getNotifications().stream().noneMatch(n->"assignment_published".equals(n.getType())));}
    @Test void adminNeedsBothGrantsForDirectSelection(){
        User admin=ReviewAccess.user(ds,"u010");
        assertEquals(403,assertThrows(QualificationService.Problem.class,()->select("u010",body())).status);
        admin.setCourseIds(java.util.List.of("m001"));
        admin.setPermissions(java.util.List.of("review.approve"));
        assertEquals(403,assertThrows(QualificationService.Problem.class,()->select("u010",body())).status);
        admin.setPermissions(java.util.List.of("review.approve",ReviewAccess.MANAGE_ASSIGNMENTS));
        assertNotNull(select("u010",body()).get("assignmentId"));
    }
    @Test void explicitRevocationIsPreserved(){ds.getUsers().stream().filter(u->"u005".equals(u.getId())).forEach(u->u.setPermissions(List.of("review.read","review.recommend")));denied(403,"u005",body());}
    @Test void readPermissionStillRequired(){ds.getUsers().stream().filter(u->"u005".equals(u.getId())).forEach(u->u.setPermissions(List.of("review.approve")));denied(403,"u005",body());}
    @Test void otherTeacherAndApplicantDenied(){denied(403,"u006",body());denied(403,"u001",body());}
    @Test void inactiveTeacherDenied(){ds.getUsers().stream().filter(u->"u005".equals(u.getId())).forEach(u->u.setStatus("suspended"));denied(403,"u005",body());}
    @Test void fullSectionLeavesNoApproval(){section.setTaPositions(0);denied(409,"u005",body());}
    @Test void conflictingAvailabilityLeavesNoApproval(){app.getAvailabilitySnapshot().setConflictSlots(app.getAvailabilitySnapshot().getAvailableSlots());denied(409,"u005",body());}
    @Test void pendingRequiresExplicitConfirmation(){app.setAvailabilitySnapshot(null);denied(409,"u005",body());var b=body();b.addProperty("confirmPending",true);b.addProperty("manualConfirmationNote","Confirmed availability directly with applicant.");select("u005",b);assertEquals("Confirmed availability directly with applicant.",ds.getSectionAssignments().get(0).getManualConfirmationNote());}
    @Test void missingSectionAndShortReasonDenied(){var b=body();b.remove("sectionId");denied(400,"u005",b);b=body();b.addProperty("note","short");denied(400,"u005",b);}
    @Test void wrongCycleDeniedWithoutMutation(){section.setPeriodId("different");denied(400,"u005",body());}
    @Test void existingDecisionCannotBeSilentlyOverwritten(){var b=body();b.addProperty("action","not_selected");new ReviewService(data).record("u005",app.getId(),b);denied(409,"u005",body());}
    @Test void retryCannotCreateDuplicate(){select("u005",body());denied(409,"u005",body());assertEquals(1,ds.getReviewRecords().size());assertEquals(1,ds.getSectionAssignments().size());}
    @Test void rowAdvertisesActionOnlyBeforeDecision(){var row=new ReviewService(data).row(ReviewAccess.user(ds,"u005"),app);assertTrue(row.get("canSelectTa").getAsBoolean());select("u005",body());row=new ReviewService(data).row(ReviewAccess.user(ds,"u005"),app);assertFalse(row.get("canSelectTa").getAsBoolean());assertEquals(ds.getSectionAssignments().get(0).getId(),row.get("selectionAssignmentId").getAsString());}
    @Test void withdrawnApplicationCannotBeSelected(){app.setStatus("withdrawn");denied(409,"u005",body());}
    @Test void archivedSectionCannotBeSelected(){section.setStatus("archived");denied(409,"u005",body());}
    @Test void inactiveApplicantCannotBeSelected(){ds.getUsers().stream().filter(u->app.getApplicantId().equals(u.getId())).forEach(u->u.setStatus("suspended"));denied(409,"u005",body());}
    @Test void sectionOutsidePostingIsDenied(){ds.getJobs().stream().filter(j->app.getJobId().equals(j.getId())).forEach(j->j.setSectionIds(List.of("another")));denied(400,"u005",body());}
}
