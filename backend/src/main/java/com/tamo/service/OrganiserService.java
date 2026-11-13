package com.tamo.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tamo.model.Application;
import com.tamo.model.DataStore;
import com.tamo.model.Job;
import com.tamo.model.Module;
import com.tamo.model.Notification;
import com.tamo.model.Profile;

import com.tamo.model.User;
import com.tamo.util.JsonUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of OrganiserService.
 * Provides necessary operations and logic for the system.
 */
public class OrganiserService {

    private static final Set<String> VALID_APP_STATUSES = Set.of(
            "applied", "review", "shortlisted", "offer", "rejected"
    );
    private static final Set<String> VALID_ACTIONS = Set.of("shortlist", "reject", "offer");

    private final JsonDataService dataService;

    private boolean reviewAllowed(String id, String permission) {
        try {
            User user = ReviewAccess.user(dataService.getData(), id);
            return "organiser".equals(user.getRole()) && ReviewAccess.can(user, "review.read")
                    && (ReviewAccess.can(user, permission) || ("review.recommend".equals(permission) && ReviewAccess.can(user, "review.approve")));
        } catch (QualificationService.Problem denied) { return false; }
    }
    private void requireReview(String id) {
        if (!reviewAllowed(id, "review.read")) throw new QualificationService.Problem(403, "Current application review permission is required.");
    }
    private void addCapabilities(JsonObject row, String id, String appId) {
        User user = ReviewAccess.user(dataService.getData(), id);
        row.addProperty("canApprove", ReviewAccess.can(user, "review.approve"));
        row.addProperty("canRecommend", ReviewAccess.can(user, "review.recommend"));
        row.addProperty("canNote", ReviewAccess.can(user, "review.note"));
        row.addProperty("selectionApproved", ReviewService.approved(dataService.getData(), appId));
        dataService.getData().getApplications().stream().filter(a -> Objects.equals(appId,a.getId())).findFirst()
                .ifPresent(a -> TeacherSelectionService.addCapabilities(row,dataService.getData(),user,a));
    }

