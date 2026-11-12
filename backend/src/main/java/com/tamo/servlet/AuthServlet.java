package com.tamo.servlet;

import com.google.gson.JsonObject;
import com.tamo.model.User;
import com.tamo.service.AuthService;
import com.tamo.service.EmailService;
import com.tamo.service.JsonDataService;
import com.tamo.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Implementation of AuthServlet.
 * Provides necessary operations and logic for the system.
 */
public class AuthServlet extends HttpServlet {

    private AuthService authService;
    private JsonDataService dataService;

    @Override
    /**
     * Executes the init operation.
     */
    /**
     * Executes the init operation.
     */
    public void init() throws ServletException {
        this.dataService = JsonDataService.getInstance(
                JsonDataService.resolveClassesPath(getServletContext()));
        this.authService = new AuthService(dataService);

        String imagesPath = getServletContext().getRealPath("/static/images");
        EmailService.getInstance().setImagesBasePath(imagesPath);
    }

    @Override
    /**
     * Executes the doPost operation.
     * @param req the req argument
     * @param resp the resp argument
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = JsonUtil.getPathInfo(req);

        switch (path) {
            case "login" -> handleLogin(req, resp);
            case "signup" -> handleSignup(req, resp);
            case "logout" -> handleLogout(req, resp);
            default -> JsonUtil.writeError(resp, 404, "Port is not found");
        }
    }

    @Override
    /**
     * Executes the doGet operation.
     * @param req the req argument
     * @param resp the resp argument
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = JsonUtil.getPathInfo(req);

        if ("me".equals(path)) {
            handleMe(req, resp);
        } else {
            JsonUtil.writeError(resp, 404, "Port is not found");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = JsonUtil.readRequestBodyAsJson(req);
        String email = body.has("email") ? body.get("email").getAsString() : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            JsonUtil.writeError(resp, 400, "Missing required fields");
            return;
        }

        User user = authService.findByEmailAndPassword(email, password);
        if (user == null) {
            JsonUtil.writeError(resp, 401, "Invalid email or password");
            return;
        }

        if (!"active".equals(user.getStatus())) {
            JsonUtil.writeError(resp, 403, "Account has been suspended");
            return;
        }
   

        authService.recordLogin(user);

        HttpSession session = req.getSession(true);
        req.changeSessionId();
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole());
        session.setAttribute("userName", user.getName());

        String ip = req.getRemoteAddr();
        dataService.addLog("User Login", "info", user.getName(), user.getId(),
                "User " + user.getName() + " (" + user.getRole() + ") logged in", ip);

        JsonObject safeUser = buildSafeUser(user);
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.add("user", safeUser);
        JsonUtil.writeJson(resp, result);
    }

    private void handleSignup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = JsonUtil.readRequestBodyAsJson(req);
        String name = body.has("name") ? body.get("name").getAsString() : null;
        String email = body.has("email") ? body.get("email").getAsString() : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;
        String role = body.has("role") ? body.get("role").getAsString() : "applicant";

        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            JsonUtil.writeError(resp, 400, "Missing required fields");
            return;
        }

        if (!AuthService.isValidEmail(email)) {
            JsonUtil.writeError(resp, 400, "Invalid email format");
            return;
        }

        if (!AuthService.isValidRole(role)) {
            JsonUtil.writeError(resp, 400, "Invalid role value");
            return;
        }

        if (!"applicant".equals(role)) {
            JsonUtil.writeError(resp, 403, "Staff accounts must be created by an administrator.");
            return;
        }

        User user = authService.signup(name, email, password, role);
        if (user == null) {
            JsonUtil.writeError(resp, 409, "Email is already registered");
            return;
        }
   

        HttpSession session = req.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole());
        session.setAttribute("userName", user.getName());

        String ip = req.getRemoteAddr();
        dataService.addLog("User Signup", "success", user.getName(), user.getId(),
                "New " + user.getRole() + " account registered: " + user.getEmail(), ip);

        resp.setStatus(201);
        JsonObject safeUser = buildSafeUser(user);
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.add("user", safeUser);
        JsonUtil.writeJson(resp, result);
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        String userName = null;
        String userId = null;
        if (session != null) {
            userName = (String) session.getAttribute("userName");
            userId = (String) session.getAttribute("userId");
            session.invalidate();
        }
        String ip = req.getRemoteAddr();
        dataService.addLog("User Logout", "info", userName, userId,
                "User " + (userName != null ? userName : "unknown") + " logged out", ip);

        JsonObject result = new JsonObject();
        result.addProperty("message", "Logout successfully");
        JsonUtil.writeJson(resp, result);
    }

    private void handleMe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.writeError(resp, 401, "Not logged in");
            return;
        }

        String userId = (String) session.getAttribute("userId");
        User user = authService.getUserById(userId);
        if (user == null) {
            JsonUtil.writeError(resp, 404, "User not found");
            return;
        }

        JsonUtil.writeJson(resp, buildSafeUser(user));
    }

    /**
     * Build user info without password
     */
    private JsonObject buildSafeUser(User user) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", user.getId());
        obj.addProperty("name", user.getName());
        obj.addProperty("email", user.getEmail());
        obj.addProperty("role", user.getRole());
        obj.addProperty("department", user.getDepartment());
        obj.addProperty("avatar", user.getAvatar());
        obj.addProperty("status", user.getStatus());
        obj.addProperty("lastLogin", user.getLastLogin());
        obj.addProperty("createdAt", user.getCreatedAt());
        return obj;
    }
}
