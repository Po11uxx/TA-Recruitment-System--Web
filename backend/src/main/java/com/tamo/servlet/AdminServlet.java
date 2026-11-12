package com.tamo.servlet;

import com.google.gson.JsonObject;
import com.tamo.model.Module;
import com.tamo.service.AdminService;
import com.tamo.service.JsonDataService;
import com.tamo.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Implementation of AdminServlet.
 * Provides necessary operations and logic for the system.
 */
public class AdminServlet extends HttpServlet {

    private AdminService service;
    private JsonDataService dataService;

    private boolean readOnlyCollection(String collection, HttpServletResponse resp) throws IOException {
        if (!java.util.Set.of("modules","jobs","applications","workloadAssignments","logs").contains(collection)) return false;
        JsonUtil.writeError(resp,403,"This collection is read-only in Data Editor.");
        return true;
    }

    @Override
    /**
     * Executes the init operation.
     */
    /**
     * Executes the init operation.
     */
    public void init() throws ServletException {
        this.dataService = JsonDataService.getInstance();
        this.service = new AdminService(dataService);
    }

    private String getUserId(HttpServletRequest req) {
        return (String) req.getSession().getAttribute("userId");
    }

    private String getUserName(HttpServletRequest req) {
        return (String) req.getSession().getAttribute("userName");
    }

