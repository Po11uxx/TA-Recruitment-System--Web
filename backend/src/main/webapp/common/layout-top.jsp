<%@ page pageEncoding="UTF-8" %>
<%
  String userRole = (String) session.getAttribute("role");
  String uri = request.getRequestURI();
  String[] parts = uri.split("/");
  String currentSection = parts.length > 2 ? parts[2] : "";
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>UCI BioSci — TA Application System</title>
  <meta name="ctx" content="<%= request.getContextPath() %>">
  <jsp:include page="/common/head.jsp"/>
</head>
<body class="bg-zinc-50 text-zinc-900 font-sans selection:bg-zinc-200">
<div class="flex h-screen w-full">
  <jsp:include page="/common/sidebar.jsp"/>

  <main class="flex-1 flex flex-col min-w-0 overflow-hidden">
    <header class="hidden md:flex h-14 items-center justify-between px-6 border-b border-zinc-200/50 bg-white/30 backdrop-blur-md sticky top-0 z-10">
      <div class="flex items-center gap-2 text-sm text-zinc-500">
        <span>UCI BioSci</span>
        <i data-lucide="chevron-right" class="h-4 w-4"></i>
        <span class="text-zinc-900 font-medium"><%= com.tamo.util.JsonUtil.escapeHtml("organiser".equals(currentSection)||"applicant".equals(currentSection)||"admin".equals(currentSection) ? com.tamo.util.RoleLabels.display(currentSection) : currentSection) %></span>
      </div>
      <div class="flex items-center gap-4 relative">
        <button id="notificationBtn" onclick="toggleNotificationPanel()" class="relative p-2 text-zinc-400 hover:text-zinc-900 transition-colors rounded-full hover:bg-zinc-100">
          <i data-lucide="bell" class="h-5 w-5"></i>
          <span id="headerUnreadBadge" class="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-blue-500 hidden"></span>
        </button>
        <!-- Notification Dropdown Panel -->
        <div id="notificationPanel" class="hidden absolute right-0 top-12 w-80 bg-white rounded-xl shadow-xl border border-zinc-200 z-50">
          <div class="p-4 border-b border-zinc-100 flex items-center justify-between">
            <h3 class="font-semibold text-zinc-900">Notifications</h3>
            <button onclick="markAllRead()" class="text-xs text-blue-600 hover:text-blue-800">Mark all read</button>
          </div>
          <div id="headerNotifList" class="max-h-80 overflow-y-auto divide-y divide-zinc-100">
            <div class="p-4 text-center text-zinc-400 text-sm">Loading...</div>
          </div>
          <% if ("applicant".equals(userRole)) { %>
          <div class="p-3 border-t border-zinc-100">
            <a href="<%= request.getContextPath() %>/applicant/dashboard.jsp" class="text-sm text-blue-600 hover:text-blue-800 font-medium">View all on Dashboard</a>
          </div>
          <% } %>
        </div>
        <div class="h-8 w-8 rounded-full bg-zinc-200 border border-zinc-300 overflow-hidden flex items-center justify-center text-xs font-bold text-zinc-600">
          <%
            String un = (String) session.getAttribute("userName");
            out.print(com.tamo.util.JsonUtil.escapeHtml(un != null && !un.isEmpty() ? un.substring(0,1).toUpperCase() : "U"));
          %>
        </div>
      </div>
    </header>

    <div class="flex-1 overflow-auto p-4 pt-16 md:pt-4 md:p-8">
      <div class="max-w-6xl mx-auto w-full">
