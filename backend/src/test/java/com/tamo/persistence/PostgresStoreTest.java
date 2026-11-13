package com.tamo.persistence;

import com.tamo.model.*;
import com.tamo.service.TestDataFactory;
import com.tamo.util.JsonUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real database tests, isolated in a disposable schema in an explicitly named test database. */
@EnabledIfEnvironmentVariable(named = "TAMO_TEST_DB_URL", matches = ".+")
class PostgresStoreTest {
    @TempDir Path temp;
    private String baseUrl;
    private String url;
    private String schema;
    private String user;
    private String password;
    private Path seed;
    private PostgresStore store;

    @BeforeEach void setup() throws Exception {
        baseUrl = System.getenv("TAMO_TEST_DB_URL");
        if (!baseUrl.matches("jdbc:postgresql://[^/]+/[^?]*_test(?:\\?.*)?")) {
            throw new IllegalArgumentException("Integration tests require a database name ending in _test");
        }
        user = System.getenv().getOrDefault("TAMO_DB_USER", "tamo");
        password = System.getenv().getOrDefault("TAMO_DB_PASSWORD", "tamo_local_only");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection conn = DriverManager.getConnection(baseUrl, user, password);
             Statement stmt = conn.createStatement()) { stmt.execute("CREATE SCHEMA " + schema); }
        url = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        seed = temp.resolve("data.json");
        Files.writeString(seed, JsonUtil.getGson().toJson(TestDataFactory.createTestDataStore()));
        store = new PostgresStore(url, user, password);
        store.initialize(seed, temp.resolve("backups"));
    }

    @AfterEach void cleanup() throws Exception {
        if (store != null) store.close();
        if (schema != null) {
            try (Connection conn = DriverManager.getConnection(baseUrl, user, password);
                 Statement stmt = conn.createStatement()) { stmt.execute("DROP SCHEMA " + schema + " CASCADE"); }
        }
    }

    @Test void qualificationsCriteriaGrantsAndReviewEvidenceSurviveReconnect() throws Exception {
        store.begin();DataStore ds=store.load();
        var userRow=ds.getUsers().get(0);userRow.setPermissions(java.util.List.of("review.read"));userRow.setCourseIds(java.util.List.of("m001"));
        var snapshot=ds.getProfiles().get(0).getQualifications().deepCopy();ds.getApplications().get(0).setQualificationSnapshot(snapshot);
        ds.getJobs().get(0).setSectionIds(java.util.List.of("synthetic_lecture","synthetic_lab"));
        var policies=new com.google.gson.JsonObject();policies.add("period_legacy",com.google.gson.JsonParser.parseString("{\"requiredSkills\":[\"Python\"]}"));ds.getJobs().get(0).setPeriodCriteria(policies);
        ReviewRecord review=new ReviewRecord();review.setId("synthetic_review");review.setApplicationId(ds.getApplications().get(0).getId());review.setActorId(userRow.getId());review.setAction("recommend");review.setNote("Synthetic persistence verification");review.setCreatedAt("2026-10-17T00:00:00Z");review.setCriteriaSnapshot(policies);ds.getReviewRecords().add(review);
        store.save(ds);store.commit();store.close();store.begin();DataStore actual=store.load();
        assertEquals(java.util.List.of("synthetic_lecture","synthetic_lab"),actual.getJobs().get(0).getSectionIds());
        assertEquals(snapshot,actual.getApplications().get(0).getQualificationSnapshot());assertEquals(policies,actual.getJobs().get(0).getPeriodCriteria());assertEquals(java.util.List.of("review.read"),actual.getUsers().get(0).getPermissions());assertEquals(java.util.List.of("m001"),actual.getUsers().get(0).getCourseIds());assertEquals(policies,actual.getReviewRecords().get(0).getCriteriaSnapshot());assertEquals(userRow.getId(),actual.getReviewRecords().get(0).getActorId());
    }

    @Test void upgradingExistingOfferBackfillsOneHistoricalApprovalWithoutReimport() throws Exception {
        store.begin();DataStore ds=store.load();ds.getApplications().get(0).setStatus("offer");store.save(ds);store.commit();store.close();
        try(Connection conn=DriverManager.getConnection(url,user,password);Statement stmt=conn.createStatement()){stmt.execute("DELETE FROM tamo_schema_version WHERE version>=8");}
        Files.writeString(seed,"invalid seed must not be reimported");store.initialize(seed,temp.resolve("backups"));store.begin();DataStore upgraded=store.load();assertTrue(com.tamo.service.ReviewService.approved(upgraded,"a001"));int approvals=upgraded.getReviewRecords().size();assertTrue(approvals>0);store.commit();store.close();
        store.initialize(seed,temp.resolve("backups"));store.begin();assertEquals(approvals,store.load().getReviewRecords().size());
    }

    @Test void sponsorCsvBatchAndSectionEvidenceSurviveDatabaseReconnect() throws Exception {
        store.begin();var detached=com.tamo.service.JsonDataService.detached(store.load());var csv=new com.tamo.service.SponsorCsvService(detached);Path samples=Path.of("../Test_Data");if(!Files.isDirectory(samples))samples=Path.of("Test_Data");
        Path courseFile=samples.resolve("Courses & Sections Dataset.csv");
        var courses=com.tamo.service.SponsorCsv.read(Files.readString(courseFile),com.tamo.service.SponsorCsvService.COURSES);
        for(var row:courses){
            String code=row.get("Course ID");
            if(detached.getData().getModules().stream().anyMatch(m->code.equals(m.getCode())))continue;
            var course=new com.tamo.model.Module();course.setId(code.replace(" ","_"));course.setCode(code);
            course.setName(row.get("Course Name"));course.setOrganiserId("u005");course.setOrganiserName("Teacher");course.setStatus("active");
            detached.getData().getModules().add(course);
        }
        for(String[] pair:new String[][]{{"courses","Courses & Sections Dataset.csv"},{"students","Student Applications Dataset(Updated).csv"},{"selections","Selection & Assignment Management Dataset(Updated).csv"}}){var b=new com.google.gson.JsonObject();b.addProperty("dataset",pair[0]);b.addProperty("csv",Files.readString(samples.resolve(pair[1])));b.addProperty("periodId","period_test");b.addProperty("organiserId","u005");b.addProperty("hoursPerWeek",4);var preview=csv.run("u010",b,false);assertEquals(0,preview.get("errors").getAsInt(),preview.toString());b.add("previewToken",preview.get("previewToken"));csv.run("u010",b,true);}
        store.save(detached.getData());store.commit();store.close();store.begin();DataStore actual=store.load();assertEquals(6,actual.getCourseSections().size());assertEquals(6,actual.getReviewRecords().size());assertNotNull(actual.getCourseSections().get(0).getMatchingCriteria());assertNotNull(actual.getReviewRecords().get(0).getSectionId());assertNotNull(actual.getReviewRecords().get(0).getSourceMetadata());assertTrue(actual.getSectionAssignments().isEmpty());
        var exporter=new com.tamo.service.SponsorCsvService(com.tamo.service.JsonDataService.detached(actual));assertEquals(20,com.tamo.service.SponsorCsv.read(exporter.export("u010","students","period_test",false),com.tamo.service.SponsorCsvService.STUDENTS).size());
    }

    @Test void directAssignmentPersistsWithoutAnApplication() throws Exception {
        store.begin();
        DataStore ds=store.load();
        var sectionBody=com.google.gson.JsonParser.parseString("""
                {"periodId":"period_test","name":"Direct lab","type":"lab","taPositions":1,
                 "hoursPerWeek":4,"timeZone":"America/Los_Angeles",
                 "meetingTimes":[{"dayOfWeek":1,"startTime":"10:00","endTime":"12:00"}]}
                """).getAsJsonObject();
        var service=new com.tamo.service.CourseSectionService(com.tamo.service.JsonDataService.detached(ds));
        var section=service.create("u010","admin","m001",sectionBody);
        var request=com.google.gson.JsonParser.parseString("""
                {"periodId":"period_test","applicantId":"u001",
                 "reason":"Approved staffing exception","confirmPending":true,
                 "manualConfirmationNote":"Confirmed schedule with student."}
                """).getAsJsonObject();
        request.addProperty("sectionId",section.getId());
        var assignments=assignmentService(ds);
        var assignment=assignments.assignDirect("u010","admin",request);
        assignments.publish("u010","admin",assignment.getId(),new com.google.gson.JsonObject());
        store.save(ds);store.commit();store.close();store.begin();
        DataStore reloaded=store.load();
        var saved=reloaded.getSectionAssignments().stream()
                .filter(a->assignment.getId().equals(a.getId())).findFirst().orElseThrow();
        assertEquals(assignment.getId(),reloaded.getOfferEmails().get(0).id());
        assertEquals("published",saved.getPublicationStatus());
        assertNull(saved.getApplicationId());
        assertEquals("Approved staffing exception",saved.getDirectReason());
    }

    @Test void importsEveryCollectionAndPreservesBackup() throws Exception {
        store.begin();
        assertEquals(JsonUtil.getGson().toJsonTree(TestDataFactory.createTestDataStore()),
                JsonUtil.getGson().toJsonTree(store.load()));
        store.close();
        try (var files = Files.list(temp.resolve("backups"))) {
            Path backup = files.findFirst().orElseThrow();
            assertEquals(Files.readString(seed), Files.readString(backup));
        }
    }

    @Test void upgradesV6PendingOffersWithSevenDayGraceWithoutExpiringAcceptedOnes() throws Exception {
        store.begin();DataStore ds=store.load();CourseSection section=assignmentSection();section.setTaPositions(2);ds.getCourseSections().add(section);
        ds.getApplications().get(0).setStatus("offer");ds.getApplications().get(2).setStatus("offer");var service=assignmentService(ds);
        SectionAssignment pending=service.assign("u010","admin",assignmentBody("a001"));service.publish("u010","admin",pending.getId(),deadlineBody());
        SectionAssignment accepted=service.assign("u010","admin",assignmentBody("a003"));service.publish("u010","admin",accepted.getId(),deadlineBody());
        com.google.gson.JsonObject reply=new com.google.gson.JsonObject();reply.addProperty("decision","accept");service.respond("u002","applicant",accepted.getId(),reply);
        store.save(ds);store.commit();store.close();
        try(Connection conn=DriverManager.getConnection(url,user,password);Statement stmt=conn.createStatement()) {
            stmt.execute("ALTER TABLE section_assignments DROP COLUMN response_deadline CASCADE, DROP COLUMN expired_at CASCADE");stmt.execute("DELETE FROM tamo_schema_version WHERE version IN (7,15)");
        }
        Files.writeString(seed,"do not reimport");java.time.Instant before=java.time.Instant.now();store.initialize(seed,temp.resolve("backups"));store.begin();DataStore upgraded=store.load();
        SectionAssignment p=upgraded.getSectionAssignments().stream().filter(a->a.getId().equals(pending.getId())).findFirst().orElseThrow();
        assertEquals("pending",p.getResponseStatus());java.time.Instant deadline=java.time.Instant.parse(p.getResponseDeadline());assertTrue(deadline.isAfter(before.plusSeconds(6*86400)));assertTrue(deadline.isBefore(java.time.Instant.now().plusSeconds(8*86400)));
        SectionAssignment a=upgraded.getSectionAssignments().stream().filter(x->x.getId().equals(accepted.getId())).findFirst().orElseThrow();assertEquals("accepted",a.getResponseStatus());assertNull(a.getResponseDeadline());
    }
    @Test void publicationWithoutDeadlineAndLegacyReplyPersistAcrossReconnect() throws Exception {
        store.begin();DataStore ds=store.load();ds.getCourseSections().add(assignmentSection());ds.getApplications().get(0).setStatus("offer");var service=assignmentService(ds);
        SectionAssignment a=service.assign("u010","admin",assignmentBody("a001"));service.publish("u010","admin",a.getId(),new com.google.gson.JsonObject());assertNull(a.getResponseDeadline());
        store.save(ds);store.commit();store.close();
        store.begin();DataStore pending=store.load();SectionAssignment row=pending.getSectionAssignments().get(0);assertEquals("pending",row.getResponseStatus());assertNull(row.getResponseDeadline());
        row.setResponseDeadline("2020-01-01T00:00:00Z");store.save(pending);store.commit();store.close();
        store.begin();DataStore replyData=store.load();com.google.gson.JsonObject reply=new com.google.gson.JsonObject();reply.addProperty("decision","accept");
        assignmentService(replyData).respond("u001","applicant",a.getId(),reply);store.save(replyData);store.commit();store.close();
        store.begin();DataStore actual=store.load();SectionAssignment accepted=actual.getSectionAssignments().get(0);assertEquals("accepted",accepted.getResponseStatus());assertEquals("active",accepted.getStatus());assertNotNull(accepted.getRespondedAt());assertEquals(1,assignmentService(actual).occupied(a.getSectionId(),a.getPeriodId()));
    }

    @Test void v15RemovesMandatoryDeadlineWithoutDeletingLegacyMetadata() throws Exception {
        store.begin();DataStore ds=store.load();ds.getCourseSections().add(assignmentSection());ds.getApplications().get(0).setStatus("offer");var service=assignmentService(ds);
        SectionAssignment a=service.assign("u010","admin",assignmentBody("a001"));service.publish("u010","admin",a.getId(),new com.google.gson.JsonObject());a.setResponseDeadline("2020-01-01T00:00:00Z");
        store.save(ds);store.commit();store.close();
        try(Connection conn=DriverManager.getConnection(url,user,password);Statement stmt=conn.createStatement()) {
            stmt.execute("ALTER TABLE section_assignments ADD CONSTRAINT assignment_pending_deadline CHECK (status <> 'active' OR publication_status <> 'published' OR response_status <> 'pending' OR response_deadline IS NOT NULL)");
            stmt.execute("DELETE FROM tamo_schema_version WHERE version=15");
            assertThrows(SQLException.class,()->stmt.executeUpdate("UPDATE section_assignments SET response_deadline=NULL"));
        }
        store.initialize(seed,temp.resolve("backups"));store.begin();DataStore upgraded=store.load();SectionAssignment retained=upgraded.getSectionAssignments().get(0);assertEquals("2020-01-01T00:00:00Z",retained.getResponseDeadline());assertEquals("pending",retained.getResponseStatus());
        retained.setResponseDeadline(null);store.save(upgraded);store.commit();store.close();
        store.begin();assertNull(store.load().getSectionAssignments().get(0).getResponseDeadline());
    }

    @Test void upgradesExistingV5AssignmentsAsUnpublishedWithoutSendingNotifications() throws Exception {
        store.begin();DataStore ds=store.load();ds.getCourseSections().add(assignmentSection());ds.getApplications().get(0).setStatus("offer");
        assignmentService(ds).assign("u010","admin",assignmentBody("a001"));store.save(ds);store.commit();store.close();
        try (Connection conn=DriverManager.getConnection(url,user,password);Statement stmt=conn.createStatement()) {
            stmt.execute("ALTER TABLE section_assignments DROP COLUMN publication_status CASCADE, DROP COLUMN published_by CASCADE, DROP COLUMN published_at CASCADE, DROP COLUMN response_status CASCADE, DROP COLUMN responded_at CASCADE, DROP COLUMN response_reason CASCADE, DROP COLUMN response_deadline CASCADE, DROP COLUMN expired_at CASCADE");
            stmt.execute("DELETE FROM tamo_schema_version WHERE version>=6");
        }
        Files.writeString(seed,"do not import");store.initialize(seed,temp.resolve("backups"));store.begin();DataStore actual=store.load();
        assertEquals(1,actual.getSectionAssignments().size());SectionAssignment a=actual.getSectionAssignments().get(0);assertEquals("draft",a.getPublicationStatus());assertEquals("pending",a.getResponseStatus());assertNull(a.getPublishedAt());
        assertEquals(TestDataFactory.createTestDataStore().getNotifications().size(),actual.getNotifications().size());
    }
    @Test void publicationDeclineAndNotificationMetadataSurviveReconnect() throws Exception {
        store.begin();DataStore ds=store.load();ds.getCourseSections().add(assignmentSection());ds.getApplications().get(0).setStatus("offer");
        var service=assignmentService(ds);SectionAssignment a=service.assign("u010","admin",assignmentBody("a001"));service.publish("u010","admin",a.getId(),deadlineBody());
        com.google.gson.JsonObject reply=new com.google.gson.JsonObject();reply.addProperty("decision","decline");reply.addProperty("reason","Another commitment");service.respond("u001","applicant",a.getId(),reply);
        store.save(ds);store.commit();store.close();store.begin();DataStore actual=store.load();SectionAssignment saved=actual.getSectionAssignments().get(0);
        assertEquals("published",saved.getPublicationStatus());assertEquals("u010",saved.getPublishedBy());assertNotNull(saved.getPublishedAt());assertEquals("declined",saved.getResponseStatus());assertEquals("cancelled",saved.getStatus());assertEquals("Another commitment",saved.getResponseReason());assertNotNull(saved.getRespondedAt());
        assertEquals(2,actual.getNotifications().stream().filter(n->a.getId().equals(n.getRelatedId())).count());
    }
    @Test void concurrentRepliesProduceOneDecisionAndOneResponseNotification() throws Exception {
        store.begin();DataStore ds=store.load();ds.getCourseSections().add(assignmentSection());ds.getApplications().get(0).setStatus("offer");
        var service=assignmentService(ds);SectionAssignment a=service.assign("u010","admin",assignmentBody("a001"));service.publish("u010","admin",a.getId(),deadlineBody());store.save(ds);store.commit();store.close();
        store.begin();DataStore first=store.load();com.google.gson.JsonObject accept=new com.google.gson.JsonObject();accept.addProperty("decision","accept");assignmentService(first).respond("u001","applicant",a.getId(),accept);store.save(first);
        ExecutorService executor=Executors.newSingleThreadExecutor();CountDownLatch started=new CountDownLatch(1);
        try {
            Future<Integer> second=executor.submit(()->{
                PostgresStore other=new PostgresStore(url,user,password);started.countDown();
                try {other.begin();DataStore fresh=other.load();com.google.gson.JsonObject decline=new com.google.gson.JsonObject();decline.addProperty("decision","decline");decline.addProperty("reason","Conflicting reply");
                    try {assignmentService(fresh).respond("u001","applicant",a.getId(),decline);return 200;}catch(com.tamo.service.SectionAssignmentService.Problem e){return e.status;}
                } finally {other.close();}
            });
            assertTrue(started.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->second.get(200,TimeUnit.MILLISECONDS));store.commit();store.close();assertEquals(409,second.get(10,TimeUnit.SECONDS));
            store.begin();DataStore actual=store.load();assertEquals("accepted",actual.getSectionAssignments().get(0).getResponseStatus());assertEquals(1,actual.getNotifications().stream().filter(n->"assignment_response".equals(n.getType())&&a.getId().equals(n.getRelatedId())).count());
        }finally{store.close();executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
    }

    @Test void upgradesV4WithoutLosingDataOrImportingAssignments() throws Exception {
        store.close();
        try (Connection conn=DriverManager.getConnection(url,user,password); Statement stmt=conn.createStatement()) {
            stmt.execute("DROP TABLE review_records");
            stmt.execute("DROP TABLE offer_emails");
            stmt.execute("DROP TABLE section_assignments"); stmt.execute("DELETE FROM tamo_schema_version WHERE version>=5");
        }
        Files.writeString(seed,"must not reimport");store.initialize(seed,temp.resolve("backups"));
        store.begin();DataStore actual=store.load();
        assertEquals(TestDataFactory.createTestDataStore().getApplications().size(),actual.getApplications().size());
        assertTrue(actual.getSectionAssignments().isEmpty());store.close();
        store.initialize(seed,temp.resolve("backups"));store.begin();assertTrue(store.load().getSectionAssignments().isEmpty());
    }

    private CourseSection assignmentSection() {
        CourseSection s=JsonUtil.getGson().fromJson("{\"id\":\"assignment_section\",\"periodId\":\"period_test\",\"moduleId\":\"m001\",\"name\":\"Assignment lab\",\"type\":\"lab\",\"timeZone\":\"America/Los_Angeles\",\"meetingTimes\":[{\"dayOfWeek\":1,\"startTime\":\"10:00\",\"endTime\":\"12:00\"}],\"taPositions\":1,\"hoursPerWeek\":8.5,\"location\":\"Room 1\",\"notes\":\"\",\"status\":\"active\",\"createdAt\":\"now\",\"updatedAt\":\"now\"}",CourseSection.class);
        return s;
    }
    private com.tamo.service.SectionAssignmentService assignmentService(DataStore ds) {
        for(Application app:ds.getApplications())if("offer".equals(app.getStatus())&&!com.tamo.service.ReviewService.approved(ds,app.getId())){ReviewRecord selected=new ReviewRecord();selected.setId("fixture_approval_"+app.getId());selected.setApplicationId(app.getId());selected.setActorId("u010");selected.setAction("approve");selected.setNote("Synthetic selection approval");selected.setCreatedAt("2026-01-01T00:00:00Z");ds.getReviewRecords().add(selected);}
        com.tamo.service.JsonDataService data=org.mockito.Mockito.mock(com.tamo.service.JsonDataService.class);
        org.mockito.Mockito.when(data.getData()).thenReturn(ds);return new com.tamo.service.SectionAssignmentService(data);
    }
    private com.google.gson.JsonObject assignmentBody(String app) {
        com.google.gson.JsonObject b=new com.google.gson.JsonObject();b.addProperty("applicationId",app);b.addProperty("sectionId","assignment_section");b.addProperty("confirmPending",true);b.addProperty("manualConfirmationNote","Confirmed by local test");return b;
    }
    @Test void assignmentSnapshotsCancellationAndUniqueConstraintPersistAcrossConnections() throws Exception {
        store.begin();DataStore ds=store.load();ds.getCourseSections().add(assignmentSection());ds.getApplications().get(0).setStatus("offer");
        SectionAssignment assigned=assignmentService(ds).assign("u010","admin",assignmentBody("a001"));
        store.save(ds);store.commit();store.close();store.begin();ds=store.load();
        assertEquals(8.5,ds.getSectionAssignments().get(0).getSectionSnapshot().getHoursPerWeek());
        SectionAssignment duplicate=JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(assigned),SectionAssignment.class);duplicate.setId("duplicate");ds.getSectionAssignments().add(duplicate);
        DataStore invalid=ds;assertThrows(SQLException.class,()->store.save(invalid));store.close();store.begin();ds=store.load();assertEquals(1,ds.getSectionAssignments().size());
        com.google.gson.JsonObject reason=new com.google.gson.JsonObject();reason.addProperty("reason","Test cancellation");assignmentService(ds).cancel("u010","admin",assigned.getId(),reason);
        store.save(ds);store.commit();store.close();store.begin();
        SectionAssignment actual=store.load().getSectionAssignments().get(0);assertEquals("cancelled",actual.getStatus());assertEquals("Test cancellation",actual.getCancellationReason());assertNotNull(actual.getCancelledAt());
    }
    @Test void concurrentAssignmentsCannotExceedLastPosition() throws Exception {
        store.begin();DataStore ds=store.load();ds.getCourseSections().add(assignmentSection());
        ds.getApplications().stream().filter(a->"a001".equals(a.getId())||"a003".equals(a.getId())).forEach(a->a.setStatus("offer"));
        store.save(ds);store.commit();store.close();store.begin();DataStore first=store.load();
        assignmentService(first).assign("u010","admin",assignmentBody("a001"));store.save(first);
        ExecutorService executor=Executors.newSingleThreadExecutor();CountDownLatch started=new CountDownLatch(1);
        try {
            Future<Integer> second=executor.submit(()->{
                PostgresStore other=new PostgresStore(url,user,password);started.countDown();
                try {
                    other.begin();DataStore fresh=other.load();
                    try { assignmentService(fresh).assign("u010","admin",assignmentBody("a003"));return 201; }
                    catch(com.tamo.service.SectionAssignmentService.Problem e) {return e.status;}
                } finally {other.close();}
            });
            assertTrue(started.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->second.get(200,TimeUnit.MILLISECONDS));
            store.commit();store.close();assertEquals(409,second.get(10,TimeUnit.SECONDS));store.begin();assertEquals(1,store.load().getSectionAssignments().size());
        } finally {store.close();executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
    }

    @Test void upgradesV3WithoutChangingCoursesOrInventingSections() throws Exception {
        store.close();
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE review_records");
            stmt.execute("DROP TABLE offer_emails");
            stmt.execute("DROP TABLE section_assignments");
            stmt.execute("DELETE FROM tamo_schema_version WHERE version>=5");
            stmt.execute("DROP TABLE course_sections");
            stmt.execute("DELETE FROM tamo_schema_version WHERE version=4");
        }
        Files.writeString(seed, "must not reimport JSON");
        store.initialize(seed, temp.resolve("backups"));
        store.begin();
        DataStore actual = store.load();
        assertTrue(actual.getCourseSections().isEmpty());
        assertEquals(JsonUtil.getGson().toJsonTree(TestDataFactory.createTestDataStore().getModules()), JsonUtil.getGson().toJsonTree(actual.getModules()));
        assertEquals(TestDataFactory.createTestDataStore().getApplications().size(), actual.getApplications().size());
        store.close();
        store.initialize(seed, temp.resolve("backups"));
        store.begin(); assertTrue(store.load().getCourseSections().isEmpty());
    }

    @Test void sectionsMeetingsAndArchivedStateSurviveRoundTripAndUniqueNamesAreEnforced() throws Exception {
        store.begin();
        DataStore data = store.load();
        CourseSection section = new CourseSection(); section.setId("section_db"); section.setModuleId("m001");
        section.setName("Lab A"); section.setType("lab"); section.setTimeZone("America/Los_Angeles");
        CourseSection.MeetingTime meeting = new CourseSection.MeetingTime(); meeting.setDayOfWeek(2); meeting.setStartTime("14:00"); meeting.setEndTime("17:00");
        section.setMeetingTimes(java.util.List.of(meeting)); section.setTaPositions(2); section.setHoursPerWeek(8.5);
        section.setLocation("Test room"); section.setNotes("Synthetic notes"); section.setStatus("active");
        section.setCreatedAt("2026-10-16T19:00:00Z"); section.setUpdatedAt(section.getCreatedAt());
        data.getCourseSections().add(section); store.save(data); store.commit(); store.close();
        store.begin();
        DataStore reloaded = store.load();
        assertEquals(JsonUtil.getGson().toJsonTree(section), JsonUtil.getGson().toJsonTree(reloaded.getCourseSections().get(0)));
        reloaded.getCourseSections().get(0).setStatus("archived"); store.save(reloaded); store.commit(); store.close();
        store.begin();
        DataStore archived = store.load(); assertEquals("archived", archived.getCourseSections().get(0).getStatus());
        CourseSection duplicate = JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(section), CourseSection.class);
        duplicate.setId("section_duplicate"); duplicate.setName("lab a"); archived.getCourseSections().add(duplicate);
        assertThrows(SQLException.class, () -> store.save(archived)); store.close();
        store.begin(); assertEquals(1, store.load().getCourseSections().size());
    }

    @Test void upgradesV2WithoutInventingAvailabilityForExistingApplications() throws Exception {
        store.close();
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE weekly_availabilities");
            stmt.execute("ALTER TABLE applications DROP COLUMN availability_snapshot");
            stmt.execute("DELETE FROM tamo_schema_version WHERE version=3");
        }
        Files.writeString(seed, "must not reimport JSON");
        store.initialize(seed, temp.resolve("backups"));
        store.begin();
        DataStore actual = store.load();
        assertEquals(TestDataFactory.createTestDataStore().getApplications().size(), actual.getApplications().size());
        assertTrue(actual.getWeeklyAvailabilities().isEmpty());
        assertTrue(actual.getApplications().stream().allMatch(a -> a.getAvailabilitySnapshot() == null));
    }

    @Test void scheduleAndImmutableSnapshotSurviveDatabaseRoundTrip() throws Exception {
        store.begin();
        DataStore data = store.load();
        WeeklyAvailability saved = data.getWeeklyAvailabilities().get(0);
        saved.setNotes("Original notes");
        data.getApplications().get(0).setAvailabilitySnapshot(JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(saved), WeeklyAvailability.class));
        store.save(data); store.commit(); store.close();
        store.begin();
        DataStore reloaded = store.load();
        assertEquals("Original notes", reloaded.getApplications().get(0).getAvailabilitySnapshot().getNotes());
        reloaded.getWeeklyAvailabilities().get(0).setNotes("Updated notes");
        store.save(reloaded); store.commit(); store.close();
        store.begin();
        DataStore result = store.load();
        assertEquals("Updated notes", result.getWeeklyAvailabilities().get(0).getNotes());
        assertEquals("Original notes", result.getApplications().get(0).getAvailabilitySnapshot().getNotes());
    }

    @Test void upgradesExistingV1DatabaseWithoutReadingSeedOrLosingHistory() throws Exception {
        store.close();
        try (Connection conn = DriverManager.getConnection(baseUrl, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA " + schema + " CASCADE");
            stmt.execute("CREATE SCHEMA " + schema);
        }
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             var sql = PostgresStore.class.getResourceAsStream("/db/V1__initial.sql")) {
            stmt.execute(new String(sql.readAllBytes(), StandardCharsets.UTF_8));
            stmt.execute("INSERT INTO tamo_schema_version(version) VALUES (1)");
            stmt.execute("INSERT INTO users(id,name,email,row_order) VALUES ('old_user','Existing user','old@example.test',0)");
            stmt.execute("INSERT INTO jobs(id,title,row_order) VALUES ('old_job','Existing position',0)");
            stmt.execute("INSERT INTO applications(id,job_id,applicant_id,status,row_order) "
                    + "VALUES ('old_app','old_job','old_user','review',0)");
        }
        Files.writeString(seed, "must not import this file");
        store.initialize(seed, temp.resolve("backups"));
        store.begin();
        DataStore migrated = store.load();
        assertEquals("Existing user", migrated.getUsers().get(0).getName());
        assertEquals("review", migrated.getApplications().get(0).getStatus());
        assertEquals("period_legacy", migrated.getApplications().get(0).getPeriodId());
        assertTrue(migrated.getApplicationPeriods().get(0).getLegacy());
        assertEquals("closed", migrated.getApplicationPeriods().get(0).getStatus());
        store.close();
        store.initialize(seed, temp.resolve("backups"));
        store.begin();
        assertEquals(1, store.load().getApplicationPeriods().size());
    }

    @Test void savesUpdatesInsertsAndDeletesAcrossConnectionsWithoutReimport() throws Exception {
        store.begin();
        DataStore data = store.load();
        data.getUsers().get(0).setName("更新后的名字");
        User added = new User();
        added.setId("u999"); added.setName("New"); added.setEmail("new@example.test");
        added.setRole("applicant"); added.setPassword("test");
        data.getUsers().add(added);
        data.getMessages().remove(0);
        store.save(data); store.commit(); store.close();
        Files.writeString(seed, "invalid JSON must not be read again");
        PostgresStore reopened = new PostgresStore(url, user, password);
        reopened.initialize(seed, temp.resolve("backups"));
        try {
            reopened.begin();
            DataStore actual = reopened.load();
            assertEquals(JsonUtil.getGson().toJsonTree(data), JsonUtil.getGson().toJsonTree(actual));
            actual.getUsers().removeIf(u -> "u999".equals(u.getId()));
            reopened.save(actual); reopened.commit();
        } finally { reopened.close(); }
        store.begin();
        assertFalse(store.load().getUsers().stream().anyMatch(u -> "u999".equals(u.getId())));
    }

    @Test void closingWithoutCommitRollsBackAllChanges() throws Exception {
        store.begin();
        DataStore data = store.load();
        data.getUsers().get(0).setName("Must roll back");
        data.getJobs().get(0).setTitle("Must roll back too");
        store.save(data); store.close();
        store.begin();
        assertEquals(JsonUtil.getGson().toJsonTree(TestDataFactory.createTestDataStore()),
                JsonUtil.getGson().toJsonTree(store.load()));
    }

    @Test void duplicateEmailIsRejectedAndChangesRollBack() throws Exception {
        store.begin();
        DataStore data = store.load();
        data.getUsers().get(1).setEmail(data.getUsers().get(0).getEmail().toUpperCase());
        assertThrows(SQLException.class, () -> store.save(data));
        store.close(); store.begin();
        assertEquals("bob@university.edu", store.load().getUsers().get(1).getEmail());
    }

    @Test void foreignKeyFailureAtCommitRollsBackEarlierSaves() throws Exception {
        store.begin();
        DataStore data = store.load();
        data.getUsers().get(0).setName("Must roll back");
        data.getApplications().get(0).setApplicantId("missing-user");
        store.save(data);
        assertThrows(SQLException.class, store::commit);
        store.close(); store.begin();
        assertEquals("Alex Johnson", store.load().getUsers().get(0).getName());
    }

    @Test void secondWriterLoadsFirstWritersCommittedChanges() throws Exception {
        store.begin();
        DataStore first = store.load();
        first.getUsers().get(0).setName("First writer");
        store.save(first);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        try {
            Future<String> second = executor.submit(() -> {
                PostgresStore other = new PostgresStore(url, user, password);
                started.countDown();
                try {
                    other.begin();
                    DataStore data = other.load();
                    String name = data.getUsers().get(0).getName();
                    data.getUsers().get(1).setName("Second writer");
                    other.save(data); other.commit();
                    return name;
                } finally { other.close(); }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
            store.commit(); store.close();
            assertEquals("First writer", second.get(10, TimeUnit.SECONDS));
            store.begin();
            assertEquals("Second writer", store.load().getUsers().get(1).getName());
        } finally {
            store.close(); executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
    private com.google.gson.JsonObject deadlineBody() {
        com.google.gson.JsonObject b=new com.google.gson.JsonObject();b.addProperty("responseDeadline",java.time.Instant.now().plusSeconds(86400).toString());return b;
    }
}
