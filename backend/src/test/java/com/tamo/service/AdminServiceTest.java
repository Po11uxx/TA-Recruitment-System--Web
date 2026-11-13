package com.tamo.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tamo.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AdminService — Admin Feature Tests")
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private JsonDataService dataService;
    private AdminService service;
    private DataStore dataStore;

    @BeforeEach
    void setUp() {
        dataStore = TestDataFactory.createTestDataStore();
        lenient().when(dataService.getData()).thenReturn(dataStore);
        lenient().when(dataService.generateId(anyString(), anyList())).thenReturn("new001");
        service = new AdminService(dataService);
    }

    @Nested
    @DisplayName("getDashboard")
    class DashboardTests {
        @Test
        void pendingQueueExcludesDecisionsAndListsOldestFirst() {
            var pending = service.getDashboard().getAsJsonArray("pendingApplications");
            assertEquals(2, pending.size());
            assertEquals("a001", pending.get(0).getAsJsonObject().get("id").getAsString());
            assertEquals("a003", pending.get(1).getAsJsonObject().get("id").getAsString());
            var decision = new com.tamo.model.ReviewRecord();
            decision.setApplicationId("a001");
            decision.setAction("not_selected");
            dataStore.getReviewRecords().add(decision);
            pending = service.getDashboard().getAsJsonArray("pendingApplications");
            assertEquals(1, pending.size());
            assertEquals("a003", pending.get(0).getAsJsonObject().get("id").getAsString());
        }

        @Test
        @DisplayName("Should return system-wide analytics")
        void shouldReturnAnalytics() {
            JsonObject result = service.getDashboard();
            assertNotNull(result);
            assertTrue(result.has("totalApplicants"));
            assertTrue(result.has("activePostings"));
            assertTrue(result.has("acceptanceRate"));
            assertTrue(result.has("trendData"));
            assertTrue(result.has("moduleStats"));
            assertTrue(result.has("systemAlerts"));

            assertEquals(3, result.get("totalApplicants").getAsLong());
            assertEquals(2, result.get("activePostings").getAsLong());
        }

        @Test
        @DisplayName("Acceptance rate should be calculated correctly")
        void acceptanceRateShouldBeCorrect() {
            JsonObject result = service.getDashboard();
            String rate = result.get("acceptanceRate").getAsString();
            assertNotNull(rate);
            assertTrue(rate.endsWith("%"));
        }
    }

    @Nested
    @DisplayName("User Management")
    class UserManagementTests {
        @Test
        @DisplayName("getUsers should return all users without filters")
        void getUsersNoFilter() {
            List<JsonObject> users = service.getUsers(null, null, null);
            assertEquals(6, users.size());
        }

        @Test
        @DisplayName("getUsers should filter by role")
        void getUsersByRole() {
            List<JsonObject> applicants = service.getUsers(null, "applicant", null);
            assertEquals(3, applicants.size());
        }

        @Test
        @DisplayName("getUsers should filter by keyword")
        void getUsersByKeyword() {
            List<JsonObject> users = service.getUsers("Alex", null, null);
            assertEquals(1, users.size());
        }

        @Test
        @DisplayName("getUsers should filter by status")
        void getUsersByStatus() {
            List<JsonObject> active = service.getUsers(null, null, "active");
            assertEquals(5, active.size());
        }

        @Test
        @DisplayName("getUsers should not expose passwords")
        void usersShouldNotExposePasswords() {
            List<JsonObject> users = service.getUsers(null, null, null);
            for (JsonObject u : users) {
                assertFalse(u.has("password"));
            }
        }

        @Test
        @DisplayName("createUser should succeed with valid data")
        void createUserShouldSucceed() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "New User");
            body.addProperty("email", "newuser@university.edu");
            body.addProperty("role", "applicant");

            Map<String, Object> result = service.createUser(body);
            assertNotNull(result.get("user"));
            verify(dataService).save();
        }

        @Test
        @DisplayName("createUser with duplicate email should fail")
        void duplicateEmailShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "Dup");
            body.addProperty("email", "alex@university.edu");

            Map<String, Object> result = service.createUser(body);
            assertEquals(409, result.get("code"));
        }

        @Test
        @DisplayName("createUser with invalid role should fail")
        void invalidRoleShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "Test");
            body.addProperty("email", "test@university.edu");
            body.addProperty("role", "superadmin");

            Map<String, Object> result = service.createUser(body);
            assertEquals(400, result.get("code"));
        }

        @Test
        @DisplayName("createUser missing required fields should fail")
        void missingFieldsShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "");
            body.addProperty("email", "");

            Map<String, Object> result = service.createUser(body);
            assertEquals(400, result.get("code"));
        }

        @Test
        @DisplayName("updateUser should modify fields")
        void updateShouldModify() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "Updated Name");
            body.addProperty("status", "suspended");

            Map<String, Object> result = service.updateUser("u010", "u001", body);
            assertNotNull(result.get("user"));
            JsonObject updated = (JsonObject) result.get("user");
            assertEquals("Updated Name", updated.get("name").getAsString());
            assertEquals("suspended", updated.get("status").getAsString());
        }

        @Test
        @DisplayName("Admin cannot change own role")
        void cannotChangeOwnRole() {
            JsonObject body = new JsonObject();
            body.addProperty("role", "applicant");

            Map<String, Object> result = service.updateUser("u010", "u010", body);
            assertEquals(400, result.get("code"));
        }

        @Test
        @DisplayName("Update non-existent user should fail")
        void updateNonExistent() {
            Map<String, Object> result = service.updateUser("u010", "u999", new JsonObject());
            assertEquals(404, result.get("code"));
        }

        @Test
        @DisplayName("deleteUser should remove user")
        void deleteShouldRemove() {
            int before = dataStore.getUsers().size();
            Map<String, Object> result = service.deleteUser("u010", "u001");
            assertTrue((Boolean) result.get("success"));
            assertEquals(before - 1, dataStore.getUsers().size());
        }

        @Test
        @DisplayName("Admin cannot delete self")
        void cannotDeleteSelf() {
            Map<String, Object> result = service.deleteUser("u010", "u010");
            assertEquals(400, result.get("code"));
        }

        @Test
        @DisplayName("Delete non-existent user should fail")
        void deleteNonExistent() {
            Map<String, Object> result = service.deleteUser("u010", "u999");
            assertEquals(404, result.get("code"));
        }
    }

    @Nested
    @DisplayName("Module Management")
    class ModuleTests {
        @Test
        @DisplayName("getModules should return all modules")
        void getAllModules() {
            List<JsonObject> modules = service.getModules(null);
            assertEquals(2, modules.size());
        }

        @Test
        @DisplayName("getModules should filter by keyword")
        void filterByKeyword() {
            List<JsonObject> modules = service.getModules("Software");
            assertEquals(1, modules.size());
        }

        @Test
        @DisplayName("Module should include organiserName enrichment")
        void shouldEnrichOrganiserName() {
            List<JsonObject> modules = service.getModules(null);
            for (JsonObject m : modules) {
                assertTrue(m.has("organiserName"));
                assertNotEquals("-", m.get("organiserName").getAsString());
            }
        }

        @Test
        @DisplayName("createModule should succeed")
        void createShouldSucceed() {
            JsonObject body = new JsonObject();
            body.addProperty("code", "EBU7000");
            body.addProperty("name", "New Module");
            body.addProperty("department", "Computer Science");
            body.addProperty("term", "2025-2026 Spring");

            Map<String, Object> result = service.createModule(body);
            assertNotNull(result.get("module"));
        }

        @Test
        @DisplayName("createModule with duplicate code should fail")
        void duplicateCodeShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("code", "EBU6304");
            body.addProperty("name", "Duplicate");

            Map<String, Object> result = service.createModule(body);
            assertEquals(409, result.get("code_http"));
        }

        @Test
        @DisplayName("createModule with non-existent department should fail")
        void nonExistentDeptShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("code", "NEW001");
            body.addProperty("name", "Test");
            body.addProperty("department", "NonExistentDept");

            Map<String, Object> result = service.createModule(body);
            assertEquals(400, result.get("code_http"));
        }

        @Test
        @DisplayName("deleteModule with associated jobs should fail")
        void deleteWithJobsShouldFail() {
            Map<String, Object> result = service.deleteModule("m001");
            assertEquals(400, result.get("code"));
        }
    }

    @Nested
    @DisplayName("Properties: Terms & Departments")
    class PropertiesTests {
        @Test
        @DisplayName("getTerms should return all terms")
        void getTerms() {
            List<Term> terms = service.getTerms();
            assertEquals(1, terms.size());
        }

        @Test
        @DisplayName("createTerm should succeed")
        void createTermShouldSucceed() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "2026-2027 Fall");
            body.addProperty("startDate", "2026-09-01");
            body.addProperty("endDate", "2027-01-15");

            Map<String, Object> result = service.createTerm(body);
            assertNotNull(result.get("term"));
        }

        @Test
        @DisplayName("createTerm with duplicate name should fail")
        void duplicateTermShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "2025-2026 Spring");

            Map<String, Object> result = service.createTerm(body);
            assertEquals(409, result.get("code"));
        }

        @Test
        @DisplayName("createTerm with start after end should fail")
        void startAfterEndShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "Invalid Term");
            body.addProperty("startDate", "2027-06-01");
            body.addProperty("endDate", "2027-01-01");

            Map<String, Object> result = service.createTerm(body);
            assertEquals(400, result.get("code"));
        }

        @Test
        @DisplayName("getDepartments should return all departments")
        void getDepartments() {
            List<Department> depts = service.getDepartments();
            assertEquals(2, depts.size());
        }

        @Test
        @DisplayName("createDepartment should succeed")
        void createDeptShouldSucceed() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "Mathematics");

            Map<String, Object> result = service.createDepartment(body);
            assertNotNull(result.get("department"));
        }

        @Test
        @DisplayName("createDepartment with duplicate should fail")
        void duplicateDeptShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("name", "Computer Science");

            Map<String, Object> result = service.createDepartment(body);
            assertEquals(409, result.get("code"));
        }
    }

    @Nested
    @DisplayName("Logs")
    class LogTests {
        @Test
        @DisplayName("getLogs should return sorted list")
        void getLogsSorted() {
            List<LogEntry> logs = service.getLogs(null, null);
            assertNotNull(logs);
        }
    }

    @Nested
    @DisplayName("Workload")
    class WorkloadTests {
        @Test
        @DisplayName("Should compute workload from offer-status applications")
        void shouldComputeWorkload() {
            List<JsonObject> workload = service.getWorkload(null, null);
            assertNotNull(workload);
            assertTrue(workload.size() > 0);
        }

        @Test
        @DisplayName("Workload entry should include status")
        void shouldIncludeStatus() {
            List<JsonObject> workload = service.getWorkload(null, null);
            for (JsonObject w : workload) {
                assertTrue(w.has("status"));
                assertTrue(w.has("totalHours"));
                assertTrue(w.has("maxHours"));
                assertTrue(w.has("taName"));
            }
        }
    }

    @Nested
    @DisplayName("Data Editor")
    class DataEditorTests {
        @Test
        @DisplayName("getCollectionNames should return all collections")
        void getCollectionNames() {
            JsonArray names = service.getCollectionNames();
            assertTrue(names.size() >= 10);
        }

        @Test
        @DisplayName("getCollectionData for users should return users")
        void getCollectionDataUsers() {
            JsonElement data = service.getCollectionData("users");
            assertTrue(data.isJsonArray());
            assertEquals(6, data.getAsJsonArray().size());
        }

        @Test
        @DisplayName("getCollectionData for unknown collection should return empty")
        void unknownCollectionShouldReturnEmpty() {
            JsonElement data = service.getCollectionData("unknown");
            assertTrue(data.isJsonArray());
            assertEquals(0, data.getAsJsonArray().size());
        }

        @Test
        @DisplayName("deleteFromCollection should work for valid record")
        void deleteShouldWork() {
            Map<String, Object> result = service.deleteFromCollection("departments", "d001");
            assertTrue((Boolean) result.get("success"));
        }

        @Test
        @DisplayName("deleteFromCollection for non-existent should fail")
        void deleteNonExistent() {
            Map<String, Object> result = service.deleteFromCollection("departments", "d999");
            assertEquals(404, result.get("code"));
        }
    }

    @Nested
    @DisplayName("Workload Reassignment")
    class ReassignTests {
        @Test
        @DisplayName("Reopen action should revoke offer and reopen job")
        void reopenShouldWork() {
            JsonObject body = new JsonObject();
            body.addProperty("action", "reopen");
            body.addProperty("applicationId", "a002");

            Map<String, Object> result = service.reassignWorkload("u001", body);
            assertTrue((Boolean) result.get("success"));

            Application app = dataStore.getApplications().stream()
                    .filter(a -> "a002".equals(a.getId())).findFirst().orElse(null);
            assertEquals("rejected", app.getStatus());

            Job job = dataStore.getJobs().stream()
                    .filter(j -> "j003".equals(j.getId())).findFirst().orElse(null);
            assertEquals("active", job.getStatus());
        }

        @Test
        @DisplayName("Transfer to valid applicant should succeed")
        void transferShouldWork() {
            JsonObject body = new JsonObject();
            body.addProperty("action", "transfer");
            body.addProperty("applicationId", "a002");
            body.addProperty("targetUserId", "u002");

            Map<String, Object> result = service.reassignWorkload("u001", body);
            assertTrue((Boolean) result.get("success"));
        }

        @Test
        @DisplayName("Transfer to self should fail")
        void transferToSelfShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("action", "transfer");
            body.addProperty("applicationId", "a002");
            body.addProperty("targetUserId", "u001");

            Map<String, Object> result = service.reassignWorkload("u001", body);
            assertEquals(400, result.get("code"));
        }

        @Test
        @DisplayName("Invalid action should fail")
        void invalidActionShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("action", "invalid");
            body.addProperty("applicationId", "a002");

            Map<String, Object> result = service.reassignWorkload("u001", body);
            assertEquals(400, result.get("code"));
        }

        @Test
        @DisplayName("Missing applicationId should fail")
        void missingAppIdShouldFail() {
            JsonObject body = new JsonObject();
            body.addProperty("action", "reopen");

            Map<String, Object> result = service.reassignWorkload("u001", body);
            assertEquals(400, result.get("code"));
        }
    }
}
