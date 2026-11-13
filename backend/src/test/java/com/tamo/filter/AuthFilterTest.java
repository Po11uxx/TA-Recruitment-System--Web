package com.tamo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@DisplayName("AuthFilter — Authentication & Authorization Tests")
@ExtendWith(MockitoExtension.class)
class AuthFilterTest {

    private AuthFilter filter;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;
    @Mock private HttpSession session;

    @BeforeEach
    void setUp() {
        filter = new AuthFilter();
    }

    @Test
    void signedOutStudentFollowsOfferLinkThroughLogin() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/applicant/assignments.jsp");
        when(request.getContextPath()).thenReturn("/tamo");
        when(request.getParameter("assignmentId")).thenReturn("assignment_email");
        filter.doFilter(request,response,chain);
        verify(response).sendRedirect("/tamo/auth/login.jsp?placementId=assignment_email");
        verifyNoInteractions(chain);
    }

    @Test
    void applicantCannotAccessCsvEndpoints() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u001");
        when(session.getAttribute("role")).thenReturn("applicant");
        when(request.getRequestURI()).thenReturn("/tamo/api/csv/export");
        stubWriter();
        filter.doFilter(request, response, chain);
        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void applicantCannotOpenStaffPageDirectly() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u001");
        when(session.getAttribute("role")).thenReturn("applicant");
        when(request.getRequestURI()).thenReturn("/tamo/admin/assignments.jsp");
        stubWriter();
        filter.doFilter(request, response, chain);
        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "applicant,/tamo/applicant/assignments.jsp,true",
        "applicant,/tamo/applicant/csv.jsp,false",
        "applicant,/tamo/organiser/assignments.jsp,false",
        "applicant,/tamo/review/dashboard.jsp,false",
        "applicant,/tamo/common/csv-content.jsp,false",
        "organiser,/tamo/organiser/csv.jsp,false",
        "admin,/tamo/admin/assignments.jsp,true",
        "organiser,/tamo/api/csv/options,false",
        "reviewer,/tamo/api/csv/export,false",
        "decision_maker,/tamo/api/csv/import,false",
        "viewer,/tamo/api/csv/preview,false",
        "admin,/tamo/admin/csv.jsp,true",
        "admin,/tamo/api/csv/options,true",
        "reviewer,/tamo/review/dashboard.jsp,true",
        "viewer,/tamo/admin/dashboard.jsp,false",
        "admin,/tamo/applicant/assignments.jsp,false"
    })
    void pageAccessMatchesRole(String role, String path, boolean allowed) throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u001");
        when(session.getAttribute("role")).thenReturn(role);
        when(request.getRequestURI()).thenReturn(path);
        if (!allowed) stubWriter();
        filter.doFilter(request, response, chain);
        if (allowed) verify(chain).doFilter(request, response);
        else {
            verify(response).setStatus(403);
            verify(chain, never()).doFilter(request, response);
        }
    }

    private void stubWriter() throws Exception {
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
    }

    @Test
    @DisplayName("OPTIONS requests should pass through without auth check")
    void optionsShouldPassThrough() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Request without session should return 401")
    void noSessionShouldReturn401() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(null);
        stubWriter();

        filter.doFilter(request, response, chain);

        verify(response).setStatus(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Session without userId should return 401")
    void sessionWithoutUserIdShouldReturn401() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(null);
        stubWriter();

        filter.doFilter(request, response, chain);

        verify(response).setStatus(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Applicant accessing /api/applicant should pass")
    void applicantAccessOwnApiShouldPass() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u001");
        when(session.getAttribute("role")).thenReturn("applicant");
        when(request.getRequestURI()).thenReturn("/tamo/api/applicant/dashboard");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Applicant accessing /api/organiser should return 403")
    void applicantAccessOrganiserApiShouldReturn403() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u001");
        when(session.getAttribute("role")).thenReturn("applicant");
        when(request.getRequestURI()).thenReturn("/tamo/api/organiser/notifications");
        stubWriter();

        filter.doFilter(request, response, chain);

        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Applicant accessing /api/admin should return 403")
    void applicantAccessAdminApiShouldReturn403() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u001");
        when(session.getAttribute("role")).thenReturn("applicant");
        when(request.getRequestURI()).thenReturn("/tamo/api/admin/users");
        stubWriter();

        filter.doFilter(request, response, chain);

        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Organiser accessing /api/organiser should pass")
    void organiserAccessOwnApiShouldPass() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u005");
        when(session.getAttribute("role")).thenReturn("organiser");
        when(request.getRequestURI()).thenReturn("/tamo/api/organiser/notifications");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings={"/api/organiser", "/api/admin/postings"})
    void teacherCannotPublishPostings(String servletPath) throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u005");
        when(session.getAttribute("role")).thenReturn("organiser");
        when(request.getServletPath()).thenReturn(servletPath);
        if ("/api/organiser".equals(servletPath)) when(request.getPathInfo()).thenReturn("/jobs");
        stubWriter();
        filter.doFilter(request,response,chain);
        verify(response).setStatus(403);
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("Admin accessing /api/admin should pass")
    void adminAccessOwnApiShouldPass() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u010");
        when(session.getAttribute("role")).thenReturn("admin");
        when(request.getRequestURI()).thenReturn("/tamo/api/admin/dashboard");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Organiser accessing /api/applicant should return 403")
    void organiserAccessApplicantApiShouldReturn403() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u005");
        when(session.getAttribute("role")).thenReturn("organiser");
        when(request.getRequestURI()).thenReturn("/tamo/api/applicant/profile");
        stubWriter();

        filter.doFilter(request, response, chain);

        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Admin accessing /api/applicant should return 403")
    void adminAccessApplicantApiShouldReturn403() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn("u010");
        when(session.getAttribute("role")).thenReturn("admin");
        when(request.getRequestURI()).thenReturn("/tamo/api/applicant/jobs");
        stubWriter();

        filter.doFilter(request, response, chain);

        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }
}
