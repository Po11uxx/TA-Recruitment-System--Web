package com.tamo.service;

import com.google.gson.*;
import com.tamo.model.*;
import java.util.*;

/** Read-only scheduling advice using only the application's submitted snapshot. */
public class SectionMatchingService {
    public JsonObject match(DataStore data, Application application, Job job, String organiserId) {
        JsonObject result = new JsonObject();
        JsonArray sections = new JsonArray();
        result.add("sections", sections);
        result.addProperty("timeZone", AvailabilityService.TIME_ZONE);
        result.addProperty("basis", "Submitted availability snapshot compared with current active section times. This is scheduling advice, not an assignment.");
        // A job's old owner must not gain access to sections after course ownership changes.
        boolean owned = job != null && data.getModules().stream().anyMatch(m -> Objects.equals(m.getId(), job.getModuleId())
                && Objects.equals(m.getOrganiserId(), organiserId));
        if (!owned) {
            result.addProperty("status", "pending");
            result.addProperty("message", "Current course ownership could not be confirmed.");
            return result;
        }
        for (CourseSection section : data.getCourseSections()) {
            if (!JobSectionService.includes(job, section)) continue;
            if(section.getPeriodId()!=null&&!Objects.equals(section.getPeriodId(),application.getPeriodId()))continue;
            if(section.getPeriodId()==null&&!"period_legacy".equals(application.getPeriodId()))continue;
            if (!Objects.equals(section.getModuleId(), job.getModuleId()) || !"active".equals(section.getStatus())) continue;
            JsonObject row = new JsonObject();
            row.addProperty("sectionId", section.getId()); row.addProperty("name", section.getName());
            row.addProperty("type", section.getType()); row.addProperty("moduleCode", job.getModuleCode());
            row.addProperty("taPositions", section.getTaPositions());
            row.add("meetingTimes", com.tamo.util.JsonUtil.getGson().toJsonTree(section.getMeetingTimes()));
            row.add("qualificationMatching",new CriteriaMatchingService().evaluate(application,job,section));
            evaluate(row,application.getAvailabilitySnapshot(),application.getApplicantId(),application.getPeriodId(),section);
            sections.add(row);
        }
        boolean available = false, pending = false;
        for (JsonElement element : sections) {
            String status = element.getAsJsonObject().get("status").getAsString();
            available |= "available".equals(status); pending |= "pending".equals(status);
        }
        result.addProperty("status", available ? "available" : pending || sections.isEmpty() ? "pending" : "conflict");
        result.addProperty("message", sections.isEmpty() ? "No active sections have been configured for this course."
                : "Results apply to each section separately; combined workload and assignments require organiser review.");
        return result;
    }

    String availabilityStatus(WeeklyAvailability schedule,String applicantId,CourseSection section) {
        JsonObject result=new JsonObject();
        evaluate(result,schedule,applicantId,section.getPeriodId(),section);
        return result.get("status").getAsString();
    }

    private void evaluate(JsonObject row,WeeklyAvailability snapshot,String applicantId,String periodId,CourseSection section) {
        JsonArray issues = new JsonArray(); row.add("issues", issues);
        if (snapshot == null) { pending(row, "No submitted availability snapshot."); return; }
        if (!Objects.equals(applicantId, snapshot.getApplicantId())
                || periodId == null || !Objects.equals(periodId, snapshot.getPeriodId())
                || !AvailabilityService.TIME_ZONE.equals(snapshot.getTimeZone())
                || !AvailabilityService.TIME_ZONE.equals(section.getTimeZone())) {
            pending(row, "Snapshot identity, application period or time zone could not be confirmed."); return;
        }
        try {
            boolean conflictOnly=snapshot.getConflictsOnly();
            boolean[][] available = slots(snapshot.getAvailableSlots());
            boolean[][] conflicts = slots(snapshot.getConflictSlots());
            if (section.getMeetingTimes() == null || section.getMeetingTimes().isEmpty()) throw new IllegalArgumentException();
            for (CourseSection.MeetingTime meeting : section.getMeetingTimes()) {
                int day = meeting.getDayOfWeek(), start = minute(meeting.getStartTime()), end = minute(meeting.getEndTime());
                validate(day, start, end);
                int from = start;
                while (from < end) {
                    String reason = reason(snapshot.getNoAvailability(), conflictOnly||available[day][from], conflicts[day][from]);
                    int to = from + 1;
                    while (to < end && Objects.equals(reason, reason(snapshot.getNoAvailability(), conflictOnly||available[day][to], conflicts[day][to]))) to++;
                    if (reason != null) {
                        JsonObject issue = new JsonObject(); issue.addProperty("dayOfWeek", day);
                        issue.addProperty("startTime", time(from)); issue.addProperty("endTime", time(to));
                        issue.addProperty("reason", reason); issues.add(issue);
                    }
                    from = to;
                }
            }
            row.addProperty("status", issues.isEmpty() ? (conflictOnly?"pending":"available") : "conflict");
            row.addProperty("message", issues.isEmpty() ? (conflictOnly?"No conflict reported in imported CSV; full availability requires confirmation.":"All meeting times are covered by submitted availability.")
                    : "Some meeting times conflict with submitted availability.");
        } catch (IllegalArgumentException | NullPointerException ex) {
            issues = new JsonArray(); row.add("issues", issues);
            pending(row, "Schedule data is incomplete or invalid; manual confirmation is required.");
        }
    }
    private String reason(boolean none, boolean available, boolean conflict) {
        if (none) return "Applicant declared no availability";
        if (conflict) return "Explicit time conflict";
        return available ? null : "Outside submitted available times";
    }
    private boolean[][] slots(List<WeeklyAvailability.Slot> slots) {
        if (slots == null) throw new IllegalArgumentException();
        boolean[][] result = new boolean[8][1440];
        for (WeeklyAvailability.Slot slot : slots) {
            int start = minute(slot.getStartTime()), end = minute(slot.getEndTime());
            validate(slot.getDayOfWeek(), start, end);
            Arrays.fill(result[slot.getDayOfWeek()], start, end, true);
        }
        return result;
    }
    private void validate(int day, int start, int end) {
        if (day < 1 || day > 7 || start >= end || start >= 1440) throw new IllegalArgumentException();
    }
    private int minute(String time) {
        if (time == null || !time.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]|24:00")) throw new IllegalArgumentException();
        return Integer.parseInt(time.substring(0, 2)) * 60 + Integer.parseInt(time.substring(3));
    }
    private String time(int minute) { return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60); }
    private void pending(JsonObject row, String message) { row.addProperty("status", "pending"); row.addProperty("message", message); }
}
