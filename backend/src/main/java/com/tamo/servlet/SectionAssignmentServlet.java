package com.tamo.servlet;

import com.google.gson.JsonParseException;
import com.tamo.model.SectionAssignment;
import com.tamo.service.*;
import com.tamo.util.JsonUtil;
import jakarta.servlet.http.*;
import java.io.IOException;

public class SectionAssignmentServlet extends HttpServlet {
    private SectionAssignmentService service;
    private JsonDataService data;
    @Override public void init() { data=JsonDataService.getInstance(); service=new SectionAssignmentService(data); }
    private String user(HttpServletRequest req) { return (String)req.getSession().getAttribute("userId"); }
    private String role(HttpServletRequest req) { return (String)req.getSession().getAttribute("role"); }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        try {
            if (!JsonUtil.getPathInfo(req).isEmpty()) { JsonUtil.writeError(resp,404,"Endpoint not found"); return; }
            JsonUtil.writeJson(resp,service.list(user(req),role(req)));
        } catch (SectionAssignmentService.Problem e) { JsonUtil.writeError(resp,e.status,e.getMessage()); }
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        try {
            String path=JsonUtil.getPathInfo(req); SectionAssignment a; String action;
            if (path.isEmpty()) { a=service.assign(user(req),role(req),JsonUtil.readRequestBodyAsJson(req)); action="Created";resp.setStatus(201); }
            else if (path.matches("[\\w]+/publish")) { a=service.publish(user(req),role(req),path.split("/")[0],JsonUtil.readRequestBodyAsJson(req)); action="Published"; }
            else if (path.matches("[\\w]+/respond")) { a=service.respond(user(req),role(req),path.split("/")[0],JsonUtil.readRequestBodyAsJson(req)); action="Responded"; }
            else if (path.matches("[\\w]+/cancel")) { a=service.cancel(user(req),role(req),path.split("/")[0],JsonUtil.readRequestBodyAsJson(req)); action="Cancelled"; }
            else { JsonUtil.writeError(resp,404,"Endpoint not found"); return; }
            data.addLog("TA Assignment "+action,"info",(String)req.getSession().getAttribute("userName"),user(req),
                    action+" assignment "+a.getId()+" application "+a.getApplicationId()+" section "+a.getSectionId()+" period "+a.getPeriodId(),req.getRemoteAddr());
            JsonUtil.writeJson(resp,"applicant".equals(role(req))?service.studentView(a):service.staffView(a));
        } catch (SectionAssignmentService.Problem e) { JsonUtil.writeError(resp,e.status,e.getMessage()); }
        catch (JsonParseException e) { JsonUtil.writeError(resp,400,"Invalid JSON request."); }
    }
}
