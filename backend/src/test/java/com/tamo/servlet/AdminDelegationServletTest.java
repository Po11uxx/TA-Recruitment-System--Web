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
    @Test void globalAdminPermissionsCannotBeChangedByCourseGrants() throws Exception {
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
        verify(update(servlet,"u010",grant)).setStatus(403);
        assertNull(delegate.getPermissions());

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