    @Override
    /**
     * Executes the doGet operation.
     * @param req the req argument
     * @param resp the resp argument
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = JsonUtil.getPathInfo(req);
        if ("data-editor/interviews".equals(path) || path.startsWith("data-editor/interviews/")) {
            JsonUtil.writeError(resp, 410, "Interview management has been removed."); return;
        }


        switch (path) {
            case "dashboard" -> JsonUtil.writeJson(resp, service.getDashboard());

            case "users" -> {
                String keyword = req.getParameter("keyword");
                String role = req.getParameter("role");
                String status = req.getParameter("status");
                JsonUtil.writeJson(resp, service.getUsers(keyword, role, status));
            }

            case "modules" -> {
                String keyword = req.getParameter("keyword");
                JsonUtil.writeJson(resp, service.getModules(keyword));
            }

            case "properties/terms" -> JsonUtil.writeJson(resp, service.getTerms());

            case "properties/departments" -> JsonUtil.writeJson(resp, service.getDepartments());

            case "logs" -> {
                String keyword = req.getParameter("keyword");
                String type = req.getParameter("type");
                JsonUtil.writeJson(resp, service.getLogs(keyword, type));
            }

            case "workload" -> {
                String keyword = req.getParameter("keyword");
                String status = req.getParameter("status");
                JsonUtil.writeJson(resp, service.getWorkload(keyword, status,req.getParameter("periodId")));
            }

            case "data-editor/collections" -> JsonUtil.writeJson(resp, service.getCollectionNames());

            default -> {
                if (path.startsWith("data-editor/")) {
                    String collection = path.substring("data-editor/".length());
                    JsonUtil.writeJson(resp, service.getCollectionData(collection));
                } else {
                    JsonUtil.writeError(resp, 404, "Endpoint not found");
                }
            }
        }
    }

    @Override
    /**
     * Executes the doPost operation.
     * @param req the req argument
     * @param resp the resp argument
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = JsonUtil.getPathInfo(req);
        if ("data-editor/users".equals(path) || path.startsWith("data-editor/users/")) {
            JsonUtil.writeError(resp, 403, "Manage accounts through Users, not Data Editor."); return;
        }

        if ("data-editor/interviews".equals(path) || path.startsWith("data-editor/interviews/")) {
            JsonUtil.writeError(resp, 410, "Interview management has been removed."); return;
        }

        JsonObject body = JsonUtil.readRequestBodyAsJson(req);
        String actor = getUserName(req);
        String actorId = getUserId(req);
        String ip = req.getRemoteAddr();

        switch (path) {
            case "users" -> {
                Map<String, Object> result = service.createUser(body);
                if (result.containsKey("error")) {
                    int code = (int) result.get("code");
                    JsonUtil.writeError(resp, code, (String) result.get("error"));
                } else {
                    resp.setStatus(201);
                    JsonUtil.writeJson(resp, result.get("user"));
                    String newName = body.has("name") ? body.get("name").getAsString() : "";
                    dataService.addLog("User Created", "success", actor, actorId,
                            "Created user: " + newName, ip);
                }
            }

            case "modules" -> {
                Map<String, Object> result = service.createModule(body);
                if (result.containsKey("error")) {
                    int code = (int) result.get("code_http");
                    JsonUtil.writeError(resp, code, (String) result.get("error"));
                } else {
                    resp.setStatus(201);
                    JsonUtil.writeJson(resp, result.get("module"));
                    String modCode = body.has("code") ? body.get("code").getAsString() : "";
                    String modName = body.has("name") ? body.get("name").getAsString() : "";
                    dataService.addLog("Module Created", "success", actor, actorId,
                            "Created module: " + modCode + " - " + modName, ip);
                }
            }

            case "properties/terms" -> {
                Map<String, Object> result = service.createTerm(body);
                if (result.containsKey("error")) {
                    int code = (int) result.get("code");
                    JsonUtil.writeError(resp, code, (String) result.get("error"));
                } else {
                    resp.setStatus(201);
                    JsonUtil.writeJson(resp, result.get("term"));
                    String termName = body.has("name") ? body.get("name").getAsString() : "";
                    dataService.addLog("Term Created", "success", actor, actorId,
                            "Created term: " + termName, ip);
                }
            }

            case "properties/departments" -> {
                Map<String, Object> result = service.createDepartment(body);
                if (result.containsKey("error")) {
                    int code = (int) result.get("code");
                    JsonUtil.writeError(resp, code, (String) result.get("error"));
                } else {
                    resp.setStatus(201);
                    JsonUtil.writeJson(resp, result.get("department"));
                    String deptName = body.has("name") ? body.get("name").getAsString() : "";
                    dataService.addLog("Department Created", "success", actor, actorId,
                            "Created department: " + deptName, ip);
                }
            }

            default -> {
                if (path.startsWith("data-editor/")) {
                    String collection = path.substring("data-editor/".length());
                    if (readOnlyCollection(collection, resp)) return;
                    Map<String, Object> result = service.addToCollection(collection, body);
                    if (result.containsKey("error")) {
                        JsonUtil.writeError(resp, (int) result.get("code"), (String) result.get("error"));
                    } else {
                        resp.setStatus(201);
                        JsonUtil.writeJson(resp, result.get("item"));
                        dataService.addLog("Data Created", "info", actor, actorId,
                                "Added item to " + collection, ip);
                    }
                } else {
                    JsonUtil.writeError(resp, 404, "Endpoint not found");
                }
            }
        }
    }

    @Override
    /**
     * Executes the doPut operation.
     * @param req the req argument
     * @param resp the resp argument
     */
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = JsonUtil.getPathInfo(req);
        if ("data-editor/users".equals(path) || path.startsWith("data-editor/users/")) {
            JsonUtil.writeError(resp, 403, "Manage accounts through Users, not Data Editor."); return;
        }

        if ("data-editor/interviews".equals(path) || path.startsWith("data-editor/interviews/")) {
            JsonUtil.writeError(resp, 410, "Interview management has been removed."); return;
        }

        String adminId = getUserId(req);
        String actor = getUserName(req);
        String ip = req.getRemoteAddr();
        JsonObject body = JsonUtil.readRequestBodyAsJson(req);

