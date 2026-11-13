package com.tamo.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tamo.model.Application;
import com.tamo.model.DataStore;
import com.tamo.model.Department;
import com.tamo.model.Job;
import com.tamo.model.LogEntry;
import com.tamo.model.Message;
import com.tamo.model.Module;
import com.tamo.model.Notification;
import com.tamo.model.Profile;
import com.tamo.model.ReviewRecord;
import com.tamo.model.Term;
import com.tamo.model.User;
import com.tamo.model.WorkloadAssignment;
import com.tamo.util.JsonUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of AdminService.
 * Provides necessary operations and logic for the system.
 */
public class AdminService {

    private static final List<String> VALID_ROLES = Arrays.asList("applicant", "organiser", "admin","reviewer","decision_maker","viewer");

    private final JsonDataService dataService;

    /**
     * Constructs a new AdminService instance.
     */
    /**
     * Constructs a new AdminService instance.
     */
    public AdminService(JsonDataService dataService) {
        this.dataService = dataService;
    }

    // ==================== Dashboard ====================

    /**
     * Executes the getDashboard operation.
     * @return the resulting object or status
     */
    /**
     * Executes the getDashboard operation.
     * @return the resulting object or status
     */
    public JsonObject getDashboard() {
        DataStore data = dataService.getData();

        long totalApplicants = data.getUsers().stream().filter(u -> "applicant".equals(u.getRole())).count();
        long activePostings = data.getJobs().stream().filter(j -> "active".equals(j.getStatus())).count();
        long totalOffers = data.getApplications().stream().filter(a -> "offer".equals(a.getStatus())).count();
        long totalApps = data.getApplications().size();
        String acceptanceRate = totalApps > 0 ? Math.round((double) totalOffers / totalApps * 100) + "%" : "0%";

        JsonObject result = new JsonObject();
        result.addProperty("totalApplicants", totalApplicants);
        result.addProperty("activePostings", activePostings);
        result.addProperty("acceptanceRate", acceptanceRate);

        JsonArray trendData = buildWeeklyApplicationTrend(data);
        result.add("trendData", trendData);

        JsonArray moduleStats = new JsonArray();
        for (Job j : data.getJobs()) {
            long appCount = data.getApplications().stream().filter(a -> a.getJobId().equals(j.getId())).count();
            JsonObject obj = new JsonObject();
            obj.addProperty("name", j.getModuleCode());
            obj.addProperty("positions", j.getPositions());
            obj.addProperty("applicants", appCount);
            moduleStats.add(obj);
        }
        result.add("moduleStats", moduleStats);

        JsonArray alerts = new JsonArray();
        for (Job j : data.getJobs()) {
            if ("active".equals(j.getStatus())) {
                long filled = data.getApplications().stream()
                        .filter(a -> a.getJobId().equals(j.getId()) && "offer".equals(a.getStatus()))
                        .count();
                if (filled < j.getPositions()) {
                    JsonObject alert = new JsonObject();
                    alert.addProperty("module", j.getModuleCode());
                    alert.addProperty("unfilled", j.getPositions() - filled);
                    alerts.add(alert);
                }
            }
        }
        result.add("systemAlerts", alerts);

        result.add("pendingApplications", pendingApplications(data));
        return result;
    }

