package com.tamo.filter;

import com.tamo.service.JsonDataService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatabaseTransactionFilterTest {
    @Test void successfulBodyIsSentOnlyAfterCommit() throws Exception {
        runScenario(false, false, false);
    }

    @Test void commitFailureDoesNotSendSuccessfulBody() throws Exception {
        runScenario(true, false, false);
    }

    @Test void validationErrorRollsBackAndKeepsErrorBody() throws Exception {
        runScenario(false, true, false);
    }

    @Test void constraintFailureBecomesConflictWithoutSuccessfulBody() throws Exception {
        runScenario(true, false, true);
    }

    private void runScenario(boolean commitFails, boolean validationError, boolean constraint) throws Exception {
        JsonDataService data = mock(JsonDataService.class);
        when(data.requestLock()).thenReturn(new Object());
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getServletContext()).thenReturn(mock(ServletContext.class));
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getCharacterEncoding()).thenReturn("UTF-8");
        AtomicInteger status = new AtomicInteger(200);
        when(resp.getStatus()).thenAnswer(call -> status.get());
        doAnswer(call -> { status.set(call.getArgument(0)); return null; }).when(resp).setStatus(anyInt());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        when(resp.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener listener) {}
            @Override public void write(int b) { bytes.write(b); }
        });
        StringWriter errors = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(errors));
        if (commitFails) {
            RuntimeException error = constraint
                    ? new IllegalStateException(new SQLException("FK conflict", "23503"))
                    : new IllegalStateException("Database unavailable");
            doThrow(error).when(data).endRequest(true);
        }
        try (var singleton = mockStatic(JsonDataService.class)) {
            singleton.when(JsonDataService::getInstance).thenReturn(data);
            new DatabaseTransactionFilter().doFilter(req, resp, (request, response) -> {
                assertEquals(0, bytes.size());
                if (validationError) ((HttpServletResponse) response).setStatus(400);
                response.getWriter().write(validationError ? "validation error" : "saved successfully");
                response.flushBuffer();
                assertEquals(0, bytes.size(), "flush must not send uncommitted data");
            });
        }
        var order=inOrder(data);
        order.verify(data).beginRequest();
        verify(data).beginRequest();
        verify(data).endRequest(!validationError);
        if (commitFails) {
            assertEquals(0, bytes.size());
            assertFalse(errors.toString().contains("saved successfully"));
            assertEquals(constraint ? 409 : 500, status.get());
        } else {
            assertEquals(validationError ? "validation error" : "saved successfully", bytes.toString("UTF-8"));
        }
    }
}
