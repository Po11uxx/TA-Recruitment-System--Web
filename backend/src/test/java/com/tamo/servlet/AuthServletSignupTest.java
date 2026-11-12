package com.tamo.servlet;

import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AuthServletSignupTest {
    @Test void publicSignupCannotCreateStaffEvenWithForgedRole() throws Exception {
        for (String role : new String[]{"admin", "organiser", "reviewer", "decision_maker", "viewer"}) {
            HttpServletRequest req = mock(HttpServletRequest.class);
            HttpServletResponse resp = mock(HttpServletResponse.class);
            when(req.getPathInfo()).thenReturn("/signup");
            when(req.getReader()).thenReturn(new BufferedReader(new StringReader(
                    "{\"name\":\"Test\",\"email\":\"test@example.test\",\"password\":\"test\",\"role\":\"" + role + "\"}")));
            StringWriter response = new StringWriter();
            when(resp.getWriter()).thenReturn(new PrintWriter(response));
            new AuthServlet().doPost(req, resp);
            verify(resp).setStatus(403);
            verify(req, never()).getSession(anyBoolean());
            assertTrue(response.toString().contains("administrator"));
        }
    }
}
