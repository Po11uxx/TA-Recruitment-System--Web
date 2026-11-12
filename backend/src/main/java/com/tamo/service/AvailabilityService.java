package com.tamo.service;

import com.google.gson.*;
import com.tamo.model.*;
import com.tamo.util.JsonUtil;
import java.time.Clock;
import java.util.*;

/** Campus-local weekly times; never substitute a current schedule for a submitted snapshot. */
public class AvailabilityService {
    public static final String TIME_ZONE = "America/Los_Angeles";
    private final JsonDataService data;
    private final Clock clock;
    public AvailabilityService(JsonDataService data) { this(data, Clock.systemUTC()); }
    public AvailabilityService(JsonDataService data, Clock clock) { this.data = data; this.clock = clock; }

    public static class Problem extends RuntimeException {
        public final int status;
        public Problem(int status, String message) { super(message); this.status = status; }
    }

    private ApplicationPeriod period(String id) {
        return data.getData().getApplicationPeriods().stream().filter(p -> p.getId().equals(id)).findFirst()
                .orElseThrow(() -> new Problem(404, "Application period not found."));
    }

    private boolean editable(ApplicationPeriod p) {
        return !p.getLegacy() && "open".equals(new ApplicationPeriodService(data, clock).effectiveStatus(p));
    }

    public List<JsonObject> periods() {
        ApplicationPeriodService periods = new ApplicationPeriodService(data, clock);
        return data.getData().getApplicationPeriods().stream()
                .filter(p -> !p.getLegacy() && !"draft".equals(p.getStatus())).map(p -> {
                    JsonObject row = periods.summary(p);
                    row.remove("applicationCount"); row.addProperty("editable", editable(p)); return row;
                }).toList();
    }

    private WeeklyAvailability find(String userId, String periodId) {
        return data.getData().getWeeklyAvailabilities().stream()
                .filter(a -> userId.equals(a.getApplicantId()) && periodId.equals(a.getPeriodId())).findFirst().orElse(null);
    }

    public JsonObject get(String userId, String periodId) {
        ApplicationPeriod p = period(periodId);
        if ("draft".equals(p.getStatus())) throw new Problem(404, "Application period is not published.");
        JsonObject result = new JsonObject();
        result.addProperty("editable", editable(p)); result.addProperty("timeZone", TIME_ZONE);
        result.add("availability", JsonUtil.getGson().toJsonTree(find(userId, periodId)));
        return result;
    }

    public WeeklyAvailability save(String userId, String periodId, JsonObject body) {
        if (!editable(period(periodId))) throw new Problem(403, "Availability can only be updated while this application period is open.");
        if (body == null) throw new Problem(400, "Availability details are required.");
        if (!TIME_ZONE.equals(text(body, "timeZone", 64))) throw new Problem(400, "Use the UCI timezone: " + TIME_ZONE + ".");
        List<WeeklyAvailability.Slot> available;
        List<WeeklyAvailability.Slot> conflicts;
        boolean none;
        if (body.has("confirmUnmarkedAvailable")) {
            JsonElement confirmation = body.get("confirmUnmarkedAvailable");
            if (!confirmation.isJsonPrimitive() || !confirmation.getAsJsonPrimitive().isBoolean() || !confirmation.getAsBoolean()) {
                throw new Problem(400, "Confirm that unmarked times are available before saving.");
            }
            conflicts = slots(body, "conflictSlots", true, 512);
            available = complement(conflicts);
            none = available.isEmpty();
        } else {
            available = slots(body, "availableSlots", true);
            conflicts = slots(body, "conflictSlots", false);
            none = false;
            if (body.has("noAvailability")) {
                JsonElement value = body.get("noAvailability");
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new Problem(400, "noAvailability must be true or false.");
                none = value.getAsBoolean();
            }
            if (available.isEmpty() && !none) throw new Problem(400, "Add an available time slot or confirm that you have no availability this period.");
            if (none && !available.isEmpty()) throw new Problem(400, "Remove available slots before selecting no availability.");
            for (var a : available) for (var c : conflicts) {
                if (overlaps(a, c)) throw new Problem(400, "Available times cannot overlap known conflicts. Adjust the available slots.");
            }
        }
        WeeklyAvailability previous = find(userId, periodId);
        WeeklyAvailability record = new WeeklyAvailability();
        record.setId(previous == null ? "availability_" + UUID.randomUUID().toString().replace("-", "") : previous.getId());
        record.setApplicantId(userId); record.setPeriodId(periodId); record.setTimeZone(TIME_ZONE);
        record.setAvailableSlots(available); record.setConflictSlots(conflicts); record.setNoAvailability(none);
        record.setNotes(text(body, "notes", 2000)); record.setUpdatedAt(clock.instant().toString());
        List<WeeklyAvailability> records = data.getData().getWeeklyAvailabilities();
        if (previous == null) records.add(record); else records.set(records.indexOf(previous), record);
        data.save();
        return record;
    }

