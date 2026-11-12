package com.tamo.service;

import com.google.gson.*;
import com.tamo.model.*;
import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseSectionServiceTest {
    private JsonDataService data;
    private DataStore ds;
    private CourseSectionService service;
    @BeforeEach void setup() {
        data = mock(JsonDataService.class); ds = TestDataFactory.createTestDataStore();
        when(data.getData()).thenReturn(ds);
        service = new CourseSectionService(data, Clock.fixed(Instant.parse("2026-10-16T19:00:00Z"), ZoneOffset.UTC));
    }
    private JsonObject body() {
        return JsonParser.parseString("{\"periodId\":\"period_test\",\"name\":\"Lab A\",\"type\":\"lab\",\"timeZone\":\"America/Los_Angeles\",\"meetingTimes\":[{\"dayOfWeek\":3,\"startTime\":\"14:00\",\"endTime\":\"17:00\"},{\"dayOfWeek\":1,\"startTime\":\"10:00\",\"endTime\":\"10:50\"}],\"taPositions\":2,\"hoursPerWeek\":8.5,\"location\":\"Room 1\",\"notes\":\"Test notes\"}").getAsJsonObject();
    }
    @Test void ownerCanCreateMultipleMeetingsAndDemandTotalsArePerTa() {
        CourseSection section = service.create("u005", "organiser", "m001", body());
        assertEquals("active", section.getStatus()); assertEquals(1, section.getMeetingTimes().get(0).getDayOfWeek());
        JsonObject result = service.list("u005", "organiser", "m001");
        assertEquals(2, result.get("taPositionsRequested").getAsInt());
        assertEquals(17, result.get("weeklyHoursRequested").getAsDouble());
        verify(data).save();
    }
    @Test void adminReadsAllCoursesButConfiguresOnlyDelegatedCourses() {
        assertEquals(1, service.modules("u005", "organiser").size());
        assertEquals(ds.getModules().size(), service.modules("u010", "admin").size());
        assertFalse(service.list("u010","admin","m002").get("canConfigure").getAsBoolean());
        assertEquals(403,assertThrows(CourseSectionService.Problem.class,()->service.create("u010","admin","m002",body())).status);
        User admin=ReviewAccess.user(ds,"u010");
        admin.setPermissions(java.util.List.of(ReviewAccess.CONFIGURE_COURSE));
        admin.setCourseIds(java.util.List.of("m002"));
        assertTrue(service.list("u010","admin","m002").get("canConfigure").getAsBoolean());
        assertEquals(403,assertThrows(CourseSectionService.Problem.class,()->service.create("u010","admin","m001",body())).status);
        CourseSection section = service.create("u010", "admin", "m002", body());
        assertEquals("m002", section.getModuleId());
    }
    @Test void nonOwnerCannotListCreateUpdateArchiveOrRestoreEvenWithForgedModule() {
        CourseSection section = service.create("u005", "organiser", "m001", body());
        assertEquals(403, assertThrows(CourseSectionService.Problem.class, () -> service.list("u006", "organiser", "m001")).status);
        assertThrows(CourseSectionService.Problem.class, () -> service.create("u006", "organiser", "m001", body()));
        JsonObject forged = body(); forged.addProperty("moduleId", "m002");
        assertThrows(CourseSectionService.Problem.class, () -> service.update("u006", "organiser", section.getId(), forged));
        assertThrows(CourseSectionService.Problem.class, () -> service.archive("u006", "organiser", section.getId(), true));
        assertThrows(CourseSectionService.Problem.class, () -> service.archive("u006", "organiser", section.getId(), false));
    }
    @Test void applicantCannotReadOrWriteSectionManagement() {
        assertThrows(CourseSectionService.Problem.class, () -> service.modules("u001", "applicant"));
        assertThrows(CourseSectionService.Problem.class, () -> service.list("u001", "applicant", "m001"));
        assertThrows(CourseSectionService.Problem.class, () -> service.create("u001", "applicant", "m001", body()));
    }
    @Test void archiveRemovesDemandAndRestoreKeepsScheduleAndId() {
        CourseSection section = service.create("u005", "organiser", "m001", body());
        service.archive("u005", "organiser", section.getId(), true);
        assertEquals(0, service.list("u005", "organiser", "m001").get("taPositionsRequested").getAsInt());
        assertEquals(1, service.list("u005", "organiser", "m001").getAsJsonArray("sections").size());
        assertThrows(CourseSectionService.Problem.class, () -> service.update("u005", "organiser", section.getId(), body()));
        CourseSection restored = service.archive("u005", "organiser", section.getId(), false);
        assertEquals(section.getId(), restored.getId()); assertEquals(2, restored.getMeetingTimes().size());
        assertEquals(2, service.list("u005", "organiser", "m001").get("taPositionsRequested").getAsInt());
    }
    @Test void duplicateNameIsCaseInsensitiveAndReservedWhileArchived() {
        CourseSection original = service.create("u005", "organiser", "m001", body());
        service.archive("u005", "organiser", original.getId(), true);
        JsonObject duplicate = body(); duplicate.addProperty("name", "lab a");
        assertEquals(409, assertThrows(CourseSectionService.Problem.class, () -> service.create("u005", "organiser", "m001", duplicate)).status);
        assertDoesNotThrow(() -> service.create("u006", "organiser", "m002", duplicate));
    }
    @Test void failedEditDoesNotMutateOriginalAndCannotMoveCourse() {
        CourseSection original = service.create("u005", "organiser", "m001", body());
        JsonObject invalid = body(); invalid.addProperty("name", "Changed"); invalid.addProperty("taPositions", -1);
        assertThrows(CourseSectionService.Problem.class, () -> service.update("u005", "organiser", original.getId(), invalid));
        assertEquals("Lab A", ds.getCourseSections().get(0).getName());
        JsonObject moved = body(); moved.addProperty("moduleId", "m002");
        assertThrows(CourseSectionService.Problem.class, () -> service.update("u010", "admin", original.getId(), moved));
    }
    @Test void invalidTypesTimezoneStaffingAndFractionalPositionsAreRejected() {
        for (String change : new String[]{"{\"type\":\"exam\"}", "{\"timeZone\":\"UTC\"}", "{\"taPositions\":1.5}", "{\"taPositions\":0}", "{\"hoursPerWeek\":0}", "{\"hoursPerWeek\":169}", "{\"taPositions\":\"2\"}"}) {
            JsonObject invalid = body(); JsonParser.parseString(change).getAsJsonObject().entrySet().forEach(e -> invalid.add(e.getKey(), e.getValue()));
            assertThrows(CourseSectionService.Problem.class, () -> service.create("u005", "organiser", "m001", invalid));
        }
        assertTrue(ds.getCourseSections().isEmpty());
        verify(data, never()).save();
    }
    @Test void lectureMayHaveNoTaAndMultipleDays() {
        JsonObject lecture = body(); lecture.addProperty("type", "lecture"); lecture.addProperty("taPositions", 0); lecture.addProperty("hoursPerWeek", 0);
        assertEquals(0, service.create("u005", "organiser", "m001", lecture).getTaPositions());
    }
    @Test void invalidMeetingTimesAndOverlapsAreRejectedWhileAdjacentSlotsAreAllowed() {
        for (String meetings : new String[]{"[]", "[{\"dayOfWeek\":8,\"startTime\":\"10:00\",\"endTime\":\"11:00\"}]", "[{\"dayOfWeek\":1,\"startTime\":\"23:00\",\"endTime\":\"01:00\"}]", "[{\"dayOfWeek\":1,\"startTime\":\"9:00\",\"endTime\":\"11:00\"}]", "[{\"dayOfWeek\":1,\"startTime\":\"10:00\",\"endTime\":\"12:00\"},{\"dayOfWeek\":1,\"startTime\":\"11:00\",\"endTime\":\"13:00\"}]"}) {
            JsonObject invalid = body(); invalid.add("meetingTimes", JsonParser.parseString(meetings));
            assertThrows(CourseSectionService.Problem.class, () -> service.create("u005", "organiser", "m001", invalid));
        }
        JsonObject valid = body(); valid.add("meetingTimes", JsonParser.parseString("[{\"dayOfWeek\":1,\"startTime\":\"20:00\",\"endTime\":\"22:00\"},{\"dayOfWeek\":1,\"startTime\":\"22:00\",\"endTime\":\"24:00\"}]"));
        assertDoesNotThrow(() -> service.create("u005", "organiser", "m001", valid));
    }
    @Test void courseDeletionCannotOrphanActiveOrArchivedSections() {
        CourseSection section = service.create("u005", "organiser", "m001", body());
        AdminService admin = new AdminService(data);
        assertEquals(409, admin.deleteModule("m001").get("code"));
        service.archive("u005", "organiser", section.getId(), true);
        assertEquals(409, admin.deleteFromCollection("modules", "m001").get("code"));
        assertTrue(ds.getModules().stream().anyMatch(m -> "m001".equals(m.getId())));
    }
    @Test void sameCourseSectionNamesAreIndependentAcrossCyclesAndCannotBeMoved() {
        ApplicationPeriod p=new ApplicationPeriod();p.setId("another_cycle");ds.getApplicationPeriods().add(p);
        CourseSection first=service.create("u005","organiser","m001",body());JsonObject secondBody=body();secondBody.addProperty("periodId","another_cycle");
        CourseSection second=service.create("u005","organiser","m001",secondBody);assertNotEquals(first.getId(),second.getId());
        assertEquals(1,service.list("u005","organiser","m001","period_test").getAsJsonArray("sections").size());
        assertEquals(1,service.list("u005","organiser","m001","another_cycle").getAsJsonArray("sections").size());
        assertEquals(400,assertThrows(CourseSectionService.Problem.class,()->service.update("u005","organiser",first.getId(),secondBody)).status);
    }
}