    private JsonArray pendingApplications(DataStore data) {
        Set<String> decided = data.getReviewRecords().stream()
                .filter(record -> "approve".equals(record.getAction()) || "not_selected".equals(record.getAction()))
                .map(ReviewRecord::getApplicationId).collect(Collectors.toSet());
        Map<String, Job> jobs = new HashMap<>();
        data.getJobs().forEach(job -> jobs.put(job.getId(), job));
        Map<String, String> names = new HashMap<>();
        data.getUsers().forEach(user -> names.put(user.getId(), user.getName()));
        Map<String, String> periods = new HashMap<>();
        data.getApplicationPeriods().forEach(period -> periods.put(period.getId(), period.getName()));
        Set<String> awaitingReview = Set.of("applied", "review", "shortlisted");
        JsonArray pending = new JsonArray();
        data.getApplications().stream()
                .filter(app -> app.getStatus() != null && awaitingReview.contains(RecruitmentStatus.current(app.getStatus()))
                        && !decided.contains(app.getId()) && jobs.containsKey(app.getJobId()))
                .sorted(Comparator.comparing(Application::getAppliedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(app -> {
                    Job job = jobs.get(app.getJobId());
                    JsonObject row = new JsonObject();
                    row.addProperty("id", app.getId());
                    row.addProperty("name", names.getOrDefault(app.getApplicantId(), "Historical applicant"));
                    row.addProperty("course", job.getModuleCode());
                    row.addProperty("position", job.getTitle());
                    row.addProperty("period", periods.getOrDefault(app.getPeriodId(), "Historical period"));
                    row.addProperty("appliedAt", app.getAppliedAt());
                    pending.add(row);
                });
        return pending;
    }

    // ==================== Users ====================

    /**
     * Get user list (without password)
     */
    /**
     * Executes the getUsers operation.
     * @param keyword the keyword argument
     * @param role the role argument
     * @param status the status argument
     * @return the resulting object or status
     */
    /**
     * Executes the getUsers operation.
     * @param keyword the keyword argument
     * @param role the role argument
     * @param status the status argument
     * @return the resulting object or status
     */
    public List<JsonObject> getUsers(String keyword, String role, String status) {
        DataStore data = dataService.getData();
        return data.getUsers().stream()
                .filter(u -> keyword == null || keyword.isEmpty() ||
                        u.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                        u.getEmail().toLowerCase().contains(keyword.toLowerCase()))
                .filter(u -> role == null || role.isEmpty() || u.getRole().equalsIgnoreCase(role))
                .filter(u -> status == null || status.isEmpty() || u.getStatus().equalsIgnoreCase(status))
                .map(this::buildSafeUser)
                .collect(Collectors.toList());
    }

    /**
     * Executes the createUser operation.
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the createUser operation.
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> createUser(JsonObject body) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();
        String accountError = validateAccountFields(body);
        if (accountError != null) { result.put("error", accountError); result.put("code", 400); return result; }


        String name = body.has("name") ? body.get("name").getAsString() : null;
        String email = body.has("email") ? body.get("email").getAsString() : null;
        String role = body.has("role") ? body.get("role").getAsString() : null;

        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            result.put("error", "Missing required fields");
            result.put("code", 400);
            return result;
        }

        if (role != null && !VALID_ROLES.contains(role)) {
            result.put("error", "Invalid role value");
            result.put("code", 400);
            return result;
        }

        boolean exists = data.getUsers().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        if (exists) {
            result.put("error", "Email already registered");
            result.put("code", 409);
            return result;
        }

        if(body.has("password")&&body.get("password").getAsString().isBlank()){result.put("error","Password must not be empty.");result.put("code",400);return result;}
        User user = new User();
        user.setId(dataService.generateId("u", data.getUsers()));
        user.setName(name);
        user.setEmail(email);
        user.setPassword(PasswordSecurity.hash(body.has("password") ? body.get("password").getAsString() : java.util.UUID.randomUUID().toString()));
        user.setRole(role != null ? role : "applicant");
        user.setDepartment(body.has("department") ? body.get("department").getAsString() : "");
        user.setStatus(body.has("status") ? body.get("status").getAsString() : "active");
        user.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        data.getUsers().add(user);

        if ("applicant".equals(user.getRole())) {
            Profile profile = new Profile();
            profile.setId(dataService.generateId("p", data.getProfiles()));
            profile.setUserId(user.getId());
            profile.setStudentId("");
            profile.setDegree("");
            profile.setYear(0);
            profile.setGpa(0);
            profile.setMaxGpa(4.0);
            profile.setProfileCompletion(0);
            profile.setSkills(new ArrayList<>());
            profile.setCoursework(new ArrayList<>());
            profile.setExperience(new ArrayList<>());
            profile.setDocuments(new ArrayList<>());
            profile.setMaxWeeklyHours(20);
            profile.setCurrentWeeklyHours(0);
            data.getProfiles().add(profile);
        }

        dataService.save();

        result.put("user", buildSafeUser(user));
        return result;
    }

    /**
     * Executes the updateUser operation.
     * @param adminId the adminId argument
     * @param userId the userId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the updateUser operation.
     * @param adminId the adminId argument
     * @param userId the userId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> updateUser(String adminId, String userId, JsonObject body) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();
        String accountError = validateAccountFields(body);
        if (accountError != null) { result.put("error", accountError); result.put("code", 400); return result; }


        User user = data.getUsers().stream().filter(u -> u.getId().equals(userId)).findFirst().orElse(null);
        if (user == null) {
            result.put("error", "User not found");
            result.put("code", 404);
            return result;
        }

        if (userId.equals(adminId) && body.has("role")) {
            result.put("error", "Cannot modify your own role");
            result.put("code", 400);
            return result;
        }

        if (userId.equals(adminId) && body.has("status") && !"active".equals(body.get("status").getAsString())) {
            result.put("error", "Cannot disable your own account"); result.put("code", 400); return result;
        }

        if(body.has("role")&&!ReviewAccess.ROLES.contains(body.get("role").getAsString())){result.put("error","Unknown user role.");result.put("code",400);return result;}
        if(body.has("email")){String email=body.get("email").getAsString().trim();if(email.isBlank()||data.getUsers().stream().anyMatch(x->!x.getId().equals(userId)&&email.equalsIgnoreCase(x.getEmail()))){result.put("error","Email must be non-empty and unique.");result.put("code",400);return result;}}
        if(body.has("password")&&body.get("password").getAsString().isBlank()){result.put("error","Password must not be empty.");result.put("code",400);return result;}
        if(body.has("email"))user.setEmail(body.get("email").getAsString().trim());
        if(body.has("password"))user.setPassword(PasswordSecurity.hash(body.get("password").getAsString()));
        if (body.has("name")) user.setName(body.get("name").getAsString());
        if (body.has("role") && !java.util.Objects.equals(user.getRole(),body.get("role").getAsString())) { user.setRole(body.get("role").getAsString()); user.setPermissions(null); }
        if (body.has("department")) user.setDepartment(body.get("department").getAsString());
        if (body.has("status")) user.setStatus(body.get("status").getAsString());

        dataService.save();
        result.put("user", buildSafeUser(user));
        return result;
    }

    /**
     * Executes the deleteUser operation.
     * @param adminId the adminId argument
     * @param userId the userId argument
     * @return the resulting object or status
     */
    /**
     * Executes the deleteUser operation.
     * @param adminId the adminId argument
     * @param userId the userId argument
     * @return the resulting object or status
     */
    public Map<String, Object> deleteUser(String adminId, String userId) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();
        if (SectionAssignmentService.hasHistory(data, "users", userId)) { result.put("error", "Assignment history must be retained."); result.put("code",409); return result; }


        if (userId.equals(adminId)) {
            result.put("error", "Cannot delete your own account");
            result.put("code", 400);
            return result;
        }

        User user = data.getUsers().stream().filter(u -> u.getId().equals(userId)).findFirst().orElse(null);
        if (user == null) {
            result.put("error", "User not found");
            result.put("code", 404);
            return result;
        }

        data.getUsers().remove(user);
        dataService.save();

        result.put("success", true);
        return result;
    }

    private String validateAccountFields(JsonObject body) {
        for (String field : List.of("role", "status")) {
            if (body.has(field) && (!body.get(field).isJsonPrimitive() || !body.get(field).getAsJsonPrimitive().isString()))
                return "Invalid " + field + " value";
        }
        if (body.has("role") && !VALID_ROLES.contains(body.get("role").getAsString())) return "Invalid role value";
        if (body.has("status") && !List.of("active", "suspended").contains(body.get("status").getAsString())) return "Invalid status value";
        return null;
    }

    // ==================== Modules ====================

    /**
     * Executes the getModules operation.
     * @param keyword the keyword argument
     * @return the resulting object or status
     */
    /**
     * Executes the getModules operation.
     * @param keyword the keyword argument
     * @return the resulting object or status
     */
    public List<JsonObject> getModules(String keyword) {
        DataStore data = dataService.getData();
        return data.getModules().stream()
                .filter(m -> keyword == null || keyword.isEmpty() ||
                        m.getCode().toLowerCase().contains(keyword.toLowerCase()) ||
                        m.getName().toLowerCase().contains(keyword.toLowerCase()))
                .map(m -> {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", m.getId());
                    obj.addProperty("code", m.getCode());
                    obj.addProperty("name", m.getName());
                    obj.addProperty("department", m.getDepartment());
                    obj.addProperty("organiserId", m.getOrganiserId());
                    obj.addProperty("term", m.getTerm());
                    obj.addProperty("status", m.getStatus());
                    // Enrich organiserName from users if organiserId exists
                    if (m.getOrganiserId() != null && !m.getOrganiserId().isEmpty()) {
                        User organiser = data.getUsers().stream()
                                .filter(u -> u.getId().equals(m.getOrganiserId()))
                                .findFirst().orElse(null);
                        obj.addProperty("organiserName", organiser != null ? organiser.getName() : m.getOrganiserId());
                    } else {
                        obj.addProperty("organiserName", "-");
                    }

                    // Attach jobs published under this module (matched by moduleId or moduleCode)
                    JsonArray jobsArr = new JsonArray();
                    int activeJobs = 0;
                    for (Job j : data.getJobs()) {
                        boolean belongs = (j.getModuleId() != null && j.getModuleId().equals(m.getId()))
                                || (j.getModuleCode() != null && j.getModuleCode().equalsIgnoreCase(m.getCode()));
                        if (!belongs) continue;
                        long appCount = data.getApplications().stream()
                                .filter(a -> j.getId().equals(a.getJobId())).count();
                        JsonObject jObj = new JsonObject();
                        jObj.addProperty("id", j.getId());
                        jObj.addProperty("title", j.getTitle());
                        jObj.addProperty("moduleCode", j.getModuleCode());
                        jObj.addProperty("status", j.getStatus());
                        jObj.addProperty("type", j.getType());
                        jObj.addProperty("hoursPerWeek", j.getHoursPerWeek());
                        jObj.addProperty("positions", j.getPositions());
                        jObj.addProperty("deadline", j.getDeadline());
                        jObj.addProperty("organiserId", j.getOrganiserId());
                        jObj.addProperty("applicationCount", appCount);
                        jObj.addProperty("createdAt", j.getCreatedAt());
                        jobsArr.add(jObj);
                        if ("active".equalsIgnoreCase(j.getStatus())) activeJobs++;
                    }
                    obj.add("jobs", jobsArr);
                    obj.addProperty("jobCount", jobsArr.size());
                    obj.addProperty("activeJobCount", activeJobs);
                    return obj;
                })
                .collect(Collectors.toList());
    }

    /**
     * Executes the createModule operation.
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the createModule operation.
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> createModule(JsonObject body) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        String code = body.has("code") ? body.get("code").getAsString() : null;
        String name = body.has("name") ? body.get("name").getAsString() : null;

        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            result.put("error", "Missing required fields");
            result.put("code_http", 400);
            return result;
        }

        boolean codeExists = data.getModules().stream().anyMatch(m -> m.getCode().equalsIgnoreCase(code));
        if (codeExists) {
            result.put("error", "Module code already exists");
            result.put("code_http", 409);
            return result;
        }

        String department = body.has("department") ? body.get("department").getAsString() : "";
        String term = body.has("term") ? body.get("term").getAsString() : "";

        String validationError = validateModuleProperties(data, department, term);
        if (validationError != null) {
            result.put("error", validationError);
            result.put("code_http", 400);
            return result;
        }

        Module module = new Module();
        module.setId(dataService.generateId("m", data.getModules()));
        module.setCode(code);
        module.setName(name);
        module.setDepartment(department);
        module.setOrganiserId(body.has("organiserId") ? body.get("organiserId").getAsString() : "");
        module.setOrganiserName(body.has("organiserName") ? body.get("organiserName").getAsString() : "");
        module.setTerm(term);
        module.setStatus("active");

        data.getModules().add(module);
        dataService.save();

        result.put("module", module);
        return result;
    }

    /**
     * Executes the updateModule operation.
     * @param moduleId the moduleId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the updateModule operation.
     * @param moduleId the moduleId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> updateModule(String moduleId, JsonObject body) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        Module module = data.getModules().stream().filter(m -> m.getId().equals(moduleId)).findFirst().orElse(null);
        if (module == null) {
            result.put("error", "Module not found");
            result.put("code_http", 404);
            return result;
        }

        String department = body.has("department") ? body.get("department").getAsString() : null;
        String term = body.has("term") ? body.get("term").getAsString() : null;

        String validationError = validateModuleProperties(data, department, term);
        if (validationError != null) {
            result.put("error", validationError);
            result.put("code_http", 400);
            return result;
        }

        User owner = null;
        if (body.has("organiserId")) {
            if (!body.get("organiserId").isJsonPrimitive() || !body.get("organiserId").getAsJsonPrimitive().isString()) {
                result.put("error", "Choose an active Teacher as course owner."); result.put("code_http", 400); return result;
            }
            String ownerId = body.get("organiserId").getAsString();
            owner = data.getUsers().stream().filter(u -> ownerId.equals(u.getId()) && "organiser".equals(u.getRole()) && "active".equals(u.getStatus())).findFirst().orElse(null);
            if (owner == null) { result.put("error", "Choose an active Teacher as course owner."); result.put("code_http", 400); return result; }
        }
        String previousCode = module.getCode();
        boolean transferring = owner != null && !Objects.equals(module.getOrganiserId(), owner.getId());

        if (body.has("code")) module.setCode(body.get("code").getAsString());
        if (body.has("name")) module.setName(body.get("name").getAsString());
        if (department != null) module.setDepartment(department);
        if (owner != null) {
            module.setOrganiserId(owner.getId());
            module.setOrganiserName(owner.getName());
            if (transferring) {
                // Change current authority only; review actors and submitted snapshots remain historical.
                for (Job job : data.getJobs()) {
                    boolean linked = moduleId.equals(job.getModuleId()) || ((job.getModuleId() == null || job.getModuleId().isBlank()) && previousCode != null && previousCode.equalsIgnoreCase(job.getModuleCode()));
                    if (linked) { job.setOrganiserId(owner.getId()); job.setProfessor(owner.getName()); }
                }
            }
        }
        if (term != null) module.setTerm(term);
        if (body.has("status")) module.setStatus(body.get("status").getAsString());

        dataService.save();
        result.put("module", module);
        return result;
    }

    /**
     * Ensure the module's department/term references match an existing entry
     * managed via the Properties page. Blank values are allowed (optional fields).
     */
    private String validateModuleProperties(DataStore data, String department, String term) {
        if (department != null && !department.isBlank()) {
            boolean deptExists = data.getDepartments().stream()
                    .anyMatch(d -> d.getName() != null && d.getName().equalsIgnoreCase(department));
            if (!deptExists) {
                return "Department \"" + department + "\" does not exist. Please pick one from Properties.";
            }
        }
        if (term != null && !term.isBlank()) {
            boolean termExists = data.getTerms().stream()
                    .anyMatch(t -> t.getName() != null && t.getName().equalsIgnoreCase(term));
            if (!termExists) {
                return "Term \"" + term + "\" does not exist. Please pick one from Properties.";
            }
        }
        return null;
    }

    /**
     * Executes the deleteModule operation.
     * @param moduleId the moduleId argument
     * @return the resulting object or status
     */
    /**
     * Executes the deleteModule operation.
     * @param moduleId the moduleId argument
     * @return the resulting object or status
     */
    public Map<String, Object> deleteModule(String moduleId) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        Module module = data.getModules().stream().filter(m -> m.getId().equals(moduleId)).findFirst().orElse(null);
        if (module == null) {
            result.put("error", "Module not found");
            result.put("code", 404);
            return result;
        }

        if (data.getCourseSections().stream().anyMatch(s -> moduleId.equals(s.getModuleId()))) {
            result.put("error", "Cannot delete a course with associated sections, including archived sections");
            result.put("code", 409);
            return result;
        }
        boolean hasJobs = data.getJobs().stream().anyMatch(j -> moduleId.equals(j.getModuleId()));
        if (hasJobs) {
            result.put("error", "Cannot delete module with associated jobs");
            result.put("code", 400);
            return result;
        }

        data.getModules().remove(module);
        dataService.save();
        result.put("success", true);
        return result;
    }

