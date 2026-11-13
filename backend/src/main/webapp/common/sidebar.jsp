<%@ page pageEncoding="UTF-8" %>
<%
  String currentPath = request.getRequestURI();
  String ctx = request.getContextPath();
  String role = (String) session.getAttribute("role");
  String userName = (String) session.getAttribute("userName");
  if (userName == null || userName.isEmpty()) userName = "User";

  boolean hasPersonalProfile = "applicant".equals(role);
  String profilePath = ctx + "/applicant/profile.jsp";
  boolean profileActive = hasPersonalProfile && currentPath.equals(profilePath);
  String accountTag = hasPersonalProfile ? "a" : "div";
  String accountAttributes = hasPersonalProfile
      ? " href=\"" + com.tamo.util.JsonUtil.escapeHtml(profilePath) + "\" aria-label=\"Open personal profile\""
          + (profileActive ? " aria-current=\"page\"" : "")
      : "";
  String accountClasses = hasPersonalProfile
      ? (profileActive ? " bg-blue-100" : " hover:bg-blue-50") + " rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-blue-600"
      : "";

  String[][] navItems;
  String sectionRoot;
  if ("applicant".equals(role)) {
    sectionRoot = ctx + "/applicant";
    navItems = new String[][] {
      {"Dashboard", sectionRoot + "/dashboard.jsp", "layout-dashboard"},
      {"TA Application Profile", sectionRoot + "/application-profile.jsp", "clipboard-list"},
      {"Weekly Availability", sectionRoot + "/availability.jsp", "calendar-clock"},
      {"Find Jobs", sectionRoot + "/jobs.jsp", "users"},
      {"My Applications", sectionRoot + "/applications.jsp", "briefcase"},
      {"My TA Placements", sectionRoot + "/assignments.jsp", "calendar-check"},
    };
  } else if ("organiser".equals(role)) {
    sectionRoot = ctx + "/organiser";
    navItems = new String[][] {
      {"Review Workspace", ctx+"/review/dashboard.jsp", "clipboard-check"},
    };
  } else if ("reviewer".equals(role)||"decision_maker".equals(role)||"viewer".equals(role)) {
    sectionRoot=ctx+"/review";navItems=new String[][]{{"Review Workspace",sectionRoot+"/dashboard.jsp","clipboard-check"}};
  } else {
    sectionRoot = ctx + "/admin";
    navItems = new String[][] {
      {"Overview", sectionRoot + "/dashboard.jsp", "layout-dashboard"},
      {"CSV Import / Export", sectionRoot + "/csv.jsp", "file-spreadsheet"},
      {"Review Workspace", ctx+"/review/dashboard.jsp", "clipboard-check"},
      {"Review Access", sectionRoot+"/review-access.jsp", "shield-check"},
      {"Workload Intel", sectionRoot + "/workload.jsp", "trending-up"},
      {"TA Positions", sectionRoot + "/jobs.jsp", "briefcase"},
      {"Global Modules", sectionRoot + "/modules.jsp", "book-open"},
      {"Matching Criteria", sectionRoot + "/criteria.jsp", "list-checks"},
      {"Course Sections", sectionRoot + "/course-sections.jsp", "calendar-range"},
      {"TA Assignments", sectionRoot + "/assignments.jsp", "calendar-check"},
      {"Application Periods", sectionRoot + "/application-periods.jsp", "calendar-range"},
      {"Properties", sectionRoot + "/properties.jsp", "sliders"},
      {"Users", sectionRoot + "/users.jsp", "users"},
      {"Data Editor", sectionRoot + "/data-editor.jsp", "database"},
      {"System Logs", sectionRoot + "/logs.jsp", "settings"},
    };
  }
%>
<!-- Mobile Menu Button (visible on mobile only) -->
<div class="md:hidden fixed top-0 left-0 right-0 h-14 bg-white/80 backdrop-blur-md border-b border-zinc-200/50 z-40 flex items-center justify-between px-4">
  <div class="flex items-center gap-2 font-semibold">
    <div class="uci-app-brand"><span>UCI BioSci</span><small>TA Application System</small></div>
  </div>
  <button onclick="toggleMobileMenu()" class="p-2 rounded-lg hover:bg-zinc-100">
    <i data-lucide="menu" class="h-5 w-5"></i>
  </button>
</div>

<!-- Mobile Menu Overlay -->
<div id="mobileMenuOverlay" class="md:hidden fixed inset-0 bg-zinc-900/50 z-40 hidden" onclick="toggleMobileMenu()"></div>

