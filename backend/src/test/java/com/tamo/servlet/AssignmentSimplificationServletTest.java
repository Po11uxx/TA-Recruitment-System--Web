package com.tamo.servlet;
import com.tamo.model.*;
import com.tamo.service.*;
import com.google.gson.JsonParser;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AssignmentSimplificationServletTest {
 @Test void cancellingLegacyAssignmentDoesNotReturnRetiredDeadlineFields() throws Exception {
  var ds=TestDataFactory.createTestDataStore();var section=new CourseSection();section.setId("legacy_section");section.setModuleId("m001");section.setName("Lab");ds.getCourseSections().add(section);
  var a=new SectionAssignment();a.setId("legacy_assignment");a.setApplicationId("a001");a.setApplicantId("u001");a.setSectionId(section.getId());a.setSectionSnapshot(section);a.setPeriodId("period_test");a.setStatus("active");a.setPublicationStatus("published");a.setResponseDeadline("2020-01-01T00:00:00Z");ds.getSectionAssignments().add(a);
  var data=JsonDataService.detached(ds);var servlet=new SectionAssignmentServlet();for(String name:new String[]{"service","data"}){var f=SectionAssignmentServlet.class.getDeclaredField(name);f.setAccessible(true);f.set(servlet,name.equals("service")?new SectionAssignmentService(data):data);}
  var req=mock(HttpServletRequest.class);var session=mock(HttpSession.class);var resp=mock(HttpServletResponse.class);when(req.getSession()).thenReturn(session);when(session.getAttribute("userId")).thenReturn("u010");when(session.getAttribute("role")).thenReturn("admin");when(req.getPathInfo()).thenReturn("/legacy_assignment/cancel");when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{\"reason\":\"Course staffing changed\"}")));var out=new StringWriter();when(resp.getWriter()).thenReturn(new PrintWriter(out));
  servlet.doPost(req,resp);var result=JsonParser.parseString(out.toString()).getAsJsonObject();assertFalse(result.has("error"),out.toString());assertEquals("cancelled",result.get("status").getAsString());assertFalse(result.has("responseDeadline"));assertFalse(result.has("expiredAt"));assertEquals("2020-01-01T00:00:00Z",a.getResponseDeadline());
 }
}