    /**
     * Constructs a new OrganiserService instance.
     */
    /**
     * Constructs a new OrganiserService instance.
     */
    public OrganiserService(JsonDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Executes the getDashboard operation.
     * @param organiserId the organiserId argument
     * @return the resulting object or status
     */
    /**
     * Executes the getDashboard operation.
     * @param organiserId the organiserId argument
     * @return the resulting object or status
     */
    public JsonObject getDashboard(String organiserId) {
        requireReview(organiserId);
        DataStore data = dataService.getData();
        List<Job> myJobs = data.getJobs().stream().filter(j -> ReviewAccess.organiserOwns(data, organiserId, j)).toList();
        Set<String> ids = myJobs.stream().map(Job::getId).collect(Collectors.toSet());
        List<Application> apps = data.getApplications().stream().filter(a -> ids.contains(a.getJobId())).toList();
        JsonObject result = new JsonObject();
        result.addProperty("activePostings", myJobs.stream().filter(j -> "active".equals(j.getStatus())).count());
        result.addProperty("currentApplicants", apps.stream().filter(a -> !"rejected".equals(a.getStatus())).count());
        result.addProperty("totalApplications", apps.size());
        result.addProperty("totalApplicants", apps.size());
        JsonArray recent = new JsonArray();
        apps.stream().sorted(Comparator.comparing(Application::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5).forEach(a -> {
                User user = findUserById(data, a.getApplicantId());
                Job job = findJobById(data, a.getJobId());
                JsonObject row = new JsonObject();
                row.addProperty("id", a.getId());
                row.addProperty("applicantName", user == null ? "Unknown" : user.getName());
                row.addProperty("jobTitle", job == null ? "" : job.getTitle());
                row.addProperty("moduleCode", job == null ? "" : job.getModuleCode());
                row.addProperty("status", RecruitmentStatus.current(a.getStatus()));
                row.addProperty("date", a.getUpdatedAt());
                row.addProperty("type", "application");
                recent.add(row);
            });
        result.add("recentApplications", recent);
        JsonArray jobs = new JsonArray();
        myJobs.stream().filter(j -> "active".equals(j.getStatus())).forEach(j -> {
            JsonObject row = JsonUtil.getGson().toJsonTree(j).getAsJsonObject();
            row.addProperty("applicants", apps.stream().filter(a -> j.getId().equals(a.getJobId()) && !"rejected".equals(a.getStatus())).count());
            row.addProperty("applicationCount", apps.stream().filter(a -> j.getId().equals(a.getJobId()) && !"rejected".equals(a.getStatus())).count());
            row.addProperty("totalApplicants", apps.stream().filter(a -> j.getId().equals(a.getJobId())).count());
            jobs.add(row);
        });
        result.add("jobs", jobs);
        result.add("activeJobs", jobs.deepCopy());
        return result;
    }

    /**
     * Executes the getNotifications operation.
     * @param organiserId the organiserId argument
     * @return the resulting object or status
     */
    /**
     * Executes the getNotifications operation.
     * @param organiserId the organiserId argument
     * @return the resulting object or status
     */
    public List<JsonObject> getNotifications(String organiserId) {
        DataStore data = dataService.getData();
        if(!postingAdmin(data,organiserId)) requireReview(organiserId);
        List<Notification> notifications = (data.getNotifications() != null
                ? data.getNotifications() : Collections.<Notification>emptyList())
                .stream()
                .filter(n -> organiserId.equals(n.getUserId()))
                .filter(n -> RecruitmentStatus.visibleNotification(n.getType()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(20)
                .collect(Collectors.toList());

        List<JsonObject> result = new ArrayList<>();
        notifications.forEach(n -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", n.getId());
            obj.addProperty("type", n.getType());
            obj.addProperty("title", n.getTitle());
            obj.addProperty("message", n.getMessage());
            obj.addProperty("relatedId", n.getRelatedId());
            obj.addProperty("createdAt", n.getCreatedAt());
            obj.addProperty("read", n.isRead());
            result.add(obj);
        });
        return result;
    }

    /**
     * Executes the markNotificationRead operation.
     * @param organiserId the organiserId argument
     * @param notifId the notifId argument
     */
    /**
     * Executes the markNotificationRead operation.
     * @param organiserId the organiserId argument
     * @param notifId the notifId argument
     */
    public void markNotificationRead(String organiserId, String notifId) {
        DataStore data = dataService.getData();
        if (data.getNotifications() == null) return;
        data.getNotifications().stream()
                .filter(n -> n.getId().equals(notifId) && organiserId.equals(n.getUserId()))
                .findFirst()
                .ifPresent(n -> {
                    n.setRead(true);
                    dataService.save();
                });
    }

    /**
     * Executes the markAllNotificationsRead operation.
     * @param organiserId the organiserId argument
     */
    /**
     * Executes the markAllNotificationsRead operation.
     * @param organiserId the organiserId argument
     */
    public void markAllNotificationsRead(String organiserId) {
        DataStore data = dataService.getData();
        if (data.getNotifications() == null) return;
        data.getNotifications().stream()
                .filter(n -> organiserId.equals(n.getUserId()) && !n.isRead())
                .forEach(n -> n.setRead(true));
        dataService.save();
    }

    private void createNotification(DataStore data, String userId, String type, String title, String message, String relatedId) {
        if (userId == null) return;
        if (data.getNotifications() == null) data.setNotifications(new ArrayList<>());
        Notification n = new Notification();
        n.setId(dataService.generateId("n", data.getNotifications()));
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setRelatedId(relatedId);
        n.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        n.setRead(false);
        data.getNotifications().add(n);
        dataService.save();
    }

    /**
     * Return the list of modules owned by the given organiser. Used by the
     * organiser job-posting UI so MOs may only pick modules they are
     * responsible for, rather than any module in the system.
     */
    /**
     * Executes the getMyModules operation.
     * @param organiserId the organiserId argument
     * @return the resulting object or status
     */
    /**
     * Executes the getMyModules operation.
     * @param organiserId the organiserId argument
     * @return the resulting object or status
     */
    private boolean postingAdmin(DataStore data,String userId) {
        return data.getUsers().stream().anyMatch(u -> Objects.equals(userId,u.getId())
                && "admin".equals(u.getRole()) && "active".equals(u.getStatus()));
    }

    public List<JsonObject> getMyModules(String organiserId) {
        DataStore data = dataService.getData();
        return data.getModules().stream()
                .filter(m -> postingAdmin(data,organiserId) || organiserId != null && organiserId.equals(m.getOrganiserId()))
                .map(m -> {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", m.getId());
                    obj.addProperty("code", m.getCode());
                    obj.addProperty("name", m.getName());
                    obj.addProperty("department", m.getDepartment());
                    obj.addProperty("term", m.getTerm());
                    obj.addProperty("status", m.getStatus());
                    return obj;
                })
                .collect(Collectors.toList());
    }

    /**
     * Check whether the given organiser is allowed to publish under the
     * referenced module. Either moduleId or moduleCode may be supplied; if
     * both are blank/null the check is skipped (caller will fail elsewhere).
     */
    private boolean isModuleOwnedBy(DataStore data, String organiserId, String moduleId, String moduleCode) {
        if (organiserId == null) return false;
        Module module = null;
        if (moduleId != null && !moduleId.isBlank()) {
            module = data.getModules().stream()
                    .filter(m -> moduleId.equals(m.getId()))
                    .findFirst().orElse(null);
        }
        if (module == null && moduleCode != null && !moduleCode.isBlank()) {
            module = data.getModules().stream()
                    .filter(m -> moduleCode.equalsIgnoreCase(m.getCode()))
                    .findFirst().orElse(null);
        }
        return module != null && (postingAdmin(data,organiserId) || organiserId.equals(module.getOrganiserId()));
    }

    /**
     * Executes the getJobs operation.
     * @param organiserId the organiserId argument
     * @return the resulting object or status
     */
    /**
     * Executes the getJobs operation.
     * @param organiserId the organiserId argument
     * @return the resulting object or status
     */
    public List<JsonObject> getJobs(String organiserId) {
        DataStore data = dataService.getData();
        return data.getJobs().stream()
                .filter(j -> (postingAdmin(data,organiserId) || ReviewAccess.organiserOwns(data, organiserId, j)))
                .map(j -> {
                    JsonObject obj = (JsonObject) JsonUtil.getGson().toJsonTree(j);
                    long totalApplications = data.getApplications().stream()
                            .filter(a -> a.getJobId().equals(j.getId())).count();
                    long currentApplications = data.getApplications().stream()
                            .filter(a -> a.getJobId().equals(j.getId()))
                            .filter(a -> a.getStatus() == null || !"rejected".equals(a.getStatus()))
                            .count();
                    long shortlisted = data.getApplications().stream()
                            .filter(a -> a.getJobId().equals(j.getId()) &&
                                    ("shortlisted".equals(a.getStatus()) || "interview".equals(a.getStatus()) || "offer".equals(a.getStatus())))
                            .count();
                    obj.addProperty("applicants", currentApplications);
                    obj.addProperty("applicationCount", currentApplications);
                    obj.addProperty("totalApplications", totalApplications);
                    obj.addProperty("shortlisted", shortlisted);
                    return obj;
                })
                .collect(Collectors.toList());
    }

    /**
     * Create job, return Map ("error"/"code" or "job")
     */
    /**
     * Executes the createJob operation.
     * @param organiserId the organiserId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the createJob operation.
     * @param organiserId the organiserId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> createJob(String organiserId, JsonObject body) {
        Map<String, Object> result = new HashMap<>();

        String title = body.has("title") ? body.get("title").getAsString() : null;
        String moduleId = body.has("moduleId") ? body.get("moduleId").getAsString() : null;
        String moduleCode = body.has("moduleCode") ? body.get("moduleCode").getAsString() : null;

        if ((moduleId == null || moduleId.isBlank()) && moduleCode != null && !moduleCode.isBlank()) {
            DataStore lookupData = dataService.getData();
            Module module = lookupData.getModules().stream()
                    .filter(m -> moduleCode.equals(m.getCode()))
                    .findFirst().orElse(null);
            moduleId = module != null ? module.getId() : moduleCode;
        }

        if (title == null || title.isBlank() || moduleId == null || moduleId.isBlank()) {
            result.put("error", "Missing required fields");
            result.put("code", 400);
            return result;
        }

        // Module Organisers may only publish jobs under modules they are responsible for.
        DataStore ownershipData = dataService.getData();
        if (!isModuleOwnedBy(ownershipData, organiserId, moduleId, moduleCode)) {
            result.put("error", "You can only post jobs for modules you are responsible for.");
            result.put("code", 403);
            return result;
        }

        if (body.has("deadline") && !body.get("deadline").getAsString().isBlank()) {
            try {
                LocalDate deadline = LocalDate.parse(body.get("deadline").getAsString());
                if (deadline.isBefore(LocalDate.now())) {
                    result.put("error", "Deadline cannot be in the past");
                    result.put("code", 400);
                    return result;
                }
            } catch (Exception ignored) {}
        }

        DataStore data = dataService.getData();
        Job job = new Job();
        job.setId(dataService.generateId("j", data.getJobs()));
        if(postingAdmin(data,organiserId)) {
            String courseId=moduleId;
            Module course=data.getModules().stream().filter(m->courseId.equals(m.getId())).findFirst().orElseThrow();
            if(course.getOrganiserId()==null || course.getOrganiserId().isBlank()) {
                result.put("error","Assign a Teacher to the course first.");result.put("code",400);return result;
            }
            job.setOrganiserId(course.getOrganiserId());
        } else job.setOrganiserId(organiserId);
        job.setModuleCode(body.has("moduleCode") ? body.get("moduleCode").getAsString() : "");
        job.setModuleId(moduleId);
        job.setTitle(title);
        job.setDescription(body.has("description") ? body.get("description").getAsString() : "");
        job.setProfessor(body.has("professor") ? body.get("professor").getAsString() : "");
        job.setDepartment(body.has("department") ? body.get("department").getAsString() : "");
        job.setType(body.has("type") ? body.get("type").getAsString() : "Part-time");
        job.setHoursPerWeek(body.has("hoursPerWeek") ? body.get("hoursPerWeek").getAsInt() : 10);
        job.setPositions(body.has("positions") ? body.get("positions").getAsInt() : 1);
        job.setDeadline(body.has("deadline") ? body.get("deadline").getAsString() : "");
        job.setStatus("active");
        job.setTags(new ArrayList<>());
        job.setRequirements(new ArrayList<>());
        job.setResponsibilities(new ArrayList<>());
        job.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (body.has("tags")) body.getAsJsonArray("tags").forEach(e -> job.getTags().add(e.getAsString()));
        if (body.has("requirements")) body.getAsJsonArray("requirements").forEach(e -> job.getRequirements().add(e.getAsString()));
        if (body.has("responsibilities")) body.getAsJsonArray("responsibilities").forEach(e -> job.getResponsibilities().add(e.getAsString()));
        if (body.has("workDates")) {
            List<String> dates = new ArrayList<>();
            body.getAsJsonArray("workDates").forEach(e -> dates.add(e.getAsString()));
            job.setWorkDates(dates);
        }

        if (body.has("sectionIds")) {
            final String courseId=moduleId;
            Module selectedCourse=data.getModules().stream().filter(m->Objects.equals(m.getId(),courseId) && (postingAdmin(data,organiserId) || Objects.equals(m.getOrganiserId(),organiserId))).findFirst().orElse(null);
            if(selectedCourse==null || moduleCode!=null && !moduleCode.isBlank() && !Objects.equals(selectedCourse.getCode(),moduleCode)) {
                result.put("error","Course and Section selections do not match."); result.put("code",400); return result;
            }
            job.setModuleCode(selectedCourse.getCode());
            try { job.setSectionIds(JobSectionService.validate(data, moduleId, body.get("sectionIds"))); }
            catch (IllegalArgumentException e) { result.put("error",e.getMessage()); result.put("code",400); return result; }
            job.setWorkDates(new ArrayList<>());
        }

        data.getJobs().add(job);
        if(postingAdmin(data,organiserId)) {
            Module course=ReviewAccess.module(data,job);
            if(course!=null) { job.setOrganiserId(course.getOrganiserId()); job.setProfessor(course.getOrganiserName()); }
        }
        dataService.save();
        result.put("job", job);
        return result;
    }

    /**
     * Edit job, return Map ("error"/"code" or "job")
     */
    /**
     * Executes the updateJob operation.
     * @param organiserId the organiserId argument
     * @param jobId the jobId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the updateJob operation.
     * @param organiserId the organiserId argument
     * @param jobId the jobId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> updateJob(String organiserId, String jobId, JsonObject body) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        Job job = data.getJobs().stream()
                .filter(j -> j.getId().equals(jobId))
                .findFirst().orElse(null);

        if (job == null) {
            result.put("error", "Job not found");
            result.put("code", 404);
            return result;
        }

        if (!postingAdmin(data,organiserId) && !ReviewAccess.organiserOwns(data, organiserId, job)) {
            result.put("error", "No permission to edit this job");
            result.put("code", 403);
            return result;
        }

        if (body.has("moduleId")) {
            String requestedCourseId=body.get("moduleId").getAsString();
            Module requestedCourse=data.getModules().stream().filter(m->Objects.equals(m.getId(),requestedCourseId) && (postingAdmin(data,organiserId) || Objects.equals(m.getOrganiserId(),organiserId))).findFirst().orElse(null);
            if(requestedCourse==null) { result.put("error","You can only post jobs for your own courses."); result.put("code",403); return result; }
            if(body.has("moduleCode") && !Objects.equals(requestedCourse.getCode(),body.get("moduleCode").getAsString())) {
                result.put("error","Course selections do not match."); result.put("code",400); return result;
            }
        }
        if ((body.has("moduleId") && !Objects.equals(job.getModuleId(),body.get("moduleId").getAsString()) ||
             body.has("moduleCode") && !Objects.equals(job.getModuleCode(),body.get("moduleCode").getAsString()))
                && data.getApplications().stream().anyMatch(a->jobId.equals(a.getJobId()))) {
            result.put("error","This posting has applications and cannot move to another course. Create a new posting."); result.put("code",409); return result;
        }
        if (body.has("moduleCode") && !Objects.equals(job.getModuleCode(),body.get("moduleCode").getAsString()) && SectionAssignmentService.hasHistory(data,"jobs",jobId)) {
            result.put("error", "A position with assignment history cannot be moved to another course."); result.put("code",409);return result;
        }
        if (job.getSectionIds()!=null && !body.has("sectionIds") &&
                (body.has("moduleId") && !Objects.equals(job.getModuleId(),body.get("moduleId").getAsString()) ||
                 body.has("moduleCode") && !Objects.equals(job.getModuleCode(),body.get("moduleCode").getAsString()))) {
            result.put("error","Select Sections again when changing the course."); result.put("code",400); return result;
        }
        List<String> selectedSections = job.getSectionIds();
        if (body.has("sectionIds")) {
            String targetModule = body.has("moduleId") ? body.get("moduleId").getAsString() : job.getModuleId();
            com.tamo.model.Module selectedCourse=data.getModules().stream().filter(m->Objects.equals(m.getId(),targetModule)).findFirst().orElse(null);
            if (selectedCourse==null || body.has("moduleCode") && !Objects.equals(selectedCourse.getCode(),body.get("moduleCode").getAsString())) {
                result.put("error","Course and Section selections do not match."); result.put("code",400); return result;
            }
            if (!isModuleOwnedBy(data, organiserId, targetModule, null)) {
                result.put("error","You can only post jobs for your own courses."); result.put("code",403); return result;
            }
            try { selectedSections=JobSectionService.validate(data,targetModule,body.get("sectionIds")); }
            catch (IllegalArgumentException e) { result.put("error",e.getMessage()); result.put("code",400); return result; }
            if (!Objects.equals(new java.util.HashSet<>(selectedSections),job.getSectionIds()==null?null:new java.util.HashSet<>(job.getSectionIds()))
                    && data.getApplications().stream().anyMatch(a->jobId.equals(a.getJobId()))) {
                result.put("error","This posting already has applications. Create a new posting to change its Section scope."); result.put("code",409); return result;
            }
        }
        if (body.has("title")) job.setTitle(body.get("title").getAsString());
        if (body.has("moduleCode")) {
            String newModuleCode = body.get("moduleCode").getAsString();
            if (newModuleCode != null && !newModuleCode.isBlank()
                    && !isModuleOwnedBy(data, organiserId, body.has("moduleId")?body.get("moduleId").getAsString():null, newModuleCode)) {
                result.put("error", "You can only post jobs for modules you are responsible for.");
                result.put("code", 403);
                return result;
            }
            job.setModuleCode(newModuleCode);
            if (newModuleCode != null && !newModuleCode.isBlank()) {
                Module module = data.getModules().stream()
                        .filter(m -> body.has("moduleId")?Objects.equals(body.get("moduleId").getAsString(),m.getId()):newModuleCode.equals(m.getCode()))
                        .findFirst().orElse(null);
                job.setModuleId(module != null ? module.getId() : newModuleCode);
            }
        }
        if (body.has("department")) job.setDepartment(body.get("department").getAsString());
        if (body.has("description")) job.setDescription(body.get("description").getAsString());
        if (body.has("type")) job.setType(body.get("type").getAsString());
        if (body.has("hoursPerWeek")) job.setHoursPerWeek(body.get("hoursPerWeek").getAsInt());
        if (body.has("positions")) job.setPositions(body.get("positions").getAsInt());
        if (body.has("deadline")) {
            String deadlineStr = body.get("deadline").getAsString();
            if (!deadlineStr.isBlank()) {
                try {
                    LocalDate deadline = LocalDate.parse(deadlineStr);
                    if (deadline.isBefore(LocalDate.now())) {
                        result.put("error", "Deadline cannot be in the past");
                        result.put("code", 400);
                        return result;
                    }
                } catch (Exception ignored) {
                }
            }
            job.setDeadline(deadlineStr);
        }
        if (body.has("status")) job.setStatus(body.get("status").getAsString());
        if (body.has("workDates")) {
            List<String> dates = new ArrayList<>();
            body.getAsJsonArray("workDates").forEach(e -> dates.add(e.getAsString()));
            job.setWorkDates(dates);
        }

        if (body.has("moduleId")) {
            job.setModuleId(body.get("moduleId").getAsString());
            data.getModules().stream().filter(m->Objects.equals(m.getId(),job.getModuleId())).findFirst().ifPresent(m->job.setModuleCode(m.getCode()));
        }
        if (body.has("sectionIds")) { job.setSectionIds(selectedSections); if(body.has("moduleId")) job.setModuleId(body.get("moduleId").getAsString()); job.setWorkDates(new ArrayList<>()); }
        if(postingAdmin(data,organiserId)) {
            Module course=ReviewAccess.module(data,job);
            if(course!=null) { job.setOrganiserId(course.getOrganiserId()); job.setProfessor(course.getOrganiserName()); }
        }
        dataService.save();
        result.put("job", job);
        return result;
    }

    /**
     * Delete job, return Map ("error"/"code" or "success"=true)
     */
    /**
     * Executes the deleteJob operation.
     * @param organiserId the organiserId argument
     * @param jobId the jobId argument
     * @return the resulting object or status
     */
    /**
     * Executes the deleteJob operation.
     * @param organiserId the organiserId argument
     * @param jobId the jobId argument
     * @return the resulting object or status
     */
    public Map<String, Object> deleteJob(String organiserId, String jobId) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        Job job = data.getJobs().stream()
                .filter(j -> j.getId().equals(jobId))
                .findFirst().orElse(null);

        if (job == null) {
            result.put("error", "Job not found");
            result.put("code", 404);
            return result;
        }

        if (!postingAdmin(data,organiserId) && !ReviewAccess.organiserOwns(data, organiserId, job)) {
            result.put("error", "No permission to delete this job");
            result.put("code", 403);
            return result;
       
        }

        if (SectionAssignmentService.hasHistory(data, "jobs", jobId)) {
            result.put("error", "This position has assignment history and must be retained."); result.put("code",409); return result;
        }
        // When deleting a job, also clean up related applications and interviews to avoid leftover pipeline in TA pages
        Set<String> deletedAppIds = data.getApplications().stream()
                .filter(a -> a.getJobId().equals(jobId))
                .map(Application::getId)
                .collect(Collectors.toSet());
        data.getApplications().removeIf(a -> deletedAppIds.contains(a.getId()));
        data.getInterviews().removeIf(i -> deletedAppIds.contains(i.getApplicationId()));

        data.getJobs().remove(job);
        dataService.save();
        result.put("success", true);
        return result;
    }

    /**
     * Executes the getPipeline operation.
     * @param organiserId the organiserId argument
     * @param moduleFilter the moduleFilter argument
     * @return the resulting object or status
     */
    /**
     * Executes the getPipeline operation.
     * @param organiserId the organiserId argument
     * @param moduleFilter the moduleFilter argument
     * @return the resulting object or status
     */
    public JsonObject getPipeline(String organiserId, String moduleFilter) { return getPipeline(organiserId, moduleFilter, null); }

    public JsonObject getPipeline(String organiserId, String moduleFilter, String periodId) {
        requireReview(organiserId);
        DataStore data = dataService.getData();
        Set<String> myJobIds = data.getJobs().stream()
                .filter(j -> ReviewAccess.organiserOwns(data, organiserId, j))
                .map(Job::getId)
                .collect(Collectors.toSet());

        List<Application> apps = data.getApplications().stream()
                .filter(a -> myJobIds.contains(a.getJobId()))
                .filter(a -> periodId == null || periodId.isBlank() || periodId.equals(a.getPeriodId()))
                .filter(a -> {
                    if (moduleFilter == null || "All".equals(moduleFilter) || moduleFilter.isEmpty()) return true;
                    Job job = findJobById(data, a.getJobId());
                    return job != null && job.getModuleCode().equals(moduleFilter);
                })
                .collect(Collectors.toList());

        JsonObject result = new JsonObject();
        for (String status : new String[]{"applied", "review", "shortlisted", "offer", "rejected"}) {
            JsonArray arr = new JsonArray();
            apps.stream().filter(a -> status.equals(RecruitmentStatus.current(a.getStatus()))).forEach(a -> {
                User applicant = findUserById(data, a.getApplicantId());
                Profile profile = findProfileByUserId(data, a.getApplicantId());
                Job job = findJobById(data, a.getJobId());
                JsonObject obj = new JsonObject();
                obj.addProperty("id", a.getId());
                addCapabilities(obj, organiserId, a.getId());
                obj.addProperty("name", applicant != null ? applicant.getName() : "Unknown");
                obj.addProperty("role", job != null ? job.getModuleCode() : "");
                obj.addProperty("status", RecruitmentStatus.current(a.getStatus()));
                obj.addProperty("avatar", applicant != null ? String.valueOf(applicant.getName().charAt(0)) : "?");
                obj.addProperty("gpa", profile != null ? String.valueOf(profile.getGpa()) : "N/A");
                obj.addProperty("date", a.getAppliedAt());
                JsonObject qualificationMatch=new CriteriaMatchingService().evaluateAcrossSections(data,a,job);
                obj.add("qualificationMatching",qualificationMatch);obj.add("submittedQualifications",a.getQualificationSnapshot()==null?new JsonObject():a.getQualificationSnapshot().deepCopy());
                obj.add("matchScore",qualificationMatch.get("score"));
                obj.addProperty("riskFlag", a.getRiskFlag());
                obj.add("sectionMatching", new SectionMatchingService().match(data, a, job, organiserId));
                obj.addProperty("aiSummary", a.getAiSummary());
                arr.add(obj);
            });
            result.add(status, arr);
        }
        return result;
    }

    /**
     * Update candidate status, return Map
     */
    /**
     * Executes the updateCandidateStatus operation.
     * @param organiserId the organiserId argument
     * @param appId the appId argument
     * @param newStatus the newStatus argument
     * @return the resulting object or status
     */
    /**
     * Executes the updateCandidateStatus operation.
     * @param organiserId the organiserId argument
     * @param appId the appId argument
     * @param newStatus the newStatus argument
     * @return the resulting object or status
     */
    public Map<String, Object> updateCandidateStatus(String organiserId, String appId, String newStatus) {
        return updateCandidateStatus(organiserId, appId, newStatus, null);
    }
    public Map<String, Object> updateCandidateStatus(String organiserId, String appId, String newStatus, String reason) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        if (newStatus == null || newStatus.isBlank()) {
            result.put("error", "Missing status parameter");
            result.put("code", 400);
            return result;
        }

        if (!VALID_APP_STATUSES.contains(newStatus)) {
            result.put("error", "Invalid status value");
            result.put("code", 400);
            return result;
        }

        Application app = data.getApplications().stream()
                .filter(a -> a.getId().equals(appId))
                .findFirst().orElse(null);

        if (app == null) {
            result.put("error", "Application not found");
            result.put("code", 404);
            return result;
        }

        Job job = findJobById(data, app.getJobId());
        if (job == null || !ReviewAccess.organiserOwns(data, organiserId, job)) {
            result.put("error", "No permission to update this candidate");
            result.put("code", 403);
            return result;
        }

        String permission = "rejected".equals(newStatus) ? "review.approve" : "review.recommend";
        if (!reviewAllowed(organiserId, permission)) {
            result.put("error", "This action requires current " + permission + " permission."); result.put("code", 403); return result;
        }
        boolean decided = data.getReviewRecords().stream().anyMatch(r -> appId.equals(r.getApplicationId()) && Set.of("approve", "not_selected").contains(r.getAction()));
        if (decided && !Set.of("offer", "rejected").contains(newStatus)) {
            result.put("error", "Use Review Workspace to reconsider a recorded selection decision."); result.put("code", 409); return result;
        }
        if ("rejected".equals(newStatus) && (reason == null || reason.trim().length() < 10 || reason.trim().length() > 4000)) {
            result.put("error", "Record a decision reason of 10–4000 characters."); result.put("code", 400); return result;
        }
        if (!"offer".equals(newStatus) && SectionAssignmentService.activeForApplication(data, appId)) {
            result.put("error", "Cancel active TA assignments before changing this application status.");
            result.put("code", 409); return result;
        }
        if("offer".equals(newStatus)&&!ReviewService.approved(data,appId)){result.put("error","Record selection approval in Review Workspace before moving to Offer.");result.put("code",409);return result;}
        String oldStatus = app.getStatus();
        if ("rejected".equals(newStatus)) {
            JsonObject decision = new JsonObject(); decision.addProperty("action", "not_selected"); decision.addProperty("note", reason);
            try { new ReviewService(dataService).record(organiserId, appId, decision); }
            catch (QualificationService.Problem denied) { result.put("error", denied.getMessage()); result.put("code", denied.status); return result; }
        } else app.setStatus(newStatus);
        app.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (app.getTimeline() != null) {
            String today = LocalDate.now().toString();
            for (Application.TimelineEntry te : app.getTimeline()) {
                if (te.getStatus().equals(newStatus)) {
                    te.setCompleted(true);
                    te.setDate(today);
                    te.setPending(null);
                    break;
                }
            }
        }

        // Create notification for the applicant
        if (!Objects.equals(oldStatus, newStatus)) {
            String applicantId = app.getApplicantId();
            String moduleCode = job != null ? job.getModuleCode() : "";
            String notifType = "status_change";
            String title = "";
            String message = "";

            switch (newStatus) {
                case "review" -> {
                    title = "Application Under Review";
                    message = "Your application for " + moduleCode + " is now being reviewed.";
                }
                case "shortlisted" -> {
                    title = "You've Been Shortlisted!";
                    message = "Congratulations! Your application for " + moduleCode + " has been shortlisted.";
                }

                case "offer" -> {
                    notifType = "offer";
                    title = "Application at Offer Stage";
                    message = "Your application for " + moduleCode + " is at the offer stage. Review My Assignments once your section assignment is published.";
                }
                case "rejected" -> {
                    notifType = "rejection";
                    title = "Application Update";
                    message = "We regret to inform you that your application for " + moduleCode + " was not successful.";
                }
                default -> {
                    title = "Application Status Updated";
                    message = "Your application for " + moduleCode + " status changed to: " + newStatus;
                }
            }
            createNotification(data, applicantId, notifType, title, message, app.getId());

        }
        dataService.save();
        result.put("application", app);
        return result;
    }

    /**
     * Get application details, including ownership verification
     */
    /**
     * Executes the getApplicationDetails operation.
     * @param organiserId the organiserId argument
     * @param appId the appId argument
     * @return the resulting object or status
     */
    /**
     * Executes the getApplicationDetails operation.
     * @param organiserId the organiserId argument
     * @param appId the appId argument
     * @return the resulting object or status
     */
    public Map<String, Object> getApplicationDetails(String organiserId, String appId) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        Application app = data.getApplications().stream()
                .filter(a -> a.getId().equals(appId))
                .findFirst().orElse(null);
        if (app == null) {
            result.put("error", "Application not found");
            result.put("code", 404);
            return result;
        }

        Job job = findJobById(data, app.getJobId());
        if (job == null || !ReviewAccess.organiserOwns(data, organiserId, job) || !reviewAllowed(organiserId, "review.read")) {
            result.put("error", "No permission to view this application");
            result.put("code", 403);
            return result;
        }

        User applicant = findUserById(data, app.getApplicantId());
        Profile profile = findProfileByUserId(data, app.getApplicantId());

        JsonObject detail = RecruitmentStatus.project((JsonObject) JsonUtil.getGson().toJsonTree(app));
        addCapabilities(detail, organiserId, appId);
        detail.add("qualificationMatching",new CriteriaMatchingService().evaluateAcrossSections(data,app,job));
        detail.add("sectionMatching", new SectionMatchingService().match(data, app, job, organiserId));
        if (applicant != null) {
            JsonObject applicantProfile = new JsonObject();
            applicantProfile.addProperty("name", applicant.getName());
            applicantProfile.addProperty("email", applicant.getEmail());
            applicantProfile.addProperty("phone", applicant.getPhone() != null ? applicant.getPhone() : "");
            applicantProfile.addProperty("department", applicant.getDepartment() != null ? applicant.getDepartment() : "");
            if (profile != null) {
                applicantProfile.addProperty("studentId", profile.getStudentId() != null ? profile.getStudentId() : "");
                applicantProfile.addProperty("degree", profile.getDegree());
                applicantProfile.addProperty("year", profile.getYear());
                applicantProfile.addProperty("gpa", profile.getGpa());
                applicantProfile.addProperty("maxGpa", profile.getMaxGpa());
                applicantProfile.add("skills", JsonUtil.getGson().toJsonTree(profile.getSkills()));
                applicantProfile.add("coursework", JsonUtil.getGson().toJsonTree(profile.getCoursework()));
                applicantProfile.add("experience", JsonUtil.getGson().toJsonTree(profile.getExperience()));
            }
            detail.add("applicantProfile", applicantProfile);
        }
        if (profile != null) {
            detail.add("documents", JsonUtil.getGson().toJsonTree(profile.getDocuments().stream().filter(d->app.getDocumentIds()!=null&&app.getDocumentIds().contains(d.getId())).toList()));
        }

        result.put("data", detail);
        return result;
    }

