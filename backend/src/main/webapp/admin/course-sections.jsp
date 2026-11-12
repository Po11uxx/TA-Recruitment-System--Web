<%@ page pageEncoding="UTF-8" %>
<%@ include file="/common/auth-check.jsp" %>
<jsp:include page="/common/layout-top.jsp"/>
<jsp:include page="/common/course-sections-content.jsp"/>
<jsp:include page="/common/layout-bottom.jsp"/>
<script src="<%= request.getContextPath() %>/static/js/course-sections.js?v=course-access-1"></script>
