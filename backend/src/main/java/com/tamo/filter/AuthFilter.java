package com.tamo.filter;

import com.tamo.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Implementation of AuthFilter.
 * Provides necessary operations and logic for the system.
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String placementId = req.getParameter("assignmentId");
            if ("GET".equals(req.getMethod()) && "/applicant/assignments.jsp".equals(req.getServletPath())
                    && placementId != null && placementId.matches("[A-Za-z0-9_-]{1,200}")) {
                resp.sendRedirect(req.getContextPath() + "/auth/login.jsp?placementId=" + placementId);
                return;
            }
            JsonUtil.writeError(resp, 401, "Not logged in");
            return;
        }

        String role = (String) session.getAttribute("role");
        try {
            var ds=com.tamo.service.JsonDataService.getInstance().getData();
            if(ds!=null&&ds.getUsers()!=null){var user=ds.getUsers().stream().filter(u->java.util.Objects.equals(session.getAttribute("userId"),u.getId())).findFirst().orElse(null);
                if(user==null||!"active".equals(user.getStatus())){JsonUtil.writeError(resp,403,"Account access is no longer active.");return;}role=user.getRole();session.setAttribute("role",role);
            }
        }catch(IllegalStateException unavailable){/* DatabaseTransactionFilter rejects unavailable database requests first. */}
        String path = req.getServletPath();
        if (path == null || path.isEmpty()) {
            path = req.getRequestURI();
            String context = req.getContextPath();
            if(context!=null&&path.startsWith(context)) path=path.substring(context.length());
        }

        if ((path.contains("/api/csv/") || path.endsWith("/csv.jsp")) && !"admin".equals(role)) {
            JsonUtil.writeError(resp, 403, "CSV import and export are available to active admin accounts only.");
            return;
        }

        if ((path.contains("/api/applicant") || path.contains("/applicant/")) && !"applicant".equals(role)) {
            JsonUtil.writeError(resp, 403, "Access denied");
            return;
        }
        if ((path.contains("/api/organiser") || path.contains("/organiser/")) && !"organiser".equals(role)) {
            JsonUtil.writeError(resp, 403, "Access denied");
            return;
        }
        if ("organiser".equals(role)) {
            if (path.startsWith("/organiser/")) {
                resp.sendRedirect(req.getContextPath()+"/review/dashboard.jsp");
                return;
            }
            if (path.startsWith("/api/organiser")) {
                String operation = "/api/organiser".equals(path) ? req.getPathInfo()
                        : path.substring("/api/organiser".length());
                if (operation==null || !operation.matches("/notifications(?:/.*)?")) {
                    JsonUtil.writeError(resp,403,"Use Review Workspace to view applications and record decisions.");
                    return;
                }
            }
        }
        if ((path.contains("/api/admin") || path.contains("/admin/")) && !"admin".equals(role)) {
            JsonUtil.writeError(resp, 403, "Access denied");
            return;
        }

        if (path.contains("/common/")
                || (path.contains("/review/") && "applicant".equals(role))
                || ((path.contains("/api/csv/") || path.endsWith("/applicant/csv.jsp"))
                    && "applicant".equals(role))) {
            JsonUtil.writeError(resp, 403, "This page or operation is available to authorised staff only.");
            return;
        }

        if(path.contains("/api/messages")&&java.util.Set.of("reviewer","decision_maker","viewer").contains(role)){JsonUtil.writeError(resp,403,"Messaging is not included in this review role.");return;}
        chain.doFilter(request, response);
    }

    @Override
    /**
     * Executes the init operation.
     * @param filterConfig the filterConfig argument
     */
    /**
     * Executes the init operation.
     * @param filterConfig the filterConfig argument
     */
    public void init(FilterConfig filterConfig) {}

    @Override
    /**
     * Executes the destroy operation.
     */
    /**
     * Executes the destroy operation.
     */
    public void destroy() {}
}
