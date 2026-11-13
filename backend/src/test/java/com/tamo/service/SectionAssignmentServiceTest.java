package com.tamo.service;

import com.google.gson.*;
import com.tamo.model.*;
import com.tamo.util.JsonUtil;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SectionAssignmentServiceTest {
    DataStore ds; JsonDataService data; SectionAssignmentService service; Application app; CourseSection section;
    @BeforeEach void setup() {
        ds=TestDataFactory.createTestDataStore();data=mock(JsonDataService.class);when(data.getData()).thenReturn(ds);service=new SectionAssignmentService(data);
        for(Application original:ds.getApplications()){ReviewRecord selected=new ReviewRecord();selected.setId("fixture_approval_"+original.getId());selected.setApplicationId(original.getId());selected.setActorId("u010");selected.setAction("approve");selected.setNote("Synthetic selection approval");selected.setCreatedAt("2026-01-01T00:00:00Z");ds.getReviewRecords().add(selected);}
        app=ds.getApplications().get(0);app.setStatus("offer");
        app.setAvailabilitySnapshot(JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(ds.getWeeklyAvailabilities().get(0)),WeeklyAvailability.class));
        WeeklyAvailability.Slot slot=new WeeklyAvailability.Slot();slot.setDayOfWeek(1);slot.setStartTime("00:00");slot.setEndTime("24:00");
        app.getAvailabilitySnapshot().setAvailableSlots(List.of(slot));app.getAvailabilitySnapshot().setConflictSlots(List.of());
        section=section("section_one","m001","10:00","12:00");ds.getCourseSections().add(section);
    }
    CourseSection section(String id,String module,String start,String end) {
        CourseSection s=new CourseSection();s.setId(id);s.setPeriodId("period_test");s.setModuleId(module);s.setName(id);s.setStatus("active");s.setType("lab");s.setTimeZone("America/Los_Angeles");s.setTaPositions(1);s.setHoursPerWeek(8.5);
        CourseSection.MeetingTime m=new CourseSection.MeetingTime();m.setDayOfWeek(1);m.setStartTime(start);m.setEndTime(end);s.setMeetingTimes(List.of(m));return s;
    }
    JsonObject body(Application a,CourseSection s) { JsonObject b=new JsonObject();b.addProperty("applicationId",a.getId());b.addProperty("sectionId",s.getId());return b; }
    SectionAssignment assign() { return service.assign("u010","admin",body(app,section)); }
    JsonObject reason() { JsonObject b=new JsonObject();b.addProperty("reason","Course staffing changed");return b; }
    void fails(int status,Runnable action) { assertEquals(status,assertThrows(SectionAssignmentService.Problem.class,action::run).status); }
    @Test void assignsOfferAndFreezesSectionAndHours() {
        SectionAssignment a=assign();assertEquals("active",a.getStatus());assertEquals(app.getPeriodId(),a.getPeriodId());assertEquals(8.5,service.weeklyHours(app.getApplicantId(),app.getPeriodId()));
        section.setHoursPerWeek(20);assertEquals(8.5,a.getSectionSnapshot().getHoursPerWeek());verify(data).save();
    }
    @Test void onlyOffersAndCurrentOwnersCanAssign() {
        app.setStatus("shortlisted");fails(409,this::assign);app.setStatus("offer");
        fails(403,()->service.assign("u006","organiser",body(app,section)));fails(403,()->service.assign("u001","applicant",body(app,section)));
        // Stale posting metadata must not remove authority from the current course owner.
        ds.getJobs().get(0).setOrganiserId("u006");assertEquals("active",assign().getStatus());
    }
    @Test void directAssignmentNeedsNoApplicationAndCanBePublishedAndAccepted() {
        ds.getApplications().clear();
        ds.getReviewRecords().clear();
        ds.getWeeklyAvailabilities().clear();
        JsonObject direct=reason();
        direct.addProperty("applicantId","u001");direct.addProperty("periodId","period_test");
        direct.addProperty("sectionId",section.getId());
        fails(403,()->service.assignDirect("u005","organiser",direct));
        fails(409,()->service.assignDirect("u010","admin",direct));
        direct.addProperty("confirmPending",true);
        direct.addProperty("manualConfirmationNote","Confirmed directly with the student.");
        SectionAssignment assignment=service.assignDirect("u010","admin",direct);
        assertNull(assignment.getApplicationId());
        assertEquals("Course staffing changed",assignment.getDirectReason());
        assertTrue(ds.getApplications().isEmpty());
        assertTrue(service.list("u001","applicant").getAsJsonArray("assignments").isEmpty());
        fails(409,()->service.assignDirect("u010","admin",direct));
        service.publish("u010","admin",assignment.getId(),new JsonObject());
        assertEquals(assignment.getId(),ds.getOfferEmails().get(0).id());
        assertFalse(service.studentView(assignment).has("directReason"));
        service.respond("u001","applicant",assignment.getId(),decision("accept",null));
        assertEquals("accepted",assignment.getResponseStatus());
    }
    @Test void applicationMustBelongToSectionCourse() {section.setModuleId("m002");User admin=ReviewAccess.user(ds,"u010");admin.setPermissions(java.util.List.of(ReviewAccess.MANAGE_ASSIGNMENTS));admin.setCourseIds(java.util.List.of("m002"));fails(403,()->service.assign("u010","admin",body(app,section)));}
    @Test void zeroCapacityAndArchivedAreRejected() {section.setTaPositions(0);fails(409,this::assign);section.setTaPositions(1);section.setStatus("archived");fails(409,this::assign);}
    @Test void duplicateAndFullCapacityAreRejected() {
        assign();fails(409,this::assign);
        Application other=copy(app);other.setId("other");other.setApplicantId("u002");other.getAvailabilitySnapshot().setApplicantId("u002");ds.getApplications().add(other);ReviewRecord approval=new ReviewRecord();approval.setId("approval_"+other.getId());approval.setApplicationId(other.getId());approval.setActorId("u010");approval.setAction("approve");approval.setNote("Synthetic selection approval");approval.setCreatedAt("2026-01-01T00:00:00Z");ds.getReviewRecords().add(approval);
        fails(409,()->service.assign("u010","admin",body(other,section)));assertEquals(1,ds.getSectionAssignments().size());
    }
    @Test void sameStudentAcrossTwoApplicationsCannotTakeSameSectionTwice() {
        section.setTaPositions(2);assign();Application other=copy(app);other.setId("other");other.setJobId("j002");ds.getApplications().add(other);ReviewRecord approval=new ReviewRecord();approval.setId("approval_"+other.getId());approval.setApplicationId(other.getId());approval.setActorId("u010");approval.setAction("approve");approval.setNote("Synthetic selection approval");approval.setCreatedAt("2026-01-01T00:00:00Z");ds.getReviewRecords().add(approval);
        fails(409,()->service.assign("u010","admin",body(other,section)));
    }
    Application copy(Application a) { return JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(a),Application.class); }
    @Test void missingAvailabilityRequiresExplicitBooleanAndNote() {
        app.setAvailabilitySnapshot(null);JsonObject b=body(app,section);fails(409,()->service.assign("u010","admin",b));
        b.addProperty("confirmPending","true");b.addProperty("manualConfirmationNote","Confirmed with applicant");fails(409,()->service.assign("u010","admin",b));
        b.addProperty("confirmPending",true);b.addProperty("manualConfirmationNote","short");fails(409,()->service.assign("u010","admin",b));
        b.addProperty("manualConfirmationNote","Confirmed with applicant");assertEquals("Confirmed with applicant",service.assign("u010","admin",b).getManualConfirmationNote());
    }
    @Test void knownConflictCannotBeOverridden() {
        app.getAvailabilitySnapshot().setNoAvailability(true);JsonObject b=body(app,section);b.addProperty("confirmPending",true);b.addProperty("manualConfirmationNote","Confirmed with applicant");
        fails(409,()->service.assign("u010","admin",b));
    }
    @Test void overlapAcrossCoursesIsBlockedButAdjacentTimesAreAllowedAndHoursAggregate() {
        assign();Application other=ds.getApplications().get(1);other.setStatus("offer");other.setAvailabilitySnapshot(copy(app).getAvailabilitySnapshot());
        CourseSection second=section("second","m002","11:00","13:00");ds.getCourseSections().add(second);
        fails(409,()->service.assign("u010","admin",body(other,second)));
        second.getMeetingTimes().get(0).setStartTime("12:00");service.assign("u010","admin",body(other,second));
        assertEquals(17,service.weeklyHours("u001","period_test"));
    }
    @Test void capacityAndConflictsAreSeparatePerPeriod() {
        assign();Application other=copy(app);other.setId("other");other.setPeriodId("period_second");other.getAvailabilitySnapshot().setPeriodId("period_second");ds.getApplications().add(other);ReviewRecord approval=new ReviewRecord();approval.setId("approval_"+other.getId());approval.setApplicationId(other.getId());approval.setActorId("u010");approval.setAction("approve");approval.setNote("Synthetic selection approval");approval.setCreatedAt("2026-01-01T00:00:00Z");ds.getReviewRecords().add(approval);
        ApplicationPeriod p=new ApplicationPeriod();p.setId("period_second");ds.getApplicationPeriods().add(p);
        fails(409,()->service.assign("u010","admin",body(other,section)));
        CourseSection copy=section("section_second","m001","10:00","12:00");copy.setPeriodId("period_second");ds.getCourseSections().add(copy);
        service.assign("u010","admin",body(other,copy));assertEquals(1,service.occupied(copy.getId(),"period_second"));assertEquals(8.5,service.weeklyHours("u001","period_second"));
    }
    @Test void cancellationRetainsHistoryReleasesCapacityAndAllowsReassignment() {
        SectionAssignment a=assign();fails(400,()->service.cancel("u010","admin",a.getId(),new JsonObject()));
        fails(403,()->service.cancel("u006","organiser",a.getId(),reason()));fails(403,()->service.cancel("u001","applicant",a.getId(),reason()));
        service.cancel("u010","admin",a.getId(),reason());assertEquals("cancelled",a.getStatus());assertNotNull(a.getCancelledAt());assertEquals(0,service.weeklyHours("u001","period_test"));
        fails(409,()->service.cancel("u010","admin",a.getId(),reason()));assertNotEquals(a.getId(),assign().getId());assertEquals(2,ds.getSectionAssignments().size());
    }
    @Test void studentsSeeOnlyOwnRowsAndNoStaffConfirmationNote() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),publicationBody());a.setManualConfirmationNote("Private staff confirmation");
        JsonObject own=service.list("u001","applicant");assertEquals(1,own.getAsJsonArray("assignments").size());
        assertFalse(own.getAsJsonArray("assignments").get(0).getAsJsonObject().has("manualConfirmationNote"));assertFalse(own.has("candidates"));
        assertTrue(service.list("u002","applicant").getAsJsonArray("assignments").isEmpty());fails(403,()->service.list("u006","organiser"));
    }
    @Test void sectionEditsArchiveAndApplicationStatusChangesRequireCancellation() {
        SectionAssignment a=assign();CourseSectionService sections=new CourseSectionService(data);
        assertEquals(409,assertThrows(CourseSectionService.Problem.class,()->sections.update("u010","admin",section.getId(),new JsonObject())).status);
        assertThrows(CourseSectionService.Problem.class,()->sections.archive("u010","admin",section.getId(),true));
        ds.getUsers().stream().filter(u->"u005".equals(u.getId())).forEach(u->u.setPermissions(List.of("review.read","review.approve")));
        assertEquals(409,new OrganiserService(data).updateCandidateStatus("u005",app.getId(),"rejected","A documented final decision reason.").get("code"));assertEquals("offer",app.getStatus());
        service.cancel("u010","admin",a.getId(),reason());assertEquals("archived",sections.archive("u010","admin",section.getId(),true).getStatus());
    }
    @Test void adminEditorAndDeletePathsPreserveHistoryBeforeMutation() {
        assign();AdminService admin=new AdminService(data);JsonObject change=new JsonObject();change.addProperty("status","rejected");
        assertEquals(409,admin.updateCollectionItem("applications",app.getId(),change).get("code"));assertEquals("offer",app.getStatus());
        for(String[] ref:new String[][]{{"applications",app.getId()},{"users",app.getApplicantId()},{"jobs",app.getJobId()},{"modules",section.getModuleId()}})
            assertEquals("users".equals(ref[0])?403:409,admin.deleteFromCollection(ref[0],ref[1]).get("code"));
        assertEquals(409,new OrganiserService(data).deleteJob("u005",app.getJobId()).get("code"));assertTrue(ds.getApplications().contains(app));
    }
    @Test void ownershipTransferMovesManagementToNewOwnerButKeepsStudentVisibility() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),publicationBody());ds.getModules().get(0).setOrganiserId("u006");
        assertEquals(1,service.list("u010","admin").getAsJsonArray("assignments").size());fails(403,()->service.cancel("u005","organiser",a.getId(),reason()));
        assertEquals(1,service.list("u001","applicant").getAsJsonArray("assignments").size());service.cancel("u010","admin",a.getId(),reason());
    }
    @Test void malformedRequestsHaveNoSideEffects() { fails(400,()->service.assign("u010","admin",null));JsonObject b=body(app,section);b.addProperty("applicationId",42);fails(400,()->service.assign("u010","admin",b));assertTrue(ds.getSectionAssignments().isEmpty()); }
    JsonObject decision(String value,String reason) {JsonObject b=new JsonObject();b.addProperty("decision",value);if(reason!=null)b.addProperty("reason",reason);return b;}
    @Test void draftIsHiddenAndPublicationNotifiesExactlyOnce() {
        SectionAssignment a=assign();int before=ds.getNotifications().size();
        assertTrue(ds.getOfferEmails().isEmpty());assertEquals("draft",a.getPublicationStatus());assertTrue(service.list("u001","applicant").getAsJsonArray("assignments").isEmpty());
        assertEquals(1,service.occupied(section.getId(),"period_test"));assertEquals(0,service.acceptedWeeklyHours("u001","period_test"));
        fails(403,()->service.publish("u006","organiser",a.getId(),publicationBody()));fails(403,()->service.publish("u001","applicant",a.getId(),publicationBody()));
        service.publish("u010","admin",a.getId(),publicationBody());assertEquals("published",a.getPublicationStatus());assertNotNull(a.getPublishedAt());
        assertEquals(before+1,ds.getNotifications().size());Notification n=ds.getNotifications().get(before);assertEquals("u001",n.getUserId());assertEquals(a.getId(),n.getRelatedId());assertEquals("assignment_published",n.getType());
        OfferEmail email=assertDoesNotThrow(()->ds.getOfferEmails().get(0));
        assertEquals(a.getId(),email.id());
        assertEquals(ReviewAccess.user(ds,"u001").getEmail(),email.recipientEmail());
        assertEquals("/applicant/assignments.jsp?assignmentId="+a.getId(),email.placementPath());
        fails(409,()->service.publish("u010","admin",a.getId(),publicationBody()));
        assertEquals(1,ds.getOfferEmails().size());
        assertEquals(before+1,ds.getNotifications().size());
    }
    @Test void onlyOwnerCanReplyToPublishedPendingAssignment() {
        SectionAssignment a=assign();fails(404,()->service.respond("u001","applicant",a.getId(),decision("accept",null)));
        service.publish("u010","admin",a.getId(),publicationBody());fails(404,()->service.respond("u002","applicant",a.getId(),decision("accept",null)));
        fails(403,()->service.respond("u010","admin",a.getId(),decision("accept",null)));assertEquals("pending",a.getResponseStatus());
    }
    @Test void acceptanceRecordsTimeAndCountsOnlyAcceptedHours() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),publicationBody());int before=ds.getNotifications().size();
        service.respond("u001","applicant",a.getId(),decision("accept",null));assertEquals("accepted",a.getResponseStatus());assertNotNull(a.getRespondedAt());assertEquals("active",a.getStatus());
        assertEquals(8.5,service.acceptedWeeklyHours("u001","period_test"));assertEquals(1,service.occupied(section.getId(),"period_test"));
        assertEquals(before+1,ds.getNotifications().size());assertEquals("u010",ds.getNotifications().get(before).getUserId());
        fails(409,()->service.respond("u001","applicant",a.getId(),decision("accept",null)));fails(409,()->service.respond("u001","applicant",a.getId(),decision("decline","Changed mind")));assertEquals(before+1,ds.getNotifications().size());
    }
    @Test void declineRequiresReasonReleasesPositionAndKeepsResponseOnReassignment() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),publicationBody());
        fails(400,()->service.respond("u001","applicant",a.getId(),decision("decline","  ")));assertEquals("active",a.getStatus());assertEquals("pending",a.getResponseStatus());
        service.respond("u001","applicant",a.getId(),decision("decline","Schedule changed"));
        assertEquals("declined",a.getResponseStatus());assertEquals("cancelled",a.getStatus());assertEquals("u001",a.getCancelledBy());assertEquals("Schedule changed",a.getResponseReason());
        assertEquals(0,service.occupied(section.getId(),"period_test"));assertEquals(0,service.weeklyHours("u001","period_test"));assertEquals(0,service.acceptedWeeklyHours("u001","period_test"));
        SectionAssignment next=assign();assertNotEquals(a.getId(),next.getId());assertEquals("draft",next.getPublicationStatus());assertEquals("pending",next.getResponseStatus());
        assertEquals(1,service.list("u001","applicant").getAsJsonArray("assignments").size());
    }
    @Test void invalidResponseHasNoPartialMutationOrNotification() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),publicationBody());int before=ds.getNotifications().size();
        fails(400,()->service.respond("u001","applicant",a.getId(),decision("other",null)));
        fails(400,()->service.respond("u001","applicant",a.getId(),decision("decline","x".repeat(1001))));
        fails(400,()->service.respond("u001","applicant",a.getId(),null));assertNull(a.getRespondedAt());assertEquals("pending",a.getResponseStatus());assertEquals(before,ds.getNotifications().size());
    }
    @Test void cancelledPublishedOfferCannotBeRepliedToOrRepublishedAndNotifiesStudent() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),publicationBody());int before=ds.getNotifications().size();
        service.cancel("u010","admin",a.getId(),reason());assertEquals(before+1,ds.getNotifications().size());assertEquals("assignment_cancelled",ds.getNotifications().get(before).getType());
        fails(409,()->service.respond("u001","applicant",a.getId(),decision("accept",null)));fails(409,()->service.publish("u010","admin",a.getId(),publicationBody()));
    }
    @Test void cancellingAcceptedOfferRetainsAcceptanceButRemovesAcceptedWorkload() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),publicationBody());service.respond("u001","applicant",a.getId(),decision("accept",null));String acceptedAt=a.getRespondedAt();
        service.cancel("u010","admin",a.getId(),reason());assertEquals("accepted",a.getResponseStatus());assertEquals(acceptedAt,a.getRespondedAt());assertEquals(0,service.acceptedWeeklyHours("u001","period_test"));
    }
    @Test void draftsDoNotLeakThroughStudentTotalsAndStaffSeesReservedAndAcceptedSeparately() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),publicationBody());
        CourseSection other=section("another","m001","12:00","13:00");ds.getCourseSections().add(other);service.assign("u010","admin",body(app,other));
        JsonObject row=service.list("u001","applicant").getAsJsonArray("assignments").get(0).getAsJsonObject();assertEquals(8.5,row.get("weeklyHoursTotal").getAsDouble());assertFalse(row.has("publishedBy"));assertEquals(17,service.weeklyHours("u001","period_test"));
        service.respond("u001","applicant",a.getId(),decision("accept",null));assertEquals(8.5,service.acceptedWeeklyHours("u001","period_test"));
    }
    @Test void replyNotifiesAdminWhoPublishedAfterTransfer() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),publicationBody());ds.getModules().get(0).setOrganiserId("u006");int before=ds.getNotifications().size();
        service.respond("u001","applicant",a.getId(),decision("decline","Unable to attend"));assertEquals("u010",ds.getNotifications().get(before).getUserId());
    }
    @Test void draftCancellationIsSilentAndCannotBePublished() {
        SectionAssignment a=assign();int before=ds.getNotifications().size();service.cancel("u010","admin",a.getId(),reason());assertEquals(before,ds.getNotifications().size());fails(409,()->service.publish("u010","admin",a.getId(),publicationBody()));
    }
    private com.google.gson.JsonObject publicationBody() {
        return new JsonObject();
    }
    private void at(String instant) {service=new SectionAssignmentService(data,java.time.Clock.fixed(java.time.Instant.parse(instant),java.time.ZoneOffset.UTC));}
    @Test void clientDeadlineFieldsAreIgnoredWhenPublishing() {
        SectionAssignment a=assign();JsonObject body=new JsonObject();body.addProperty("responseDeadline","not a date");
        service.publish("u010","admin",a.getId(),body);assertNull(a.getResponseDeadline());assertEquals("published",a.getPublicationStatus());
    }
    @Test void pendingReplyRetainsCapacityUntilExplicitCancellation() {
        at("2026-10-16T12:00:00Z");SectionAssignment a=assign();service.publish("u010","admin",a.getId(),new JsonObject());
        at("2027-10-16T12:00:00Z");service.list("u001","applicant");assertEquals("pending",a.getResponseStatus());assertEquals("active",a.getStatus());assertEquals(1,service.occupied(section.getId(),app.getPeriodId()));
        service.cancel("u010","admin",a.getId(),reason());assertEquals(0,service.occupied(section.getId(),app.getPeriodId()));
    }
    @Test void historicalExpiredCancellationStaysCancelledAndCannotBeRepliedTo() {
        SectionAssignment a=assign();a.setPublicationStatus("published");a.setResponseDeadline("2020-01-01T00:00:00Z");a.setStatus("cancelled");a.setResponseStatus("expired");a.setExpiredAt("2020-01-01T00:00:00Z");
        fails(409,()->service.respond("u001","applicant",a.getId(),decision("accept",null)));fails(409,()->service.publish("u010","admin",a.getId(),new JsonObject()));
        JsonObject row=service.list("u001","applicant").getAsJsonArray("assignments").get(0).getAsJsonObject();assertFalse(row.has("responseDeadline"));assertFalse(row.has("expiredAt"));assertEquals("cancelled",a.getStatus());
    }
    @Test void reportsUseAcceptedLedgerAndHideDraftHoursFromStudents() {
        SectionAssignment a=assign();AssignmentReportingService reporting=new AssignmentReportingService(ds);
        assertEquals(0,reporting.workload("u001","period_test",false).get("current").getAsDouble());assertEquals(0,reporting.workload("u001","period_test",false).get("reservedHours").getAsDouble());
        assertEquals(8.5,reporting.workload("u001","period_test",true).get("reservedHours").getAsDouble());
        service.publish("u010","admin",a.getId(),publicationBody());assertEquals(0,reporting.workload("u001","period_test",false).get("current").getAsDouble());
        service.respond("u001","applicant",a.getId(),decision("accept",null));assertEquals(8.5,reporting.workload("u001","period_test",false).get("current").getAsDouble());
        assertEquals(0,reporting.workload("u001","other_period",false).get("current").getAsDouble());
        service.cancel("u010","admin",a.getId(),reason());assertEquals(0,reporting.workload("u001","period_test",false).get("current").getAsDouble());
    }
 @Test void offerStatusAloneDoesNotBypassSelectionApproval(){ds.getReviewRecords().clear();fails(409,this::assign);assertTrue(ds.getSectionAssignments().isEmpty());}
    @Test void publishWithoutDeadlineKeepsReplyPending() {
        SectionAssignment a=assign();service.publish("u010","admin",a.getId(),new JsonObject());
        assertEquals("published",a.getPublicationStatus());assertEquals("pending",a.getResponseStatus());assertNull(a.getResponseDeadline());
        assertTrue(ds.getNotifications().stream().noneMatch(n->n.getMessage()!=null&&n.getMessage().contains("before ")));
    }
    @Test void elapsedLegacyDeadlineDoesNotRejectStudentReplyOrReleasePosition() {
        at("2026-10-16T12:00:00Z");SectionAssignment a=assign();a.setPublicationStatus("published");a.setResponseDeadline("2026-10-17T12:00:00Z");
        at("2026-12-16T12:00:00Z");assertEquals(1,service.occupied(section.getId(),app.getPeriodId()));
        service.respond("u001","applicant",a.getId(),decision("accept",null));
        assertEquals("active",a.getStatus());assertEquals("accepted",a.getResponseStatus());assertEquals(1,service.occupied(section.getId(),app.getPeriodId()));
    }
}
