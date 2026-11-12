package com.tamo.service;

import com.google.gson.*;
import com.tamo.model.CourseSection;
import com.tamo.model.Module;
import com.tamo.model.User;
import com.tamo.util.JsonUtil;
import java.time.Clock;
import java.util.*;

/** Staff-maintained section schedules and staffing demand; no assignment or matching decisions. */
public class CourseSectionService {
    public static final String TIME_ZONE = "America/Los_Angeles";
    private final JsonDataService data;
    private final Clock clock;
    public CourseSectionService(JsonDataService data) { this(data, Clock.systemUTC()); }
    public CourseSectionService(JsonDataService data, Clock clock) { this.data = data; this.clock = clock; }

    public static class Problem extends RuntimeException {
        public final int status;
        public Problem(int status, String message) { super(message); this.status = status; }
    }

    private void staff(String role) {
        if (!"admin".equals(role) && !"organiser".equals(role)) throw new Problem(403, "Only authorized staff can manage course sections.");
    }
    private User actor(String userId,String role) {
        staff(role);
        return data.getData().getUsers().stream()
                .filter(u -> Objects.equals(userId,u.getId()) && role.equals(u.getRole()) && "active".equals(u.getStatus()))
                .findFirst().orElseThrow(() -> new Problem(403,"An active staff account is required."));
    }
    private void requireWrite(String userId,String role,Module module) {
        if(!ReviewAccess.canWrite(actor(userId,role),module,ReviewAccess.CONFIGURE_COURSE))
            throw new Problem(403,"Course configuration access is required for this course.");
    }
    private Module module(String userId, String role, String id) {
        actor(userId,role);
        Module module = data.getData().getModules().stream().filter(m -> m.getId().equals(id)).findFirst()
                .orElseThrow(() -> new Problem(404, "Course not found."));
        if (!"admin".equals(role) && !Objects.equals(userId, module.getOrganiserId())) throw new Problem(403, "You can only manage sections for your assigned courses.");
        return module;
    }
    public List<JsonObject> modules(String userId, String role) {
        actor(userId,role);
        return data.getData().getModules().stream().filter(m -> "admin".equals(role) || Objects.equals(userId, m.getOrganiserId()))
                .map(m -> JsonUtil.getGson().toJsonTree(m).getAsJsonObject()).toList();
    }
    public JsonObject list(String userId,String role,String moduleId){return list(userId,role,moduleId,null);}
    public JsonObject list(String userId, String role, String moduleId,String periodId) {
        Module module = module(userId, role, moduleId);
        List<CourseSection> sections = data.getData().getCourseSections().stream().filter(s -> moduleId.equals(s.getModuleId())).filter(s->periodId==null||("legacy_unscoped".equals(periodId)?s.getPeriodId()==null:periodId.equals(s.getPeriodId()))).toList();
        JsonObject result = new JsonObject();
        result.add("module", JsonUtil.getGson().toJsonTree(module));
        result.addProperty("canConfigure",ReviewAccess.canWrite(actor(userId,role),module,ReviewAccess.CONFIGURE_COURSE));
        result.add("sections", JsonUtil.getGson().toJsonTree(sections));
        result.addProperty("timeZone", TIME_ZONE);
        result.addProperty("taPositionsRequested", sections.stream().filter(s -> "active".equals(s.getStatus())).mapToInt(CourseSection::getTaPositions).sum());
        result.addProperty("weeklyHoursRequested", sections.stream().filter(s -> "active".equals(s.getStatus())).mapToDouble(s -> s.getTaPositions() * s.getHoursPerWeek()).sum());
        return result;
    }
    public CourseSection create(String userId, String role, String moduleId, JsonObject body) {
        Module course=module(userId, role, moduleId);
        requireWrite(userId,role,course);
        String period=text(body,"periodId",100,true);
        if(data.getData().getApplicationPeriods().stream().noneMatch(p->period.equals(p.getId())))throw new Problem(400,"Choose an existing application period.");
        CourseSection section = new CourseSection();
        section.setId("section_" + UUID.randomUUID().toString().replace("-", "")); section.setModuleId(moduleId);
        section.setPeriodId(period);section.setFacultyName(course.getOrganiserName());
        section.setStatus("active"); section.setCreatedAt(clock.instant().toString());
        configure(section, body);
        data.getData().getCourseSections().add(section); data.save(); return section;
    }
    private CourseSection section(String userId, String role, String id) {
        staff(role);
        CourseSection section = data.getData().getCourseSections().stream().filter(s -> s.getId().equals(id)).findFirst()
                .orElseThrow(() -> new Problem(404, "Section not found."));
        requireWrite(userId,role,module(userId,role,section.getModuleId()));
        return section;
    }
    public CourseSection update(String userId, String role, String id, JsonObject body) {
        CourseSection original = section(userId, role, id);
        if(original.getPeriodId()==null)throw new Problem(409,"Copy an unscoped historical section into a specific cycle before editing it.");
        if (SectionAssignmentService.activeForSection(data.getData(), id)) throw new Problem(409, "Cancel active TA assignments before editing this section.");
        if ("archived".equals(original.getStatus())) throw new Problem(409, "Restore this section before editing it.");
        CourseSection changed = JsonUtil.getGson().fromJson(JsonUtil.getGson().toJson(original), CourseSection.class);
        configure(changed, body);
        data.getData().getCourseSections().set(data.getData().getCourseSections().indexOf(original), changed);
        data.save(); return changed;
    }
    public CourseSection archive(String userId, String role, String id, boolean archived) {
        CourseSection section = section(userId, role, id);
        if (archived && SectionAssignmentService.activeForSection(data.getData(), id)) throw new Problem(409, "Cancel active TA assignments before archiving this section.");
        section.setStatus(archived ? "archived" : "active"); section.setUpdatedAt(clock.instant().toString());
        data.save(); return section;
    }
    private void configure(CourseSection section, JsonObject body) {
        if (body == null) throw new Problem(400, "Section details are required.");
        if (body.has("moduleId") && !section.getModuleId().equals(text(body, "moduleId", 100, true))) throw new Problem(400, "A section cannot be moved to another course.");
        if(body.has("periodId")&&!Objects.equals(section.getPeriodId(),text(body,"periodId",100,true)))throw new Problem(400,"A section cannot move between cycles. Create a separate copy.");
        if(body.has("facultyName"))section.setFacultyName(text(body,"facultyName",200,false));
        if(body.has("matchingCriteria"))section.setMatchingCriteria(body.get("matchingCriteria").isJsonNull()?null:CriteriaMatchingService.validate(body.getAsJsonObject("matchingCriteria")));
        String name = text(body, "name", 120, true);
        if (data.getData().getCourseSections().stream().anyMatch(s -> !s.getId().equals(section.getId()) && s.getModuleId().equals(section.getModuleId()) && Objects.equals(s.getPeriodId(),section.getPeriodId()) && s.getName().equalsIgnoreCase(name))) throw new Problem(409, "A section with this name already exists in the course, including archived sections.");
        String type = text(body, "type", 30, true);
        if (!Set.of("lecture", "lab", "discussion").contains(type)) throw new Problem(400, "Choose Lecture, Lab or Discussion.");
        if (!TIME_ZONE.equals(text(body, "timeZone", 64, true))) throw new Problem(400, "Use the UCI timezone: " + TIME_ZONE + ".");
        int positions;
        double hours;
        try {
            if (!number(body, "taPositions") || !number(body, "hoursPerWeek")) throw new IllegalArgumentException();
            positions = body.get("taPositions").getAsBigDecimal().intValueExact(); hours = body.get("hoursPerWeek").getAsDouble();
        } catch (IllegalArgumentException | ArithmeticException e) { throw new Problem(400, "TA positions must be a whole number and hours must be numeric."); }
        if (positions < 0 || positions > 1000 || !Double.isFinite(hours) || hours < 0 || hours > 168) throw new Problem(400, "Use 0–1000 TA positions and 0–168 hours per TA per week.");
        if ((positions == 0) != (hours == 0)) throw new Problem(400, "Set both TA positions and hours to zero when no TA is required; otherwise both must be positive.");
        List<CourseSection.MeetingTime> meetings = meetings(body);
        String location = text(body, "location", 200, false), notes = text(body, "notes", 2000, false);
        section.setName(name); section.setType(type); section.setTimeZone(TIME_ZONE); section.setMeetingTimes(meetings);
        section.setTaPositions(positions); section.setHoursPerWeek(hours); section.setLocation(location); section.setNotes(notes);
        section.setUpdatedAt(clock.instant().toString());
    }
    private static boolean number(JsonObject body, String key) { return body.has(key) && body.get(key).isJsonPrimitive() && body.get(key).getAsJsonPrimitive().isNumber(); }
    private static String text(JsonObject body, String key, int limit, boolean required) {
        if (!body.has(key) && !required) return "";
        JsonElement value = body.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new Problem(400, key + " must be text.");
        String text = value.getAsString().trim();
        if ((required && text.isEmpty()) || text.length() > limit) throw new Problem(400, key + " is required and must be at most " + limit + " characters.");
        return text;
    }
    private static List<CourseSection.MeetingTime> meetings(JsonObject body) {
        if (!body.has("meetingTimes") || !body.get("meetingTimes").isJsonArray()) throw new Problem(400, "Add at least one weekly meeting time.");
        JsonArray rows = body.getAsJsonArray("meetingTimes");
        if (rows.isEmpty() || rows.size() > 70) throw new Problem(400, "Use between 1 and 70 meeting times.");
        List<CourseSection.MeetingTime> meetings = new ArrayList<>();
        for (JsonElement value : rows) {
            if (!value.isJsonObject()) throw new Problem(400, "Each meeting time must be an object.");
            JsonObject row = value.getAsJsonObject();
            if (!row.has("dayOfWeek") || !row.get("dayOfWeek").toString().matches("[1-7]")) throw new Problem(400, "Choose a day from Monday (1) to Sunday (7).");
            String start = text(row, "startTime", 5, true), end = text(row, "endTime", 5, true);
            if (!start.matches("(?:[01]\\d|2[0-3]):[0-5]\\d") || !end.matches("(?:[01]\\d|2[0-3]):[0-5]\\d|24:00")) throw new Problem(400, "Use 24-hour HH:mm times. Only an end time may be 24:00.");
            if (minutes(start) >= minutes(end)) throw new Problem(400, "Start time must be before end time. Split overnight meetings across two days.");
            CourseSection.MeetingTime meeting = new CourseSection.MeetingTime();
            meeting.setDayOfWeek(row.get("dayOfWeek").getAsInt()); meeting.setStartTime(start); meeting.setEndTime(end); meetings.add(meeting);
        }
        meetings.sort(Comparator.comparingInt(CourseSection.MeetingTime::getDayOfWeek).thenComparing(CourseSection.MeetingTime::getStartTime));
        for (int i = 1; i < meetings.size(); i++) {
            var previous = meetings.get(i - 1); var next = meetings.get(i);
            if (previous.getDayOfWeek() == next.getDayOfWeek() && minutes(previous.getEndTime()) > minutes(next.getStartTime())) throw new Problem(400, "Meeting times within a section cannot overlap or repeat.");
        }
        return meetings;
    }
    private static int minutes(String value) { return Integer.parseInt(value.substring(0, 2)) * 60 + Integer.parseInt(value.substring(3)); }
}
