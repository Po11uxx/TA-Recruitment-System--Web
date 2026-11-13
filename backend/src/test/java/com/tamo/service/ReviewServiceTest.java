package com.tamo.service;
import com.google.gson.*;
import com.tamo.model.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ReviewServiceTest{
 DataStore ds;JsonDataService data;ReviewService service;
 @BeforeEach void setup(){ds=TestDataFactory.createTestDataStore();data=mock(JsonDataService.class);when(data.getData()).thenReturn(ds);service=new ReviewService(data);for(String role:List.of("reviewer","decision_maker","viewer")){User u=new User();u.setId(role);u.setName(role);u.setRole(role);u.setStatus("active");u.setCourseIds(List.of("m001"));ds.getUsers().add(u);}ds.getApplications().get(0).setQualificationSnapshot(new QualificationService(data).snapshot("u001"));}
 JsonObject action(String name){JsonObject b=new JsonObject();b.addProperty("action",name);b.addProperty("note","Reviewed submitted information and scheduling criteria");return b;}
 @Test void adminReviewsGloballyWithNoDelegation(){
  User admin=ReviewAccess.user(ds,"u010");
  assertTrue(service.row(admin,ds.getApplications().get(0)).get("canApprove").getAsBoolean());
  service.record("u010","a001",action("approve"));
  service.record("u010","a002",action("approve"));
  assertEquals("u010",ds.getReviewRecords().get(0).getActorId());
 }
 @Test void reviewerCanRecommendButCannotApprove(){service.record("reviewer","a001",action("recommend"));assertFalse(ReviewService.approved(ds,"a001"));assertEquals("applied",ds.getApplications().get(0).getStatus());assertEquals(403,assertThrows(QualificationService.Problem.class,()->service.record("reviewer","a001",action("approve"))).status);}
 @Test void decisionMakerApprovesAndRecordsEvidenceAndActor(){service.record("decision_maker","a001",action("approve"));assertTrue(ReviewService.approved(ds,"a001"));assertEquals("offer",ds.getApplications().get(0).getStatus());ReviewRecord r=ds.getReviewRecords().get(0);assertEquals("decision_maker",r.getActorId());assertNotNull(r.getCriteriaSnapshot());assertNotNull(r.getScheduleSnapshot());assertNotNull(r.getCreatedAt());}
 @Test void viewerCannotWriteOrReadInternalNotes(){service.record("reviewer","a001",action("note"));JsonObject row=service.list("viewer",Map.of()).getAsJsonArray("applications").get(0).getAsJsonObject();assertFalse(row.has("records"));assertFalse(row.get("canApprove").getAsBoolean());assertThrows(QualificationService.Problem.class,()->service.record("viewer","a001",action("note")));}
 @Test void scopeAndLiveRevocationAreEnforced(){assertThrows(QualificationService.Problem.class,()->service.record("reviewer","a002",action("recommend")));ds.getUsers().stream().filter(u->u.getId().equals("reviewer")).findFirst().orElseThrow().setPermissions(List.of());assertThrows(QualificationService.Problem.class,()->service.list("reviewer",Map.of()));}
 @Test void filtersAreCombinedAgainstSubmittedInformation(){assertEquals(1,service.list("viewer",Map.of("level","masters","skill","python","interest","biology")).getAsJsonArray("applications").size());assertEquals(0,service.list("viewer",Map.of("level","phd")).getAsJsonArray("applications").size());}
 @Test void approvalNeedsReasonAndSelectionIsSeparateFromRecommendation(){JsonObject b=action("approve");b.addProperty("note","short");assertThrows(QualificationService.Problem.class,()->service.record("decision_maker","a001",b));service.record("decision_maker","a001",action("approve"));service.record("reviewer","a001",action("alternate"));assertTrue(ReviewService.approved(ds,"a001"));service.record("decision_maker","a001",action("not_selected"));assertFalse(ReviewService.approved(ds,"a001"));assertEquals("rejected",ds.getApplications().get(0).getStatus());}
 @Test void applicantsHaveNoReviewAccessAndHistoryCannotBeDeleted(){assertThrows(QualificationService.Problem.class,()->service.list("u001",Map.of()));service.record("reviewer","a001",action("flag"));assertTrue(SectionAssignmentService.hasHistory(ds,"applications","a001"));assertTrue(SectionAssignmentService.hasHistory(ds,"users","reviewer"));JsonObject moved=new JsonObject();moved.addProperty("applicantId","u002");assertEquals(409,new AdminService(data).updateCollectionItem("applications","a001",moved).get("code"));assertEquals(409,new AdminService(data).deleteFromCollection("applications","a001").get("code"));}
}
