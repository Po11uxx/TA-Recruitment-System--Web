package com.tamo.filter;

import com.tamo.service.JsonDataService;
import com.tamo.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.Charset;

/** Serialize legacy model mutations and commit before sending a successful response. */
public class DatabaseTransactionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if ("OPTIONS".equals(req.getMethod())) { chain.doFilter(request, response); return; }
        String origin=req.getHeader("Origin");
        if(!java.util.Set.of("GET","HEAD","OPTIONS").contains(req.getMethod())&&origin!=null){
            String expected=req.getScheme()+"://"+req.getServerName()+((req.getServerPort()==80||req.getServerPort()==443)?"":":"+req.getServerPort());
            if(!origin.equals(expected)){JsonUtil.writeError(resp,403,"Cross-origin write requests are not permitted.");return;}
        }
        JsonDataService data = JsonDataService.getInstance();
        BufferedResponse buffered = new BufferedResponse(resp);
        synchronized (data.requestLock()) {
            boolean active = false;
            try {
                data.beginRequest();
                active = true;
                chain.doFilter(request, buffered);
                active = false; // endRequest always closes, even if commit fails
                data.endRequest(buffered.getStatus() < 400);
            } catch (Exception e) {
                if (active) {
                    try { data.endRequest(false); } catch (Exception suppressed) { e.addSuppressed(suppressed); }
                }
                req.getServletContext().log("Database request failed", e);
                resp.resetBuffer();
                resp.setHeader("Content-Length", null);
                HttpSession session = req.getSession(false);
                if (session != null && session.isNew()) session.invalidate();
                String sqlState = sqlState(e);
                if ("23503".equals(sqlState)) {
                    JsonUtil.writeError(resp, 409, "Record is still referenced, or a related record does not exist.");
                } else if ("23505".equals(sqlState)) {
                    JsonUtil.writeError(resp, 409, "A record with the same unique value already exists.");
                } else {
                    JsonUtil.writeError(resp, 500, "Request could not be saved. Please retry.");
                }
                return;
            }
        }
        buffered.copyToResponse();
    }

    private static String sqlState(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && sql.getSQLState() != null) return sql.getSQLState();
        }
        return null;
    }

    private static final class BufferedResponse extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private PrintWriter writer;
        private ServletOutputStream stream;

        BufferedResponse(HttpServletResponse response) { super(response); }

        @Override public PrintWriter getWriter() {
            if (stream != null) throw new IllegalStateException("Output stream already requested");
            if (writer == null) writer = new PrintWriter(new OutputStreamWriter(body, Charset.forName(getCharacterEncoding())));
            return writer;
        }

        @Override public ServletOutputStream getOutputStream() {
            if (writer != null) throw new IllegalStateException("Writer already requested");
            if (stream == null) stream = new ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener listener) { throw new UnsupportedOperationException(); }
                @Override public void write(int value) { body.write(value); }
                @Override public void write(byte[] bytes, int offset, int length) { body.write(bytes, offset, length); }
            };
            return stream;
        }

        @Override public void flushBuffer() { if (writer != null) writer.flush(); }
        @Override public void resetBuffer() { flushBuffer(); body.reset(); }
        @Override public void reset() { super.reset(); body.reset(); writer = null; stream = null; }
        @Override public void sendError(int code, String message) {
            resetBuffer(); setStatus(code); getWriter().write(message == null ? "" : message);
        }
        @Override public void sendError(int code) { sendError(code, ""); }
        @Override public void sendRedirect(String location) { resetBuffer(); setStatus(302); setHeader("Location", location); }

        void copyToResponse() throws IOException {
            flushBuffer();
            body.writeTo(getResponse().getOutputStream());
        }
    }
}
