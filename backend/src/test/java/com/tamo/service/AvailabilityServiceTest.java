package com.tamo.service;

import com.google.gson.*;
import com.tamo.model.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AvailabilityServiceTest {
    private JsonDataService data;
    private DataStore ds;
    private AvailabilityService service;
    @BeforeEach void setup() {
        data = mock(JsonDataService.class); ds = TestDataFactory.createTestDataStore();
        when(data.getData()).thenReturn(ds);
        service = new AvailabilityService(data, Clock.fixed(Instant.parse("2026-10-16T19:00:00Z"), ZoneOffset.UTC));
    }
    private JsonObject body() {
        return JsonParser.parseString("{\"timeZone\":\"America/Los_Angeles\",\"availableSlots\":[{\"dayOfWeek\":2,\"startTime\":\"09:00\",\"endTime\":\"12:00\"},{\"dayOfWeek\":2,\"startTime\":\"14:00\",\"endTime\":\"17:00\"}],\"conflictSlots\":[{\"dayOfWeek\":2,\"startTime\":\"12:00\",\"endTime\":\"14:00\",\"reason\":\"Class\"}],\"notes\":\"Test notes\"}").getAsJsonObject();
    }
    @Test void savesMultipleSlotsAndUpdatesOnlyCurrentStudent() {
        String other = new com.google.gson.Gson().toJson(service.get("u002", "period_test"));
        JsonObject body = body(); body.addProperty("applicantId", "u002");
        WeeklyAvailability saved = service.save("u001", "period_test", body);
        assertEquals("u001", saved.getApplicantId()); assertEquals(2, saved.getAvailableSlots().size());
        assertEquals("Class", saved.getConflictSlots().get(0).getReason());
        assertEquals(other, new com.google.gson.Gson().toJson(service.get("u002", "period_test")));
        verify(data).save();
    }
    @Test void studentWithoutRecordCannotSeeAnotherStudentsScheduleOrSubmit() {
        ds.getWeeklyAvailabilities().removeIf(a -> a.getApplicantId().equals("u001"));
        assertTrue(service.get("u001", "period_test").get("availability").isJsonNull());
        assertEquals(400, assertThrows(AvailabilityService.Problem.class, () -> service.submissionSnapshot("u001", "period_test")).status);
        ds.getApplications().clear();
        assertEquals(400, new ApplicantService(data).applyForJob("u001", "j001", "test", List.of()).get("code"));
    }
    @Test void explicitNoAvailabilityIsDifferentFromMissingData() {
        JsonObject empty = body(); empty.add("availableSlots", new JsonArray());
        assertThrows(AvailabilityService.Problem.class, () -> service.save("u001", "period_test", empty));
        empty.addProperty("noAvailability", true);
        assertTrue(service.save("u001", "period_test", empty).getNoAvailability());
        assertTrue(service.submissionSnapshot("u001", "period_test").getNoAvailability());
        empty.add("availableSlots", body().get("availableSlots"));
        assertThrows(AvailabilityService.Problem.class, () -> service.save("u001", "period_test", empty));
    }
    @Test void rejectsOverlapsDuplicatesAndAvailableConflictOverlap() {
        for (String key : List.of("availableSlots", "conflictSlots")) {
            JsonObject invalid = body(); JsonArray list = invalid.getAsJsonArray(key); list.add(list.get(0).deepCopy());
            assertThrows(AvailabilityService.Problem.class, () -> service.save("u001", "period_test", invalid));
        }
        JsonObject invalid = body(); invalid.getAsJsonArray("conflictSlots").get(0).getAsJsonObject().addProperty("startTime", "11:00");
        assertThrows(AvailabilityService.Problem.class, () -> service.save("u001", "period_test", invalid));
        verify(data, never()).save();
    }
    @Test void rejectsInvalidDaysTimesAndOvernightSlotsWithoutMutatingSavedRecord() {
        String original = service.get("u001", "period_test").toString();
        for (String invalid : List.of("9:00", "25:00", "09:99", "24:00", "18:00")) {
            JsonObject request = body(); request.getAsJsonArray("availableSlots").get(0).getAsJsonObject().addProperty("startTime", invalid);
            assertThrows(AvailabilityService.Problem.class, () -> service.save("u001", "period_test", request));
        }
        JsonObject badDay = body(); badDay.getAsJsonArray("availableSlots").get(0).getAsJsonObject().addProperty("dayOfWeek", 1.5);
        assertThrows(AvailabilityService.Problem.class, () -> service.save("u001", "period_test", badDay));
        assertEquals(original, service.get("u001", "period_test").toString());
    }
    @Test void allowsAdjacentSlotsAndEndOfDay() {
        JsonObject request = body(); request.add("conflictSlots", new JsonArray());
        request.getAsJsonArray("availableSlots").get(1).getAsJsonObject().addProperty("startTime", "12:00");
        request.getAsJsonArray("availableSlots").get(1).getAsJsonObject().addProperty("endTime", "24:00");
        assertEquals("24:00", service.save("u001", "period_test", request).getAvailableSlots().get(1).getEndTime());
    }
    @Test void closedScheduledAndExpiredPeriodsAreReadOnly() {
        ApplicationPeriod p = ds.getApplicationPeriods().get(0);
        for (String state : List.of("closed", "scheduled", "expired")) {
            p.setStatus(state.equals("closed") ? "closed" : "open");
            p.setOpensAt(state.equals("scheduled") ? "2090-01-01T00:00:00Z" : "2000-01-01T00:00:00Z");
            p.setClosesAt(state.equals("expired") ? "2020-01-01T00:00:00Z" : "2100-01-01T00:00:00Z");
            assertFalse(service.get("u001", p.getId()).get("editable").getAsBoolean());
            assertEquals(403, assertThrows(AvailabilityService.Problem.class, () -> service.save("u001", p.getId(), body())).status);
        }
    }
    @Test void periodsHaveSeparateRecordsAndCannotBorrowFromAnotherPeriod() {
        ApplicationPeriod p = new ApplicationPeriod(); p.setId("another"); p.setStatus("closed"); ds.getApplicationPeriods().add(p);
        assertTrue(service.get("u001", "another").get("availability").isJsonNull());
        assertThrows(AvailabilityService.Problem.class, () -> service.submissionSnapshot("u001", "another"));
    }
    @Test void submittedSnapshotNeverChangesWhenScheduleIsEdited() {
        ds.getApplications().clear(); when(data.generateId(anyString(), anyList())).thenReturn("snapshot_app");
        WeeklyAvailability before = service.save("u001", "period_test", body());
        Application application = (Application) new ApplicantService(data).applyForJob("u001", "j001", "test", List.of()).get("application");
        JsonObject updated = body(); updated.addProperty("notes", "New notes");
        service.save("u001", "period_test", updated);
        before.getAvailableSlots().get(0).setStartTime("01:00");
        assertEquals("Test notes", application.getAvailabilitySnapshot().getNotes());
        assertEquals("09:00", application.getAvailabilitySnapshot().getAvailableSlots().get(0).getStartTime());
        assertEquals("New notes", service.submissionSnapshot("u001", "period_test").getNotes());
    }
    @Test void rejectsInvalidTimezoneAndExcessiveNotes() {
        JsonObject request = body(); request.addProperty("timeZone", "UTC");
        assertThrows(AvailabilityService.Problem.class, () -> service.save("u001", "period_test", request));
        request.addProperty("timeZone", AvailabilityService.TIME_ZONE); request.addProperty("notes", "x".repeat(2001));
        assertThrows(AvailabilityService.Problem.class, () -> service.save("u001", "period_test", request));
    }

    @Test void rawUserDeletionIsBlockedAndPreservesStudentSchedules() {
        assertEquals(403, new AdminService(data).deleteFromCollection("users", "u001").get("code"));
        assertTrue(ds.getWeeklyAvailabilities().stream().anyMatch(a -> a.getApplicantId().equals("u001")));
        assertTrue(ds.getWeeklyAvailabilities().stream().anyMatch(a -> a.getApplicantId().equals("u002")));
    }
    @Test void confirmedBusySlotsDeriveAvailabilityWithoutChangingEarlierSnapshots() {
        WeeklyAvailability original = service.submissionSnapshot("u001", "period_test");
        JsonObject input = body(); input.remove("availableSlots"); input.addProperty("confirmUnmarkedAvailable", true);
        WeeklyAvailability saved = service.save("u001", "period_test", input);
        assertFalse(saved.getConflictsOnly());
        assertEquals(6, saved.getAvailableSlots().size());
        assertEquals("12:00", saved.getAvailableSlots().get(1).getEndTime());
        assertEquals("14:00", saved.getAvailableSlots().get(2).getStartTime());
        assertNotEquals(saved.getAvailableSlots().size(), original.getAvailableSlots().size());
    }
    @Test void confirmedEmptyAndFullyBusyWeeksAreDistinct() {
        JsonObject input = body(); input.remove("availableSlots"); input.addProperty("confirmUnmarkedAvailable", true);
        JsonArray busy = new JsonArray(); input.add("conflictSlots", busy);
        var clearWeek = service.save("u001", "period_test", input).getAvailableSlots();
        assertEquals(5, clearWeek.size());
        assertTrue(clearWeek.stream().allMatch(slot -> slot.getDayOfWeek() <= 5
                && "08:00".equals(slot.getStartTime()) && "20:00".equals(slot.getEndTime())));
        for (int day = 1; day <= 5; day++) {
            JsonObject slot = new JsonObject(); slot.addProperty("dayOfWeek", day);
            slot.addProperty("startTime", "07:50"); slot.addProperty("endTime", "20:10"); busy.add(slot);
        }
        assertTrue(service.save("u001", "period_test", input).getNoAvailability());
    }
}