    /**
     * Perform action on application (shortlist/reject/offer), return Map
     */
    /**
     * Executes the applicationAction operation.
     * @param organiserId the organiserId argument
     * @param appId the appId argument
     * @param action the action argument
     * @return the resulting object or status
     */
    /**
     * Executes the applicationAction operation.
     * @param organiserId the organiserId argument
     * @param appId the appId argument
     * @param action the action argument
     * @return the resulting object or status
     */
    public Map<String, Object> applicationAction(String organiserId, String appId, String action) {
        return applicationAction(organiserId, appId, action, null);
    }
    public Map<String, Object> applicationAction(String organiserId, String appId, String action, String reason) {
        Map<String, Object> result = new HashMap<>();

        if (action == null || action.isBlank()) {
            result.put("error", "Missing required fields");
            result.put("code", 400);
            return result;
        }

        if (!VALID_ACTIONS.contains(action)) {
            result.put("error", "Invalid action type; only shortlist/reject/offer are supported");
            result.put("code", 400);
            return result;
        }

        String mappedStatus = switch (action) {
            case "shortlist" -> "shortlisted";
            case "reject" -> "rejected";
            case "offer" -> "offer";
            default -> action;
        };

        return updateCandidateStatus(organiserId, appId, mappedStatus, reason);
    }

    private User findUserById(DataStore data, String userId) {
        return data.getUsers().stream().filter(u -> u.getId().equals(userId)).findFirst().orElse(null);
    }

    private Profile findProfileByUserId(DataStore data, String userId) {
        return data.getProfiles().stream().filter(p -> p.getUserId().equals(userId)).findFirst().orElse(null);
    }

    private Job findJobById(DataStore data, String jobId) {
        return data.getJobs().stream().filter(j -> j.getId().equals(jobId)).findFirst().orElse(null);
    }


}