        if (path.matches("users/[\\w]+")) {
            String userId = path.split("/")[1];
            Map<String, Object> result = service.updateUser(adminId, userId, body);
            if (result.containsKey("error")) {
                int code = (int) result.get("code");
                JsonUtil.writeError(resp, code, (String) result.get("error"));
            } else {
                JsonUtil.writeJson(resp, result.get("user"));
                dataService.addLog("User Updated", "info", actor, adminId,
                        "Updated user " + userId, ip);
            }

        } else if (path.matches("modules/[\\w]+")) {
            String moduleId = path.split("/")[1];
            Map<String, Object> result = service.updateModule(moduleId, body);
            if (result.containsKey("error")) {
                int code = (int) result.get("code_http");
                JsonUtil.writeError(resp, code, (String) result.get("error"));
            } else {
                Module module = (Module) result.get("module");
                JsonUtil.writeJson(resp, module);
                dataService.addLog("Module Updated", "info", actor, adminId,
                        "Updated module " + moduleId + " (" + module.getCode() + ")", ip);
            }

        } else if (path.matches("workload/[\\w]+/reassign")) {
            JsonUtil.writeError(resp,403,"Use TA Assignments to manage course placements.");

        } else if (path.matches("data-editor/[\\w]+/[\\w-]+")) {
            String[] segments = path.split("/");
            String collection = segments[1];
            String itemId = segments[2];
            if (readOnlyCollection(collection, resp)) return;
            Map<String, Object> result = service.updateCollectionItem(collection, itemId, body);
            if (result.containsKey("error")) {
                int code = (int) result.get("code");
                JsonUtil.writeError(resp, code, (String) result.get("error"));
            } else {
                JsonUtil.writeJson(resp, result.get("item"));
                dataService.addLog("Data Updated", "info", actor, adminId,
                        "Updated " + collection + " item " + itemId, ip);
            }

        } else {
            JsonUtil.writeError(resp, 404, "Endpoint not found");
        }
    }

    @Override
    /**
     * Executes the doDelete operation.
     * @param req the req argument
     * @param resp the resp argument
     */
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = JsonUtil.getPathInfo(req);
        if ("data-editor/users".equals(path) || path.startsWith("data-editor/users/")) {
            JsonUtil.writeError(resp, 403, "Manage accounts through Users, not Data Editor."); return;
        }

        if ("data-editor/interviews".equals(path) || path.startsWith("data-editor/interviews/")) {
            JsonUtil.writeError(resp, 410, "Interview management has been removed."); return;
        }

        String adminId = getUserId(req);
        String actor = getUserName(req);
        String ip = req.getRemoteAddr();

        if (path.matches("users/[\\w]+")) {
            String userId = path.split("/")[1];
            Map<String, Object> result = service.deleteUser(adminId, userId);
            if (result.containsKey("error")) {
                int code = (int) result.get("code");
                JsonUtil.writeError(resp, code, (String) result.get("error"));
            } else {
                JsonObject ok = new JsonObject();
                ok.addProperty("message", "User deleted successfully");
                JsonUtil.writeJson(resp, ok);
                dataService.addLog("User Deleted", "warning", actor, adminId,
                        "Deleted user " + userId, ip);
            }

        } else if (path.matches("modules/[\\w]+")) {
            String moduleId = path.split("/")[1];
            Map<String, Object> result = service.deleteModule(moduleId);
            if (result.containsKey("error")) {
                int code = (int) result.get("code");
                JsonUtil.writeError(resp, code, (String) result.get("error"));
            } else {
                JsonObject ok = new JsonObject();
                ok.addProperty("message", "Module deleted successfully");
                JsonUtil.writeJson(resp, ok);
                dataService.addLog("Module Deleted", "warning", actor, adminId,
                        "Deleted module " + moduleId, ip);
            }

        } else if (path.matches("data-editor/[\\w]+/[\\w-]+")) {
            String[] segments = path.split("/");
            String collection = segments[1];
            String itemId = segments[2];
            if (readOnlyCollection(collection, resp)) return;
            Map<String, Object> result = service.deleteFromCollection(collection, itemId);
            if (result.containsKey("error")) {
                int code = (int) result.get("code");
                JsonUtil.writeError(resp, code, (String) result.get("error"));
            } else {
                JsonObject ok = new JsonObject();
                ok.addProperty("message", "Record deleted successfully");
                JsonUtil.writeJson(resp, ok);
                dataService.addLog("Data Deleted", "warning", actor, adminId,
                        "Deleted " + collection + " item " + itemId, ip);
            }

        } else {
            JsonUtil.writeError(resp, 404, "Endpoint not found");
        }
    }
}