<!-- Mobile Sidebar -->
<aside id="mobileSidebar" class="md:hidden fixed inset-y-0 left-0 w-64 bg-white z-50 transform -translate-x-full transition-transform duration-300 flex flex-col">
  <div class="flex h-14 items-center justify-between px-4">
    <div class="flex items-center gap-2 font-semibold">
      <div class="uci-app-brand"><span>UCI BioSci</span><small>TA Application System</small></div>
    </div>
    <button onclick="toggleMobileMenu()" class="p-2 rounded-lg hover:bg-zinc-100">
      <i data-lucide="x" class="h-5 w-5"></i>
    </button>
  </div>
  <div class="flex-1 overflow-auto py-4">
    <nav class="space-y-1 px-3">
      <% for (String[] item : navItems) {
        boolean isActive = currentPath.equals(item[1]) || currentPath.equals(item[1].replace(ctx, ""));
        String activeClass = isActive ? "text-blue-700 bg-blue-100 shadow-[inset_3px_0_0_#2563eb]" : "text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100/50";
      %>
      <a href="<%= item[1] %>" class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all <%= activeClass %>">
        <i data-lucide="<%= item[2] %>" class="h-4 w-4"></i>
        <span><%= item[0] %></span>
      </a>
      <% } %>
    </nav>
  </div>
  <div class="p-4 border-t border-zinc-200/50">
    <<%= accountTag %><%= accountAttributes %> class="flex items-center gap-3 px-3 py-2 mb-2<%= accountClasses %>">
      <div class="h-8 w-8 rounded-full bg-zinc-200 border border-zinc-300 flex items-center justify-center text-xs font-bold text-zinc-600">
        <%= com.tamo.util.JsonUtil.escapeHtml(userName.substring(0,1).toUpperCase()) %>
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-sm font-medium text-zinc-900 truncate"><%= com.tamo.util.JsonUtil.escapeHtml(userName) %></p>
        <p class="text-xs text-zinc-500"><%= com.tamo.util.RoleLabels.display(role) %></p>
      </div>
      <% if (hasPersonalProfile) { %><i data-lucide="chevron-right" class="h-4 w-4 shrink-0 text-zinc-400" aria-hidden="true"></i><% } %>
    </<%= accountTag %>>
    <button onclick="doLogout()" class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-zinc-500 hover:bg-zinc-100/50 hover:text-zinc-900">
      <i data-lucide="log-out" class="h-4 w-4"></i>
      <span>Sign out</span>
    </button>
  </div>
</aside>

<!-- Desktop Sidebar -->
<aside class="hidden md:flex w-64 flex-col border-r border-zinc-200/50 bg-white/50 backdrop-blur-xl flex-shrink-0">
  <div class="flex h-14 items-center px-6 pt-2">
    <div class="flex items-center gap-2 font-semibold">
      <div class="uci-app-brand"><span>UCI BioSci</span><small>TA Application System</small></div>
    </div>
  </div>

  <div class="flex-1 overflow-auto py-6">
    <nav class="space-y-1 px-4">
      <% for (String[] item : navItems) {
        boolean isActive = currentPath.equals(item[1]) || currentPath.equals(item[1].replace(ctx, ""));
        String activeClass = isActive ? "text-blue-700 bg-blue-100 shadow-[inset_3px_0_0_#2563eb]" : "text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100/50";
      %>
      <a href="<%= item[1] %>"
         class="group flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all duration-200 <%= activeClass %>">
        <i data-lucide="<%= item[2] %>" class="h-4 w-4"></i>
        <span><%= item[0] %></span>
      </a>
      <% } %>
    </nav>
  </div>

  <div class="p-4 mt-auto border-t border-zinc-200/50">
    <<%= accountTag %><%= accountAttributes %> class="flex items-center gap-3 px-3 py-2 mb-2<%= accountClasses %>">
      <div class="h-8 w-8 rounded-full bg-zinc-200 border border-zinc-300 flex items-center justify-center text-xs font-bold text-zinc-600">
        <%= com.tamo.util.JsonUtil.escapeHtml(userName.substring(0,1).toUpperCase()) %>
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-sm font-medium text-zinc-900 truncate"><%= com.tamo.util.JsonUtil.escapeHtml(userName) %></p>
        <p class="text-xs text-zinc-500"><%= com.tamo.util.RoleLabels.display(role) %></p>
      </div>
      <% if (hasPersonalProfile) { %><i data-lucide="chevron-right" class="h-4 w-4 shrink-0 text-zinc-400" aria-hidden="true"></i><% } %>
    </<%= accountTag %>>
    <button onclick="doLogout()" class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-zinc-500 hover:bg-zinc-100/50 hover:text-zinc-900 transition-colors">
      <i data-lucide="log-out" class="h-4 w-4"></i>
      <span>Sign out</span>
    </button>
  </div>
</aside>
