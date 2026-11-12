package com.tamo.servlet;
import com.tamo.service.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthServletLoginTest {
 @Test void loginUsesStoredAccountRoleEvenWhenClientSuppliesDifferentRole() throws Exception {
  var data=JsonDataService.detached(TestDataFactory.createTestDataStore());var servlet=new AuthServlet();
  for(String name:new String[]{"authService","dataService"}){var field=AuthServlet.class.getDeclaredField(name);field.setAccessible(true);field.set(servlet,name.equals("authService")?new AuthService(data):data);}
  var req=mock(HttpServletRequest.class);var resp=mock(HttpServletResponse.class);var session=mock(HttpSession.class);
  when(req.getPathInfo()).thenReturn("/login");when(req.getSession(true)).thenReturn(session);
  when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{\"email\":\"alan@university.edu\",\"password\":\"pass5\",\"role\":\"admin\"}")));
  var out=new StringWriter();when(resp.getWriter()).thenReturn(new PrintWriter(out));servlet.doPost(req,resp);
  verify(resp,never()).setStatus(403);verify(session).setAttribute("role","organiser");assertTrue(out.toString().contains("\"role\": \"organiser\""));assertFalse(out.toString().contains("pass5"));
 }
}