    // ==================== Properties: Terms ====================

    /**
     * Executes the getTerms operation.
     * @return the resulting object or status
     */
    /**
     * Executes the getTerms operation.
     * @return the resulting object or status
     */
    public List<Term> getTerms() {
        return dataService.getData().getTerms();
    }

    /**
     * Executes the createTerm operation.
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the createTerm operation.
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> createTerm(JsonObject body) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        String name = body.has("name") ? body.get("name").getAsString() : null;

        if (name == null || name.isBlank()) {
            result.put("error", "Missing required fields");
            result.put("code", 400);
            return result;
        }

        boolean nameExists = data.getTerms().stream().anyMatch(t -> t.getName().equalsIgnoreCase(name));
        if (nameExists) {
            result.put("error", "Term name already exists");
            result.put("code", 409);
            return result;
        }

        String startDate = body.has("startDate") ? body.get("startDate").getAsString() : "";
        String endDate = body.has("endDate") ? body.get("endDate").getAsString() : "";

        if (!startDate.isBlank() && !endDate.isBlank()) {
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                if (start.isAfter(end)) {
                    result.put("error", "Start date cannot be after end date");
                    result.put("code", 400);
                    return result;
                }
            } catch (Exception ignored) {}
        }

        Term term = new Term();
        term.setId(dataService.generateId("t", data.getTerms()));
        term.setName(name);
        term.setStartDate(startDate);
        term.setEndDate(endDate);
        term.setStatus("active");

        data.getTerms().add(term);
        dataService.save();

        result.put("term", term);
        return result;
    }

    // ==================== Properties: Departments ====================

    /**
     * Executes the getDepartments operation.
     * @return the resulting object or status
     */
    /**
     * Executes the getDepartments operation.
     * @return the resulting object or status
     */
    public List<Department> getDepartments() {
        return dataService.getData().getDepartments();
    }

