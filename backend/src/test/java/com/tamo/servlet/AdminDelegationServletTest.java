package com.tamo.servlet;

import com.tamo.model.User;
import com.tamo.service.JsonDataService;
import com.tamo.service.TestDataFactory;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDelegationServletTest {
    @Test void anotherAdminMustProvideCourseAndReasonAndCanRevoke() throws Exception {
        var data = TestDataFactory.createTestDataStore();
        User delegate = new User();
        delegate.setId("delegate");
        delegate.setRole("admin");
        delegate.setStatus("active");
        data.getUsers().add(delegate);

        var servlet = new ReviewServlet();
        var field = ReviewServlet.class.getDeclaredField("data");
        field.setAccessible(true);
        field.set(servlet, JsonDataService.detached(data));

        String grant = """
                {"permissions":["assignment.manage"],"courseIds":["m001"],"reason":"Teacher on leave"}
                """;
        verify(update(servlet,"delegate",grant)).setStatus(403);
        verify(update(servlet,"u010",grant.replace("[\"m001\"]","[]"))).setStatus(400);
        verify(update(servlet,"u010",grant.replace("Teacher on leave",""))).setStatus(400);
        assertNull(delegate.getPermissions());

        update(servlet,"u010",grant);
        assertEquals(List.of("assignment.manage"),delegate.getPermissions());
        assertEquals(List.of("m001"),delegate.getCourseIds());
        assertTrue(data.getLogs().stream().anyMatch(log -> "u010".equals(log.getActorId())
                && log.getDetails().contains("Teacher on leave")));

        update(servlet,"u010","""
                {"permissions":[],"courseIds":[],"reason":"Teacher returned"}
                """);
        assertTrue(delegate.getPermissions().isEmpty());
        assertTrue(delegate.getCourseIds().isEmpty());
    }

    private HttpServletResponse update(ReviewServlet servlet,String actor,String body) throws Exception {
        var req = mock(HttpServletRequest.class);
        var session = mock(HttpSession.class);
        var resp = mock(HttpServletResponse.class);
        when(req.getSession(false)).thenReturn(session);
        when(req.getSession()).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(actor);
        when(req.getServletPath()).thenReturn("/api/admin/review-access");
        when(req.getPathInfo()).thenReturn("/delegate");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
        when(resp.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        servlet.doPut(req,resp);
        return resp;
    }
}
