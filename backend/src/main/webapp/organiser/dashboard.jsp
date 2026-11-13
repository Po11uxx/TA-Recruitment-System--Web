<%@ page pageEncoding="UTF-8" %>
  <%@ include file="/common/auth-check.jsp" %>
    <jsp:include page="/common/layout-top.jsp" />

    <div class="space-y-8 max-w-6xl mx-auto">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 class="text-3xl font-bold tracking-tight text-zinc-900">Teacher Overview</h1>
          <p class="text-zinc-500 mt-2">Manage your TA application pipeline across modules.</p>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6" id="statsGrid">
        <div class="card fade-in">
          <div class="p-6 flex items-center gap-4">
            <div class="stat-icon"><i data-lucide="briefcase" class="h-6 w-6"></i></div>
            <div>
              <p class="text-sm font-medium text-zinc-500">Active Postings</p>
              <p class="text-2xl font-bold text-zinc-900" id="statPostings">-</p>
            </div>
          </div>
        </div>
        <div class="card fade-in">
          <div class="p-6 flex items-center gap-4">
            <div class="stat-icon"><i data-lucide="users" class="h-6 w-6"></i></div>
            <div>
              <p class="text-sm font-medium text-zinc-500">Total Applicants</p>
              <p class="text-2xl font-bold text-zinc-900" id="statApplicants">-</p>
            </div>
          </div>
        </div>
        <div class="card fade-in">
          <div class="p-6 flex items-center gap-4">
            <div class="stat-icon"><i data-lucide="calendar" class="h-6 w-6"></i></div>
            <div>
              <p class="text-sm font-medium text-zinc-500">Total Applications</p>
              <p class="text-2xl font-bold text-zinc-900" id="statApplications">-</p>
            </div>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div class="lg:col-span-2 space-y-6">
          <div class="flex items-center justify-between">
            <h2 class="text-xl font-semibold text-zinc-900">Recent Applications</h2>
            <a href="<%= request.getContextPath() %>/organiser/pipeline.jsp"
              class="btn btn-ghost text-zinc-500 hover:text-zinc-900">View pipeline</a>
          </div>
          <div class="card">
            <div class="divide-y divide-zinc-100" id="recentActivity">
              <div class="p-6 text-center text-zinc-400">Loading...</div>
            </div>
            <div class="p-4 border-t border-zinc-100 text-center">
              <a href="<%= request.getContextPath() %>/organiser/pipeline.jsp"
                class="text-sm font-medium text-zinc-600 hover:text-zinc-900">View all applications &rarr;</a>
            </div>
          </div>
        </div>

        <div class="space-y-6">
          <div>
            <h2 class="text-xl font-semibold text-zinc-900">Active Postings</h2>
            <p class="text-sm text-zinc-500">Track your published course and TA postings in one place.</p>
          </div>
          <div class="card">
            <div class="max-h-96 overflow-y-auto">
              <div class="divide-y divide-zinc-100" id="activePostings">
                <div class="p-6 text-center text-zinc-400">Loading...</div>
              </div>
            </div>
          </div>
          <div class="card">
            <div class="p-6 flex items-center justify-between gap-4">
              <div>
                <p class="text-sm font-medium text-zinc-500">Notifications</p>
                <h3 class="text-lg font-semibold text-zinc-900">Application updates</h3>
              </div>
              <button type="button" onclick="markAllNotificationsRead()"
                class="btn btn-ghost text-sm text-zinc-600 hover:text-zinc-900">
                Mark all read
              </button>
            </div>
            <div class="divide-y divide-zinc-100 max-h-96 overflow-y-auto" id="organiserNotifications">
              <div class="p-6 text-center text-zinc-400">Loading notifications...</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <jsp:include page="/common/layout-bottom.jsp" />
    <script>
      async function loadOrganiserNotifications() {
        try {
          const notifications = await organiserApi.getNotifications();
          const container = document.getElementById('organiserNotifications');
          if (!notifications || notifications.length === 0) {
            container.innerHTML = '<div class="p-6 text-center text-zinc-400">No notifications yet</div>';
            return;
          }

          container.innerHTML = notifications.map(notif => {
            const readClass = notif.read ? 'opacity-60' : 'bg-zinc-50';
            return '<div class="p-6 hover:bg-zinc-50 transition-colors ' + readClass + '">' +
              '<div class="flex items-start justify-between gap-4">' +
              '<div>' +
              '<p class="text-sm font-semibold text-zinc-900">' + notificationText(notif.title || 'Notification') + '</p>' +
              '<p class="text-sm text-zinc-500 mt-1">' + notificationText(notif.message || '') + '</p>' +
              ((notif.type || '').startsWith('assignment_') ? '<a class="text-sm text-blue-600 underline" href="' + getNotificationLink(notif) + '">Review assignment</a>' : '') +
              '</div>' +
              '<button type="button" onclick="markNotificationRead(\'' + notif.id + '\')" class="text-xs text-blue-600 hover:underline">Mark read</button>' +
              '</div>' +
              '<p class="text-xs text-zinc-400 mt-3">' + (new Date(notif.createdAt).toLocaleString() || '') + '</p>' +
              '</div>';
          }).join('');
        } catch (e) {
          console.error('Failed to load notifications', e);
          document.getElementById('organiserNotifications').innerHTML = '<div class="p-6 text-center text-red-500">Unable to load notifications</div>';
        }
      }

      async function markNotificationRead(id) {
        try {
          await organiserApi.markNotificationRead(id);
          await loadOrganiserNotifications();
        } catch (e) {
          console.error('Failed to mark notification read', e);
        }
      }

      async function markAllNotificationsRead() {
        try {
          await organiserApi.markAllNotificationsRead();
          await loadOrganiserNotifications();
        } catch (e) {
          console.error('Failed to mark all notifications read', e);
        }
      }

      async function loadDashboard() {
        try {
          const data = await organiserApi.getDashboard();
          const jobs = data.jobs || [];
          const recentApps = data.recentApplications || [];
          document.getElementById('statPostings').textContent = data.activePostings || 0;
          document.getElementById('statApplicants').textContent = data.currentApplicants || 0;
          document.getElementById('statApplications').textContent = data.totalApplications || 0;

          const actDiv = document.getElementById('recentActivity');
          if (recentApps.length === 0) {
            actDiv.innerHTML = '<div class="p-6 text-center text-zinc-400">No recent applications</div>';
          } else {
            actDiv.innerHTML = recentApps.slice(0, 5).map(original => {
              const app = escapeDisplay(original);
              return '<a href="' + document.querySelector('meta[name="ctx"]').content + '/organiser/application-detail.jsp?id=' + encodeURIComponent(original.id) + '" class="p-6 flex items-center justify-between hover:bg-zinc-50/50 transition-colors group">' +
                '<div class="flex items-center gap-4">' +
                '<div class="h-10 w-10 rounded-full bg-zinc-200 flex items-center justify-center font-medium text-zinc-600">' + ((app.applicantName || app.name || '?').charAt(0)) + '</div>' +
                '<div class="space-y-1"><h3 class="font-semibold text-zinc-900 group-hover:text-blue-600">' + (app.applicantName || app.name || 'Applicant') + '</h3>' +
                '<p class="text-sm text-zinc-500">' + (app.moduleCode || app.role || app.jobTitle || '') + '</p></div></div>' +
                '<span class="badge badge-neutral capitalize">' + (app.status || 'applied') + '</span></a>';
            }).join('');
          }

          const posDiv = document.getElementById('activePostings');
          if (jobs.length === 0) {
            posDiv.innerHTML = '<div class="p-6 text-center text-zinc-400">No active postings</div>';
          } else {
            posDiv.innerHTML = jobs.map(original => {
              const j=escapeDisplay(original);
              const pct = Math.min(100, ((j.applicationCount || 0) / 100) * 100);
              return '<div class="p-6">' +
                '<div class="flex justify-between items-start mb-2">' +
                '<h3 class="font-semibold text-zinc-900">' + (j.moduleCode || j.title) + '</h3>' +
                '<span class="badge bg-amber-50 text-amber-600 border-amber-200">' + (j.status || 'active') + '</span>' +
                '</div>' +
                '<div class="flex items-center gap-4 text-sm mt-3">' +
                '<div class="flex flex-col"><span class="text-zinc-500">Applicants</span><span class="font-semibold text-zinc-900">' + (j.applicationCount || 0) + '</span></div>' +
                '<div class="h-8 w-px bg-zinc-200"></div>' +
                '<div class="flex flex-col"><span class="text-zinc-500">Positions</span><span class="font-semibold text-zinc-900">' + (j.positions || '-') + '</span></div>' +
                '</div>' +
                '<div class="mt-4 w-full h-1.5 bg-zinc-100 rounded-full overflow-hidden"><div class="h-full bg-blue-500 rounded-full" style="width:' + pct + '%"></div></div></div>';
            }).join('');
          }

          await loadOrganiserNotifications();
        } catch (e) {
          console.error('Failed to load dashboard', e);
        }
      }
      loadDashboard();
    </script>