    /**
     * Executes the createDepartment operation.
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the createDepartment operation.
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> createDepartment(JsonObject body) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        String name = body.has("name") ? body.get("name").getAsString() : null;

        if (name == null || name.isBlank()) {
            result.put("error", "Missing required fields");
            result.put("code", 400);
            return result;
        }

        boolean nameExists = data.getDepartments().stream().anyMatch(d -> d.getName().equalsIgnoreCase(name));
        if (nameExists) {
            result.put("error", "Department name already exists");
            result.put("code", 409);
            return result;
        }

        Department dept = new Department();
        dept.setId(dataService.generateId("d", data.getDepartments()));
        dept.setName(name);
        dept.setModuleCount(0);
        dept.setStatus("active");

        data.getDepartments().add(dept);
        dataService.save();

        result.put("department", dept);
        return result;
    }

    // ==================== Logs ====================

    /**
     * Executes the getLogs operation.
     * @param keyword the keyword argument
     * @param type the type argument
     * @return the resulting object or status
     */
    /**
     * Executes the getLogs operation.
     * @param keyword the keyword argument
     * @param type the type argument
     * @return the resulting object or status
     */
    public List<LogEntry> getLogs(String keyword, String type) {
        DataStore data = dataService.getData();
        return data.getLogs().stream()
                .filter(l -> {
                    if (keyword == null || keyword.isEmpty()) return true;
                    String kw = keyword.toLowerCase();
                    return l.getAction().toLowerCase().contains(kw)
                            || (l.getDetails() != null && l.getDetails().toLowerCase().contains(kw))
                            || (l.getActor() != null && l.getActor().toLowerCase().contains(kw))
                            || (l.getId() != null && l.getId().toLowerCase().contains(kw));
                })
                .filter(l -> type == null || type.isEmpty() || l.getType().equalsIgnoreCase(type))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    // ==================== Workload ====================

    /**
     * Workload is derived from confirmed offers: each application with status="offer" is
     * counted as an active job for its applicant, contributing the job's hoursPerWeek.
     */
    /**
     * Executes the getWorkload operation.
     * @param keyword the keyword argument
     * @param statusFilter the statusFilter argument
     * @return the resulting object or status
     */
    /**
     * Executes the getWorkload operation.
     * @param keyword the keyword argument
     * @param statusFilter the statusFilter argument
     * @return the resulting object or status
     */
    public List<JsonObject> getWorkload(String keyword, String statusFilter) {
        return getWorkload(keyword,statusFilter,null);
    }
    public List<JsonObject> getWorkload(String keyword,String statusFilter,String periodId) {
        return new AssignmentReportingService(dataService.getData()).admin(keyword,statusFilter,periodId);
    }

    /**
     * Reassign a confirmed offer.
     *
     * Body schema:
     *   { "action": "reopen",   "applicationId": "..." }
     *   { "action": "transfer", "applicationId": "...", "targetUserId": "..." }
     *
     * - reopen   : revoke the current offer (set application status to "rejected") and
     *              reopen the job (set job status to "active") so it can be applied to again.
     * - transfer : reassign the existing offer to another applicant (only changes applicantId).
     */
    /**
     * Executes the reassignWorkload operation.
     * @param taUserId the taUserId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the reassignWorkload operation.
     * @param taUserId the taUserId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> reassignWorkload(String taUserId, JsonObject body) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();

        if (body.has("applicationId") && SectionAssignmentService.hasHistory(data,"applications",body.get("applicationId").getAsString())) {
            result.put("error","Use TA Assignments to cancel and create a new assignment. Application identity and reply history must be retained.");result.put("code",409);return result;
        }
        String action = body.has("action") ? body.get("action").getAsString() : "";
        String applicationId = body.has("applicationId") ? body.get("applicationId").getAsString() : null;

        if (applicationId == null || applicationId.isBlank()) {
            result.put("error", "Missing applicationId");
            result.put("code", 400);
            return result;
        }

        Application app = data.getApplications().stream()
                .filter(a -> applicationId.equals(a.getId()) && taUserId.equals(a.getApplicantId()))
                .findFirst().orElse(null);
        if (app == null) {
            result.put("error", "Offer record not found for this TA");
            result.put("code", 404);
            return result;
        }
        if (!"offer".equals(app.getStatus())) {
            result.put("error", "Selected record is not an active offer");
            result.put("code", 400);
            return result;
        }

        Job job = data.getJobs().stream()
                .filter(j -> j.getId().equals(app.getJobId()))
                .findFirst().orElse(null);
        if (job == null) {
            result.put("error", "Associated job not found");
            result.put("code", 404);
            return result;
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        switch (action) {
            case "reopen" -> {
                app.setStatus("rejected");
                app.setUpdatedAt(now);
                markTimelineRejected(app, today);
                job.setStatus("active");
                dataService.save();
                result.put("success", true);
                result.put("message", "Offer revoked and job reopened for application");
                return result;
            }
            case "transfer" -> {
                String targetUserId = body.has("targetUserId") ? body.get("targetUserId").getAsString() : null;
                if (targetUserId == null || targetUserId.isBlank()) {
                    result.put("error", "Missing targetUserId");
                    result.put("code", 400);
                    return result;
                }
                if (targetUserId.equals(taUserId)) {
                    result.put("error", "Target TA is the same as the current TA");
                    result.put("code", 400);
                    return result;
                }

                User target = data.getUsers().stream()
                        .filter(u -> u.getId().equals(targetUserId))
                        .findFirst().orElse(null);
                if (target == null || !"applicant".equals(target.getRole())) {
                    result.put("error", "Target user is not a valid applicant");
                    result.put("code", 400);
                    return result;
                }
                if ("suspended".equals(target.getStatus())) {
                    result.put("error", "Target user account is suspended");
                    result.put("code", 400);
                    return result;
                }

                boolean duplicate = data.getApplications().stream()
                        .anyMatch(a -> !a.getId().equals(app.getId())
                                && job.getId().equals(a.getJobId())
                                && targetUserId.equals(a.getApplicantId()));
                if (duplicate) {
                    result.put("error", "Target TA already has an application for this job");
                    result.put("code", 409);
                    return result;
                }

                Profile targetProfile = data.getProfiles().stream()
                        .filter(p -> p.getUserId().equals(targetUserId))
                        .findFirst().orElse(null);
                int targetMax = targetProfile != null ? targetProfile.getMaxWeeklyHours() : 20;
                int targetCurrent = data.getApplications().stream()
                        .filter(a -> "offer".equals(a.getStatus()) && targetUserId.equals(a.getApplicantId()))
                        .mapToInt(a -> {
                            Job j = data.getJobs().stream()
                                    .filter(x -> x.getId().equals(a.getJobId()))
                                    .findFirst().orElse(null);
                            return j != null ? j.getHoursPerWeek() : 0;
                        }).sum();
                if (targetCurrent + job.getHoursPerWeek() > targetMax) {
                    result.put("error", "Transfer would exceed target TA's maximum weekly hours");
                    result.put("code", 400);
                    return result;
                }

                app.setApplicantId(targetUserId);
                app.setUpdatedAt(now);
                dataService.save();
                result.put("success", true);
                result.put("message", "Offer transferred to " + target.getName());
                return result;
            }
            default -> {
                result.put("error", "Invalid action; expected 'reopen' or 'transfer'");
                result.put("code", 400);
                return result;
            }
        }
    }

    private static void markTimelineRejected(Application app, String today) {
        if (app.getTimeline() == null) return;
        for (Application.TimelineEntry t : app.getTimeline()) {
            if ("offer".equals(t.getStatus())) {
                t.setCompleted(false);
                t.setPending(null);
                t.setError(Boolean.TRUE);
                t.setDate(today);
            }
        }
    }

    private JsonObject buildSafeUser(User user) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", user.getId());
        obj.addProperty("name", user.getName());
        obj.addProperty("email", user.getEmail());
        obj.addProperty("role", user.getRole());
        obj.addProperty("department", user.getDepartment());
        obj.addProperty("status", user.getStatus());
        obj.addProperty("lastLogin", user.getLastLogin());
        obj.addProperty("createdAt", user.getCreatedAt());
        return obj;
    }

    // ==================== Data Editor (Generic CRUD) ====================

    private static final List<String> COLLECTION_NAMES = Arrays.asList(
            "users", "profiles", "modules", "jobs", "applications",
            "notifications", "terms", "departments",
            "logs", "workloadAssignments", "messages"
    );

    private static String buildConversationId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    /**
     * Executes the getCollectionNames operation.
     * @return the resulting object or status
     */
    /**
     * Executes the getCollectionNames operation.
     * @return the resulting object or status
     */
    public JsonArray getCollectionNames() {
        JsonArray arr = new JsonArray();
        COLLECTION_NAMES.forEach(arr::add);
        return arr;
    }

    /**
     * Executes the getCollectionData operation.
     * @param collection the collection argument
     * @return the resulting object or status
     */
    /**
     * Executes the getCollectionData operation.
     * @param collection the collection argument
     * @return the resulting object or status
     */
    public JsonElement getCollectionData(String collection) {
        DataStore data = dataService.getData();
        Gson gson = JsonUtil.getGson();
        return switch (collection) {
            case "users" -> gson.toJsonTree(data.getUsers().stream().map(this::buildSafeUser).collect(Collectors.toList()));
            case "profiles" -> gson.toJsonTree(data.getProfiles());
            case "modules" -> gson.toJsonTree(data.getModules());
            case "jobs" -> gson.toJsonTree(data.getJobs());
            case "applications" -> gson.toJsonTree(data.getApplications());
            case "notifications" -> gson.toJsonTree(data.getNotifications());
            case "terms" -> gson.toJsonTree(data.getTerms());
            case "departments" -> gson.toJsonTree(data.getDepartments());
            case "logs" -> gson.toJsonTree(data.getLogs());
            case "workloadAssignments" -> gson.toJsonTree(data.getWorkloadAssignments());
            case "messages" -> gson.toJsonTree(data.getMessages() != null ? data.getMessages() : List.of());
            default -> new JsonArray();
        };
    }

    /**
     * Executes the addToCollection operation.
     * @param collection the collection argument
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the addToCollection operation.
     * @param collection the collection argument
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> addToCollection(String collection, JsonObject body) {
        DataStore data = dataService.getData();
        Gson gson = JsonUtil.getGson();
        Map<String, Object> result = new HashMap<>();
        if ("users".equals(collection)) {
            result.put("error", "Manage accounts through Users, not Data Editor."); result.put("code", 403); return result;
        }

        if ("interviews".equals(collection)) {
            result.put("error", "Interview management has been removed. Historical records are retained.");
            result.put("code", 410); return result;
        }
        if ("applications".equals(collection) && body.has("status") && "interview".equals(body.get("status").getAsString())) {
            result.put("error", "Use an active application review status."); result.put("code", 400); return result;
        }



        // Referential integrity checks before insert
        String validationError = validateCreate(collection, body, data);
        if (validationError != null) {
            result.put("error", validationError);
            result.put("code", 400);
            return result;
        }

        try {
            Object item = switch (collection) {
                case "users" -> {
                    User u = gson.fromJson(body, User.class);
                    if(u.getPassword()==null||u.getPassword().isBlank()){result.put("error","An initial password is required.");result.put("code",400);yield null;}
                    u.setPassword(PasswordSecurity.hash(u.getPassword()));
                    if (u.getId() == null || u.getId().isBlank())
                        u.setId(dataService.generateId("u", data.getUsers()));
                    if (u.getCreatedAt() == null) u.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    if (u.getStatus() == null) u.setStatus("active");
                    boolean emailExists = data.getUsers().stream().anyMatch(x -> x.getEmail().equalsIgnoreCase(u.getEmail()));
                    if (emailExists) { result.put("error", "Email already exists"); result.put("code", 409); yield null; }
                    data.getUsers().add(u);
                    yield u;
                }
                case "profiles" -> {
                    Profile p = gson.fromJson(body, Profile.class);
                    if (p.getId() == null || p.getId().isBlank())
                        p.setId(dataService.generateId("p", data.getProfiles()));
                    boolean profileExists = data.getProfiles().stream().anyMatch(x -> x.getUserId().equals(p.getUserId()));
                    if (profileExists) { result.put("error", "Profile already exists for this user"); result.put("code", 409); yield null; }
                    data.getProfiles().add(p);
                    yield p;
                }
                case "modules" -> {
                    Module m = gson.fromJson(body, Module.class);
                    if (m.getId() == null || m.getId().isBlank())
                        m.setId(dataService.generateId("m", data.getModules()));
                    boolean codeExists = data.getModules().stream().anyMatch(x -> x.getCode().equalsIgnoreCase(m.getCode()));
                    if (codeExists) { result.put("error", "Module code already exists"); result.put("code", 409); yield null; }
                    data.getModules().add(m);
                    yield m;
                }
                case "jobs" -> {
                    Job j = gson.fromJson(body, Job.class);
                    if (j.getId() == null || j.getId().isBlank())
                        j.setId(dataService.generateId("j", data.getJobs()));
                    if (j.getCreatedAt() == null) j.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    data.getJobs().add(j);
                    yield j;
                }
                case "applications" -> {
                    Application a = gson.fromJson(body, Application.class);
                    if (a.getId() == null || a.getId().isBlank())
                        a.setId(dataService.generateId("a", data.getApplications()));
                    if (a.getAppliedAt() == null) a.setAppliedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    data.getApplications().add(a);
                    yield a;
                }
                case "notifications" -> {
                    Notification n = gson.fromJson(body, Notification.class);
                    if (n.getId() == null || n.getId().isBlank())
                        n.setId(dataService.generateId("n", data.getNotifications()));
                    if (n.getCreatedAt() == null) n.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    data.getNotifications().add(n);
                    yield n;
                }
                case "terms" -> {
                    Term t = gson.fromJson(body, Term.class);
                    if (t.getId() == null || t.getId().isBlank())
                        t.setId(dataService.generateId("t", data.getTerms()));
                    boolean nameExists = data.getTerms().stream().anyMatch(x -> x.getName().equalsIgnoreCase(t.getName()));
                    if (nameExists) { result.put("error", "Term name already exists"); result.put("code", 409); yield null; }
                    data.getTerms().add(t);
                    yield t;
                }
                case "departments" -> {
                    Department d = gson.fromJson(body, Department.class);
                    if (d.getId() == null || d.getId().isBlank())
                        d.setId(dataService.generateId("d", data.getDepartments()));
                    boolean deptExists = data.getDepartments().stream().anyMatch(x -> x.getName().equalsIgnoreCase(d.getName()));
                    if (deptExists) { result.put("error", "Department name already exists"); result.put("code", 409); yield null; }
                    data.getDepartments().add(d);
                    yield d;
                }
                case "workloadAssignments" -> {
                    WorkloadAssignment w = gson.fromJson(body, WorkloadAssignment.class);
                    if (w.getId() == null || w.getId().isBlank())
                        w.setId(dataService.generateId("w", data.getWorkloadAssignments()));
                    data.getWorkloadAssignments().add(w);
                    yield w;
                }
                case "logs" -> {
                    LogEntry l = gson.fromJson(body, LogEntry.class);
                    if (l.getId() == null || l.getId().isBlank())
                        l.setId(dataService.generateId("log", data.getLogs()));
                    if (l.getTimestamp() == null)
                        l.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    data.getLogs().add(l);
                    yield l;
                }
                case "messages" -> {
                    Message m = gson.fromJson(body, Message.class);
                    if (data.getMessages() == null) data.setMessages(new ArrayList<>());
                    if (m.getId() == null || m.getId().isBlank())
                        m.setId(dataService.generateId("msg", data.getMessages()));
                    if (m.getConversationId() == null || m.getConversationId().isBlank()) {
                        if (m.getSenderId() != null && m.getReceiverId() != null)
                            m.setConversationId(buildConversationId(m.getSenderId(), m.getReceiverId()));
                    }
                    if (m.getTimestamp() == null)
                        m.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    data.getMessages().add(m);
                    yield m;
                }
                default -> null;
            };

            if (item == null) {
                if (!result.containsKey("error")) {
                    result.put("error", "Unsupported collection: " + collection);
                    result.put("code", 400);
                }
                return result;
            }
            dataService.save();
            result.put("item", item);
        } catch (Exception e) {
            result.put("error", "Invalid data format: " + e.getMessage());
            result.put("code", 400);
        }
        return result;
    }

    private String validateCreate(String collection, JsonObject body, DataStore data) {
        switch (collection) {
            case "jobs" -> {
                String moduleId = body.has("moduleId") ? body.get("moduleId").getAsString() : null;
                if (moduleId != null && !moduleId.isBlank()) {
                    boolean moduleExists = data.getModules().stream().anyMatch(m -> m.getId().equals(moduleId));
                    if (!moduleExists) return "Referenced module (moduleId=" + moduleId + ") does not exist";
                }
                String organiserId = body.has("organiserId") ? body.get("organiserId").getAsString() : null;
                if (organiserId != null && !organiserId.isBlank()) {
                    boolean organiserExists = data.getUsers().stream()
                            .anyMatch(u -> u.getId().equals(organiserId) && "organiser".equals(u.getRole()));
                    if (!organiserExists) return "Referenced Teacher (organiserId=" + organiserId + ") does not exist or is not a Teacher";
                }
            }
            case "applications" -> {
                String periodId = body.has("periodId") && !body.get("periodId").isJsonNull() ? body.get("periodId").getAsString() : null;
                if (periodId == null || data.getApplicationPeriods().stream().noneMatch(p -> p.getId().equals(periodId)))
                    return "An existing application period (periodId) is required";
                String jobId = body.has("jobId") ? body.get("jobId").getAsString() : null;
                if (jobId == null || jobId.isBlank()) return "jobId is required";
                boolean jobExists = data.getJobs().stream().anyMatch(j -> j.getId().equals(jobId));
                if (!jobExists) return "Referenced job (jobId=" + jobId + ") does not exist";

                String applicantId = body.has("applicantId") ? body.get("applicantId").getAsString() : null;
                if (applicantId == null || applicantId.isBlank()) return "applicantId is required";
                boolean applicantExists = data.getUsers().stream()
                        .anyMatch(u -> u.getId().equals(applicantId) && "applicant".equals(u.getRole()));
                if (!applicantExists) return "Referenced applicant (applicantId=" + applicantId + ") does not exist or is not an applicant";
            }
            case "profiles" -> {
                String userId = body.has("userId") ? body.get("userId").getAsString() : null;
                if (userId == null || userId.isBlank()) return "userId is required";
                boolean userExists = data.getUsers().stream().anyMatch(u -> u.getId().equals(userId));
                if (!userExists) return "Referenced user (userId=" + userId + ") does not exist";
            }
            case "notifications" -> {
                String userId = body.has("userId") ? body.get("userId").getAsString() : null;
                if (userId != null && !userId.isBlank()) {
                    boolean userExists = data.getUsers().stream().anyMatch(u -> u.getId().equals(userId));
                    if (!userExists) return "Referenced user (userId=" + userId + ") does not exist";
                }
            }
            case "workloadAssignments" -> {
                String taUserId = body.has("taUserId") ? body.get("taUserId").getAsString() : null;
                if (taUserId != null && !taUserId.isBlank()) {
                    boolean userExists = data.getUsers().stream().anyMatch(u -> u.getId().equals(taUserId));
                    if (!userExists) return "Referenced TA user (taUserId=" + taUserId + ") does not exist";
                }
                String moduleCode = body.has("moduleCode") ? body.get("moduleCode").getAsString() : null;
                if (moduleCode != null && !moduleCode.isBlank()) {
                    boolean modExists = data.getModules().stream().anyMatch(m -> m.getCode().equals(moduleCode));
                    if (!modExists) return "Referenced module (moduleCode=" + moduleCode + ") does not exist";
                }
            }
            case "modules" -> {
                String organiserId = body.has("organiserId") ? body.get("organiserId").getAsString() : null;
                if (organiserId != null && !organiserId.isBlank()) {
                    boolean organiserExists = data.getUsers().stream()
                            .anyMatch(u -> u.getId().equals(organiserId) && "organiser".equals(u.getRole()));
                    if (!organiserExists) return "Referenced Teacher (organiserId=" + organiserId + ") does not exist or is not a Teacher";
                }
            }
            case "messages" -> {
                String senderId = body.has("senderId") ? body.get("senderId").getAsString() : null;
                if (senderId == null || senderId.isBlank()) return "senderId is required";
                if (data.getUsers().stream().noneMatch(u -> u.getId().equals(senderId)))
                    return "Referenced sender (senderId=" + senderId + ") does not exist";

                String receiverId = body.has("receiverId") ? body.get("receiverId").getAsString() : null;
                if (receiverId == null || receiverId.isBlank()) return "receiverId is required";
                if (data.getUsers().stream().noneMatch(u -> u.getId().equals(receiverId)))
                    return "Referenced receiver (receiverId=" + receiverId + ") does not exist";

                if (senderId.equals(receiverId)) return "senderId and receiverId must be different";

                String content = body.has("content") ? body.get("content").getAsString() : null;
                if (content == null || content.isBlank()) return "content is required";
            }
            case "logs" -> {
                String actorId = body.has("actorId") ? body.get("actorId").getAsString() : null;
                if (actorId != null && !actorId.isBlank()
                        && data.getUsers().stream().noneMatch(u -> u.getId().equals(actorId)))
                    return "Referenced actor (actorId=" + actorId + ") does not exist";
            }
            default -> {}
        }
        return null;
    }

    private String validateUpdate(String collection, JsonObject body, DataStore data, String itemId) {
        switch (collection) {
            case "jobs" -> {
                if (body.has("moduleId")) {
                    String moduleId = body.get("moduleId").getAsString();
                    if (!moduleId.isBlank() && data.getModules().stream().noneMatch(m -> m.getId().equals(moduleId)))
                        return "Referenced module (moduleId=" + moduleId + ") does not exist";
                }
                if (body.has("organiserId")) {
                    String organiserId = body.get("organiserId").getAsString();
                    if (!organiserId.isBlank() && data.getUsers().stream()
                            .noneMatch(u -> u.getId().equals(organiserId) && "organiser".equals(u.getRole())))
                        return "Referenced Teacher (organiserId=" + organiserId + ") does not exist or is not a Teacher";
                }
            }
            case "applications" -> {
                if (body.has("periodId")) {
                    Application existing = data.getApplications().stream().filter(a -> a.getId().equals(itemId)).findFirst().orElse(null);
                    if (existing != null && (body.get("periodId").isJsonNull()
                            || !java.util.Objects.equals(existing.getPeriodId(), body.get("periodId").getAsString())))
                        return "An application's original period cannot be changed";
                }
                if (body.has("jobId")) {
                    String jobId = body.get("jobId").getAsString();
                    if (!jobId.isBlank() && data.getJobs().stream().noneMatch(j -> j.getId().equals(jobId)))
                        return "Referenced job (jobId=" + jobId + ") does not exist";
                }
                if (body.has("applicantId")) {
                    String applicantId = body.get("applicantId").getAsString();
                    if (!applicantId.isBlank() && data.getUsers().stream()
                            .noneMatch(u -> u.getId().equals(applicantId) && "applicant".equals(u.getRole())))
                        return "Referenced applicant (applicantId=" + applicantId + ") does not exist or is not an applicant";
                }
            }
            case "profiles" -> {
                if (body.has("userId")) {
                    String userId = body.get("userId").getAsString();
                    if (!userId.isBlank() && data.getUsers().stream().noneMatch(u -> u.getId().equals(userId)))
                        return "Referenced user (userId=" + userId + ") does not exist";
                }
            }
            case "notifications" -> {
                if (body.has("userId")) {
                    String userId = body.get("userId").getAsString();
                    if (!userId.isBlank() && data.getUsers().stream().noneMatch(u -> u.getId().equals(userId)))
                        return "Referenced user (userId=" + userId + ") does not exist";
                }
            }
            case "workloadAssignments" -> {
                if (body.has("taUserId")) {
                    String taUserId = body.get("taUserId").getAsString();
                    if (!taUserId.isBlank() && data.getUsers().stream().noneMatch(u -> u.getId().equals(taUserId)))
                        return "Referenced TA user (taUserId=" + taUserId + ") does not exist";
                }
                if (body.has("moduleCode")) {
                    String moduleCode = body.get("moduleCode").getAsString();
                    if (!moduleCode.isBlank() && data.getModules().stream().noneMatch(m -> m.getCode().equals(moduleCode)))
                        return "Referenced module (moduleCode=" + moduleCode + ") does not exist";
                }
            }
            case "modules" -> {
                if (body.has("organiserId")) {
                    String organiserId = body.get("organiserId").getAsString();
                    if (!organiserId.isBlank() && data.getUsers().stream()
                            .noneMatch(u -> u.getId().equals(organiserId) && "organiser".equals(u.getRole())))
                        return "Referenced Teacher (organiserId=" + organiserId + ") does not exist or is not a Teacher";
                }
            }
            case "messages" -> {
                if (body.has("senderId")) {
                    String senderId = body.get("senderId").getAsString();
                    if (!senderId.isBlank() && data.getUsers().stream().noneMatch(u -> u.getId().equals(senderId)))
                        return "Referenced sender (senderId=" + senderId + ") does not exist";
                }
                if (body.has("receiverId")) {
                    String receiverId = body.get("receiverId").getAsString();
                    if (!receiverId.isBlank() && data.getUsers().stream().noneMatch(u -> u.getId().equals(receiverId)))
                        return "Referenced receiver (receiverId=" + receiverId + ") does not exist";
                }
            }
            case "logs" -> {
                if (body.has("actorId")) {
                    String actorId = body.get("actorId").getAsString();
                    if (!actorId.isBlank() && data.getUsers().stream().noneMatch(u -> u.getId().equals(actorId)))
                        return "Referenced actor (actorId=" + actorId + ") does not exist";
                }
            }
            default -> {}
        }
        return null;
    }

    /**
     * Executes the updateCollectionItem operation.
     * @param collection the collection argument
     * @param itemId the itemId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    /**
     * Executes the updateCollectionItem operation.
     * @param collection the collection argument
     * @param itemId the itemId argument
     * @param body the body argument
     * @return the resulting object or status
     */
    public Map<String, Object> updateCollectionItem(String collection, String itemId, JsonObject body) {
        DataStore data = dataService.getData();
        Gson gson = JsonUtil.getGson();
        Map<String, Object> result = new HashMap<>();
        if ("users".equals(collection)) {
            result.put("error", "Manage accounts through Users, not Data Editor."); result.put("code", 403); return result;
        }
        if ("modules".equals(collection)) {
            Map<String, Object> updated = updateModule(itemId, body);
            if (updated.containsKey("error")) { result.put("error", updated.get("error")); result.put("code", updated.get("code_http")); }
            else result.put("item", updated.get("module"));
            return result;
        }

        if ("interviews".equals(collection)) {
            result.put("error", "Interview management has been removed. Historical records are retained.");
            result.put("code", 410); return result;
        }
        if ("applications".equals(collection) && body.has("status") && "interview".equals(body.get("status").getAsString())
                && data.getApplications().stream().noneMatch(a -> itemId.equals(a.getId()) && "interview".equals(a.getStatus()))) {
            result.put("error", "Use an active application review status."); result.put("code", 400); return result;
        }



        if ("applications".equals(collection) && body.has("status")
                && data.getApplications().stream().anyMatch(a -> itemId.equals(a.getId())
                    && !java.util.Objects.equals(a.getStatus(), body.get("status").getAsString()))) {
            result.put("error", "Change application status through Pipeline or Review Workspace to preserve decision history.");
            result.put("code", 409); return result;
        }
        if ("applications".equals(collection) && SectionAssignmentService.activeForApplication(data,itemId)
                && (body.has("jobId") || body.has("applicantId") || (body.has("status") && !"offer".equals(body.get("status").getAsString())))) {
            result.put("error", "Cancel active TA assignments before changing application ownership or status."); result.put("code",409); return result;
        }
        if ("jobs".equals(collection) && SectionAssignmentService.hasHistory(data,"jobs",itemId) && (body.has("moduleId") || body.has("organiserId"))) {
            result.put("error", "A position with assignment history cannot be moved to another course or Teacher."); result.put("code",409); return result;
        }
        if("applications".equals(collection)&&body.has("status")&&"offer".equals(body.get("status").getAsString())&&!ReviewService.approved(data,itemId)){result.put("error","Record selection approval in Review Workspace first.");result.put("code",409);return result;}
        if("applications".equals(collection)&&SectionAssignmentService.hasHistory(data,collection,itemId)&&(body.has("jobId")||body.has("applicantId"))){result.put("error","Reviewed applications cannot change applicant or position.");result.put("code",409);return result;}
        // Validate foreign key references on update
        String validationError = validateUpdate(collection, body, data, itemId);
        if (validationError != null) {
            result.put("error", validationError);
            result.put("code", 400);
            return result;
        }

        try {
            Object updated = switch (collection) {
                case "users" -> {
                    User u = data.getUsers().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (u == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("name")) u.setName(body.get("name").getAsString());
                    if (body.has("email")) u.setEmail(body.get("email").getAsString());
                    if (body.has("password")) u.setPassword(PasswordSecurity.hash(body.get("password").getAsString()));
                    if (body.has("phone")) u.setPhone(body.get("phone").getAsString());
                    if (body.has("role")&&!java.util.Objects.equals(u.getRole(),body.get("role").getAsString())) {u.setRole(body.get("role").getAsString());u.setPermissions(null);}
                    if (body.has("department")) u.setDepartment(body.get("department").getAsString());
                    if (body.has("status")) u.setStatus(body.get("status").getAsString());
                    yield u;
                }
                case "profiles" -> {
                    Profile p = data.getProfiles().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (p == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("userId")) p.setUserId(body.get("userId").getAsString());
                    if (body.has("studentId")) p.setStudentId(body.get("studentId").getAsString());
                    if (body.has("degree")) p.setDegree(body.get("degree").getAsString());
                    if (body.has("year")) p.setYear(body.get("year").getAsInt());
                    if (body.has("gpa")) p.setGpa(body.get("gpa").getAsDouble());
                    if (body.has("maxGpa")) p.setMaxGpa(body.get("maxGpa").getAsDouble());
                    if (body.has("maxWeeklyHours")) p.setMaxWeeklyHours(body.get("maxWeeklyHours").getAsInt());
                    if (body.has("currentWeeklyHours")) p.setCurrentWeeklyHours(body.get("currentWeeklyHours").getAsInt());
                    if (body.has("skills")) p.setSkills(gson.fromJson(body.get("skills"), List.class));
                    yield p;
                }
                case "modules" -> {
                    Module m = data.getModules().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (m == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("code")) m.setCode(body.get("code").getAsString());
                    if (body.has("name")) m.setName(body.get("name").getAsString());
                    if (body.has("department")) m.setDepartment(body.get("department").getAsString());
                    if (body.has("organiserId")) m.setOrganiserId(body.get("organiserId").getAsString());
                    if (body.has("organiserName")) m.setOrganiserName(body.get("organiserName").getAsString());
                    if (body.has("term")) m.setTerm(body.get("term").getAsString());
                    if (body.has("status")) m.setStatus(body.get("status").getAsString());
                    yield m;
                }
                case "jobs" -> {
                    Job j = data.getJobs().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (j == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("moduleId")) j.setModuleId(body.get("moduleId").getAsString());
                    if (body.has("moduleCode")) j.setModuleCode(body.get("moduleCode").getAsString());
                    if (body.has("title")) j.setTitle(body.get("title").getAsString());
                    if (body.has("description")) j.setDescription(body.get("description").getAsString());
                    if (body.has("professor")) j.setProfessor(body.get("professor").getAsString());
                    if (body.has("department")) j.setDepartment(body.get("department").getAsString());
                    if (body.has("type")) j.setType(body.get("type").getAsString());
                    if (body.has("hoursPerWeek")) j.setHoursPerWeek(body.get("hoursPerWeek").getAsInt());
                    if (body.has("positions")) j.setPositions(body.get("positions").getAsInt());
                    if (body.has("deadline")) j.setDeadline(body.get("deadline").getAsString());
                    if (body.has("status")) j.setStatus(body.get("status").getAsString());
                    if (body.has("organiserId")) j.setOrganiserId(body.get("organiserId").getAsString());
                    if (body.has("tags")) j.setTags(gson.fromJson(body.get("tags"), List.class));
                    if (body.has("requirements")) j.setRequirements(gson.fromJson(body.get("requirements"), List.class));
                    if (body.has("responsibilities")) j.setResponsibilities(gson.fromJson(body.get("responsibilities"), List.class));
                    yield j;
                }
                case "applications" -> {
                    Application a = data.getApplications().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (a == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("jobId")) a.setJobId(body.get("jobId").getAsString());
                    if (body.has("applicantId")) a.setApplicantId(body.get("applicantId").getAsString());
                    if (body.has("status")) a.setStatus(body.get("status").getAsString());
                    if (body.has("coverLetter")) a.setCoverLetter(body.get("coverLetter").getAsString());
                    if (body.has("matchScore")) a.setMatchScore(body.get("matchScore").getAsInt());
                    if (body.has("riskFlag")) a.setRiskFlag(body.get("riskFlag").isJsonNull() ? null : body.get("riskFlag").getAsString());
                    if (body.has("aiSummary")) a.setAiSummary(body.get("aiSummary").getAsString());
                    a.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    yield a;
                }
                case "notifications" -> {
                    Notification n = data.getNotifications().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (n == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("userId")) n.setUserId(body.get("userId").getAsString());
                    if (body.has("type")) n.setType(body.get("type").getAsString());
                    if (body.has("title")) n.setTitle(body.get("title").getAsString());
                    if (body.has("message")) n.setMessage(body.get("message").getAsString());
                    if (body.has("relatedId")) n.setRelatedId(body.get("relatedId").getAsString());
                    if (body.has("read")) n.setRead(body.get("read").getAsBoolean());
                    yield n;
                }
                case "terms" -> {
                    Term t = data.getTerms().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (t == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("name")) t.setName(body.get("name").getAsString());
                    if (body.has("startDate")) t.setStartDate(body.get("startDate").getAsString());
                    if (body.has("endDate")) t.setEndDate(body.get("endDate").getAsString());
                    if (body.has("status")) t.setStatus(body.get("status").getAsString());
                    yield t;
                }
                case "departments" -> {
                    Department d = data.getDepartments().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (d == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("name")) d.setName(body.get("name").getAsString());
                    if (body.has("moduleCount")) d.setModuleCount(body.get("moduleCount").getAsInt());
                    if (body.has("status")) d.setStatus(body.get("status").getAsString());
                    yield d;
                }
                case "logs" -> {
                    LogEntry l = data.getLogs().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (l == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("action")) l.setAction(body.get("action").getAsString());
                    if (body.has("type")) l.setType(body.get("type").getAsString());
                    if (body.has("actor")) l.setActor(body.get("actor").getAsString());
                    if (body.has("details")) l.setDetails(body.get("details").getAsString());
                    yield l;
                }
                case "workloadAssignments" -> {
                    WorkloadAssignment w = data.getWorkloadAssignments().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (w == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("taUserId")) w.setTaUserId(body.get("taUserId").getAsString());
                    if (body.has("moduleCode")) w.setModuleCode(body.get("moduleCode").getAsString());
                    if (body.has("hoursPerWeek")) w.setHoursPerWeek(body.get("hoursPerWeek").getAsInt());
                    if (body.has("term")) w.setTerm(body.get("term").getAsString());
                    if (body.has("status")) w.setStatus(body.get("status").getAsString());
                    yield w;
                }
                case "messages" -> {
                    if (data.getMessages() == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    Message m = data.getMessages().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                    if (m == null) { result.put("error", "Record not found"); result.put("code", 404); yield null; }
                    if (body.has("conversationId")) m.setConversationId(body.get("conversationId").getAsString());
                    if (body.has("senderId")) m.setSenderId(body.get("senderId").getAsString());
                    if (body.has("receiverId")) m.setReceiverId(body.get("receiverId").getAsString());
                    if (body.has("content")) m.setContent(body.get("content").getAsString());
                    if (body.has("timestamp")) m.setTimestamp(body.get("timestamp").getAsString());
                    if (body.has("read")) m.setRead(body.get("read").getAsBoolean());
                    if (body.has("replyToId")) m.setReplyToId(body.get("replyToId").isJsonNull() ? null : body.get("replyToId").getAsString());
                    if (body.has("recalled")) m.setRecalled(body.get("recalled").getAsBoolean());
                    if (body.has("edited")) m.setEdited(body.get("edited").getAsBoolean());
                    if (body.has("editedAt")) m.setEditedAt(body.get("editedAt").isJsonNull() ? null : body.get("editedAt").getAsString());
                    if (body.has("messageType")) m.setMessageType(body.get("messageType").getAsString());
                    if (body.has("attachmentUrl")) m.setAttachmentUrl(body.get("attachmentUrl").isJsonNull() ? null : body.get("attachmentUrl").getAsString());
                    if (body.has("attachmentName")) m.setAttachmentName(body.get("attachmentName").isJsonNull() ? null : body.get("attachmentName").getAsString());
                    if (body.has("attachmentSize")) m.setAttachmentSize(body.get("attachmentSize").isJsonNull() ? null : body.get("attachmentSize").getAsString());
                    if ((m.getConversationId() == null || m.getConversationId().isBlank())
                            && m.getSenderId() != null && m.getReceiverId() != null) {
                        m.setConversationId(buildConversationId(m.getSenderId(), m.getReceiverId()));
                    }
                    yield m;
                }
                default -> null;
            };

            if (updated == null && !result.containsKey("error")) {
                result.put("error", "Unsupported collection: " + collection);
                result.put("code", 400);
                return result;
            }
            if (result.containsKey("error")) return result;
            dataService.save();
            result.put("item", updated);
        } catch (Exception e) {
            result.put("error", "Update failed: " + e.getMessage());
            result.put("code", 400);
        }
        return result;
    }

    /**
     * Executes the deleteFromCollection operation.
     * @param collection the collection argument
     * @param itemId the itemId argument
     * @return the resulting object or status
     */
    /**
     * Executes the deleteFromCollection operation.
     * @param collection the collection argument
     * @param itemId the itemId argument
     * @return the resulting object or status
     */
    public Map<String, Object> deleteFromCollection(String collection, String itemId) {
        DataStore data = dataService.getData();
        Map<String, Object> result = new HashMap<>();
        if ("users".equals(collection)) {
            result.put("error", "Manage accounts through Users, not Data Editor."); result.put("code", 403); return result;
        }

        if ("interviews".equals(collection)) {
            result.put("error", "Interview management has been removed. Historical records are retained.");
            result.put("code", 410); return result;
        }

        if (SectionAssignmentService.hasHistory(data, collection, itemId)) { result.put("error", "Assignment history must be retained."); result.put("code",409); return result; }


        switch (collection) {
            case "users" -> {
                User user = data.getUsers().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                if (user == null) { result.put("error", "Record not found"); result.put("code", 404); break; }
                data.getUsers().remove(user);
                // Cascade: remove profile, jobs (as organiser), applications, interviews, notifications, workload
                data.getProfiles().removeIf(p -> itemId.equals(p.getUserId()));
                data.getWeeklyAvailabilities().removeIf(a -> itemId.equals(a.getApplicantId()));
                List<String> userJobIds = data.getJobs().stream()
                        .filter(j -> itemId.equals(j.getOrganiserId())).map(Job::getId).collect(Collectors.toList());
                data.getJobs().removeIf(j -> itemId.equals(j.getOrganiserId()));
                data.getApplications().removeIf(a -> itemId.equals(a.getApplicantId()) || userJobIds.contains(a.getJobId()));
                data.getInterviews().removeIf(i -> itemId.equals(i.getCandidateId()) || itemId.equals(i.getOrganiserId()));
                data.getNotifications().removeIf(n -> itemId.equals(n.getUserId()));
                data.getWorkloadAssignments().removeIf(w -> itemId.equals(w.getTaUserId()));
                if (data.getMessages() != null) {
                    data.getMessages().removeIf(m -> itemId.equals(m.getSenderId()) || itemId.equals(m.getReceiverId()));
                }
                result.put("success", true);
            }
            case "modules" -> {
                Module module = data.getModules().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                if (module == null) { result.put("error", "Record not found"); result.put("code", 404); break; }
                if (data.getCourseSections().stream().anyMatch(s -> itemId.equals(s.getModuleId()))) {
                    result.put("error", "Cannot delete a course with associated sections, including archived sections");
                    result.put("code", 409); break;
                }
                String moduleCode = module.getCode();
                data.getModules().remove(module);
                // Cascade: remove jobs under this module, their applications, and interviews
                List<String> jobIds = data.getJobs().stream()
                        .filter(j -> itemId.equals(j.getModuleId()) || moduleCode.equals(j.getModuleCode()))
                        .map(Job::getId).collect(Collectors.toList());
                data.getJobs().removeIf(j -> itemId.equals(j.getModuleId()) || moduleCode.equals(j.getModuleCode()));
                List<String> appIds = data.getApplications().stream()
                        .filter(a -> jobIds.contains(a.getJobId())).map(Application::getId).collect(Collectors.toList());
                data.getApplications().removeIf(a -> jobIds.contains(a.getJobId()));
                data.getInterviews().removeIf(i -> appIds.contains(i.getApplicationId()));
                data.getWorkloadAssignments().removeIf(w -> moduleCode.equals(w.getModuleCode()));
                result.put("success", true);
            }
            case "jobs" -> {
                Job job = data.getJobs().stream().filter(x -> x.getId().equals(itemId)).findFirst().orElse(null);
                if (job == null) { result.put("error", "Record not found"); result.put("code", 404); break; }
                data.getJobs().remove(job);
                // Cascade: remove applications for this job and their interviews
                List<String> appIds = data.getApplications().stream()
                        .filter(a -> itemId.equals(a.getJobId())).map(Application::getId).collect(Collectors.toList());
                data.getApplications().removeIf(a -> itemId.equals(a.getJobId()));
                data.getInterviews().removeIf(i -> appIds.contains(i.getApplicationId()));
                result.put("success", true);
            }
            case "applications" -> {
                boolean removed = data.getApplications().removeIf(x -> x.getId().equals(itemId));
                if (!removed) { result.put("error", "Record not found"); result.put("code", 404); break; }
                // Cascade: remove interviews for this application
                data.getInterviews().removeIf(i -> itemId.equals(i.getApplicationId()));
                result.put("success", true);
            }
            case "profiles" -> {
                boolean removed = data.getProfiles().removeIf(x -> x.getId().equals(itemId));
                if (!removed) { result.put("error", "Record not found"); result.put("code", 404); break; }
                result.put("success", true);
            }
            case "notifications" -> {
                boolean removed = data.getNotifications().removeIf(x -> x.getId().equals(itemId));
                if (!removed) { result.put("error", "Record not found"); result.put("code", 404); break; }
                result.put("success", true);
            }
            case "terms" -> {
                boolean removed = data.getTerms().removeIf(x -> x.getId().equals(itemId));
                if (!removed) { result.put("error", "Record not found"); result.put("code", 404); break; }
                result.put("success", true);
            }
            case "departments" -> {
                boolean removed = data.getDepartments().removeIf(x -> x.getId().equals(itemId));
                if (!removed) { result.put("error", "Record not found"); result.put("code", 404); break; }
                result.put("success", true);
            }
            case "logs" -> {
                boolean removed = data.getLogs().removeIf(x -> x.getId().equals(itemId));
                if (!removed) { result.put("error", "Record not found"); result.put("code", 404); break; }
                result.put("success", true);
            }
            case "workloadAssignments" -> {
                boolean removed = data.getWorkloadAssignments().removeIf(x -> x.getId().equals(itemId));
                if (!removed) { result.put("error", "Record not found"); result.put("code", 404); break; }
                result.put("success", true);
            }
            case "messages" -> {
                if (data.getMessages() == null) { result.put("error", "Record not found"); result.put("code", 404); break; }
                boolean removed = data.getMessages().removeIf(x -> x.getId().equals(itemId));
                if (!removed) { result.put("error", "Record not found"); result.put("code", 404); break; }
                result.put("success", true);
            }
            default -> { result.put("error", "Unsupported collection"); result.put("code", 400); }
        }

        if (result.containsKey("success")) {
            dataService.save();
        }
        return result;
    }

    /**
     * Build weekly application trend from real appliedAt dates.
     * Groups applications by ISO week and returns the most recent 6 weeks.
     */
    private JsonArray buildWeeklyApplicationTrend(DataStore data) {
        JsonArray trendData = new JsonArray();

        Map<LocalDate, Long> weekCounts = new TreeMap<>();

        for (com.tamo.model.Application app : data.getApplications()) {
            String appliedAt = app.getAppliedAt();
            if (appliedAt == null || appliedAt.isBlank()) continue;
            try {
                LocalDate date = LocalDate.parse(appliedAt.substring(0, 10));
                LocalDate weekStart = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                weekCounts.merge(weekStart, 1L, Long::sum);
            } catch (Exception ignored) {}
        }

        if (weekCounts.isEmpty()) {
            JsonObject point = new JsonObject();
            point.addProperty("name", "No data");
            point.addProperty("applicants", 0);
            trendData.add(point);
            return trendData;
        }

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MM/dd");
        List<Map.Entry<LocalDate, Long>> entries = new ArrayList<>(weekCounts.entrySet());
        int start = Math.max(0, entries.size() - 6);
        for (int i = start; i < entries.size(); i++) {
            Map.Entry<LocalDate, Long> e = entries.get(i);
            JsonObject point = new JsonObject();
            point.addProperty("name", "Week " + e.getKey().format(labelFmt));
            point.addProperty("applicants", e.getValue());
            trendData.add(point);
        }

        return trendData;
    }
}
