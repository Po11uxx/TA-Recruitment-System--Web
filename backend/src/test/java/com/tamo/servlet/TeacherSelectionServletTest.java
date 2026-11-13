package com.tamo.servlet;

import com.google.gson.JsonParser;
import com.tamo.service.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeacherSelectionServletTest {
    @Test void directSelectionRouteUsesAuthenticatedActorAndPropagatesValidation() throws Exception {
        var data=JsonDataService.detached(TestDataFactory.createTestDataStore());
        var servlet=new ReviewServlet();var field=ReviewServlet.class.getDeclaredField("data");field.setAccessible(true);field.set(servlet,data);
        var req=mock(HttpServletRequest.class);var session=mock(HttpSession.class);var resp=mock(HttpServletResponse.class);
        when(req.getSession(false)).thenReturn(session);when(session.getAttribute("userId")).thenReturn("u010");when(req.getPathInfo()).thenReturn("/a001/select-ta");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{\"sectionId\":\"missing\",\"note\":\"Reviewed qualifications for course\",\"userId\":\"u005\"}")));
        var out=new StringWriter();when(resp.getWriter()).thenReturn(new PrintWriter(out));servlet.doPost(req,resp);
        verify(resp).setStatus(400);assertTrue(JsonParser.parseString(out.toString()).getAsJsonObject().has("error"));assertTrue(data.getData().getReviewRecords().isEmpty());assertTrue(data.getData().getSectionAssignments().isEmpty());
    }
    @Test void missingSessionIs401() throws Exception {
        var servlet=new ReviewServlet();var req=mock(HttpServletRequest.class);var resp=mock(HttpServletResponse.class);when(resp.getWriter()).thenReturn(new PrintWriter(new StringWriter()));servlet.doPost(req,resp);verify(resp).setStatus(401);
    }
    @Test void successfulPostReturnsDraftAndUsesSessionActor() throws Exception {
        var ds=TestDataFactory.createTestDataStore();
        var section=new com.tamo.model.CourseSection();section.setId("direct_http");section.setModuleId("m001");section.setPeriodId("period_test");section.setName("HTTP lab");section.setStatus("active");section.setTaPositions(1);section.setTimeZone("America/Los_Angeles");section.setMeetingTimes(java.util.List.of());ds.getCourseSections().add(section);
        var servlet=new ReviewServlet();var field=ReviewServlet.class.getDeclaredField("data");field.setAccessible(true);field.set(servlet,JsonDataService.detached(ds));
        var req=mock(HttpServletRequest.class);var session=mock(HttpSession.class);var resp=mock(HttpServletResponse.class);
        when(req.getSession(false)).thenReturn(session);when(session.getAttribute("userId")).thenReturn("u010");when(req.getPathInfo()).thenReturn("/a001/select-ta");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{\"sectionId\":\"direct_http\",\"note\":\"Reviewed qualifications for course\",\"confirmPending\":true,\"manualConfirmationNote\":\"Confirmed availability directly with applicant\",\"userId\":\"u005\"}")));
        var out=new StringWriter();when(resp.getWriter()).thenReturn(new PrintWriter(out));servlet.doPost(req,resp);
        var result=JsonParser.parseString(out.toString()).getAsJsonObject();assertFalse(result.has("error"),out.toString());assertEquals("draft",result.get("publicationStatus").getAsString());
        assertEquals("u010",ds.getReviewRecords().get(0).getActorId());assertEquals("u010",ds.getSectionAssignments().get(0).getAssignedBy());
    }
}