    public WeeklyAvailability submissionSnapshot(String userId, String periodId) {
        WeeklyAvailability record = find(userId, periodId);
        if (record == null) throw new Problem(400, "Save your weekly availability for this application period before submitting.");
        return JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(record), WeeklyAvailability.class);
    }

    private static String text(JsonObject body, String key, int maxLength) {
        if (!body.has(key)) return "";
        JsonElement value = body.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new Problem(400, key + " must be text.");
        String result = value.getAsString().trim();
        if (result.length() > maxLength) throw new Problem(400, key + " is too long (maximum " + maxLength + " characters).");
        return result;
    }

    private static List<WeeklyAvailability.Slot> complement(List<WeeklyAvailability.Slot> conflicts) {
        String opening = "08:00", closing = "20:00";
        List<WeeklyAvailability.Slot> available = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            String start = opening;
            for (var busy : conflicts) {
                if (busy.getDayOfWeek() != day || busy.getEndTime().compareTo(opening) <= 0
                        || busy.getStartTime().compareTo(closing) >= 0) continue;
                addAvailable(available, day, start,
                        busy.getStartTime().compareTo(opening) < 0 ? opening : busy.getStartTime());
                start = busy.getEndTime().compareTo(closing) > 0 ? closing : busy.getEndTime();
            }
            addAvailable(available, day, start, closing);
        }
        return available;
    }

    private static void addAvailable(List<WeeklyAvailability.Slot> slots, int day, String start, String end) {
        if (start.compareTo(end) >= 0) return;
        WeeklyAvailability.Slot slot = new WeeklyAvailability.Slot();
        slot.setDayOfWeek(day); slot.setStartTime(start); slot.setEndTime(end); slot.setReason("");
        slots.add(slot);
    }

    private static List<WeeklyAvailability.Slot> slots(JsonObject body, String key, boolean required) {
        return slots(body, key, required, 70);
    }

    private static List<WeeklyAvailability.Slot> slots(JsonObject body, String key, boolean required, int limit) {
        if (!body.has(key) && !required) return new ArrayList<>();
        if (!body.has(key) || !body.get(key).isJsonArray()) throw new Problem(400, key + " must be an array.");
        JsonArray rows = body.getAsJsonArray(key);
        if (rows.size() > limit) throw new Problem(400, "Too many time slots.");
        List<WeeklyAvailability.Slot> result = new ArrayList<>();
        for (JsonElement value : rows) {
            if (!value.isJsonObject()) throw new Problem(400, "Each time slot must be an object.");
            JsonObject row = value.getAsJsonObject();
            if (!row.has("dayOfWeek") || !row.get("dayOfWeek").toString().matches("[1-7]")) throw new Problem(400, "Choose a day from Monday (1) through Sunday (7).");
            String start = text(row, "startTime", 5), end = text(row, "endTime", 5);
            if (!start.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")
                    || !end.matches("(?:[01]\\d|2[0-3]):[0-5]\\d|24:00")) throw new Problem(400, "Use 24-hour HH:mm times. Only an end time may be 24:00.");
            if (minutes(start) >= minutes(end)) throw new Problem(400, "Start time must be before end time. Split overnight availability into separate days.");
            WeeklyAvailability.Slot slot = new WeeklyAvailability.Slot();
            slot.setDayOfWeek(row.get("dayOfWeek").getAsInt()); slot.setStartTime(start); slot.setEndTime(end);
            slot.setReason(text(row, "reason", 200)); result.add(slot);
        }
        result.sort(Comparator.comparingInt(WeeklyAvailability.Slot::getDayOfWeek).thenComparing(WeeklyAvailability.Slot::getStartTime));
        for (int i = 1; i < result.size(); i++) if (overlaps(result.get(i - 1), result.get(i))) throw new Problem(400, "Time slots on the same day must not overlap or repeat.");
        return result;
    }

    private static int minutes(String time) { return Integer.parseInt(time.substring(0, 2)) * 60 + Integer.parseInt(time.substring(3)); }
    private static boolean overlaps(WeeklyAvailability.Slot a, WeeklyAvailability.Slot b) {
        return a.getDayOfWeek() == b.getDayOfWeek() && minutes(a.getStartTime()) < minutes(b.getEndTime())
                && minutes(b.getStartTime()) < minutes(a.getEndTime());
    }
}
