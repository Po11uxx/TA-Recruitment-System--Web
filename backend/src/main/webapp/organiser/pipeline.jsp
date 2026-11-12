<%@ page pageEncoding="UTF-8" %>
  <%@ include file="/common/auth-check.jsp" %>
    <jsp:include page="/common/layout-top.jsp" />

    <div class="space-y-6 min-w-0">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 class="text-3xl font-bold tracking-tight text-zinc-900">Application Pipeline</h1>
          <p class="text-zinc-500 mt-2">Manage applicants through the hiring stages. Use Review Workspace for approval.</p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <div class="relative w-full md:w-64">
            <i data-lucide="search" class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400"></i>
            <input id="searchInput" type="text" placeholder="Search candidates..." class="input pl-9 bg-white"
              onkeyup="filterView()">
          </div>
          <label class="sr-only" for="pipelinePeriod">Application period</label>
          <select id="pipelinePeriod" class="input w-auto" onchange="loadPipeline()"><option value="">All periods</option></select>
          <select id="moduleFilter" aria-label="Course" class="input w-auto" onchange="refreshSectionFilter(); renderKanban()">
            <option value="">All Modules</option>
          </select>
        </div>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <label for="sectionFilter">Section</label>
        <select id="sectionFilter" class="input w-auto" onchange="renderKanban()"><option value="">All sections</option></select>
        <label for="matchFilter">Time matching</label>
        <select id="matchFilter" class="input w-auto" onchange="renderKanban()">
          <option value="">All results</option><option value="available">Available</option>
          <option value="conflict">Time conflict</option><option value="pending">Needs confirmation</option>
        </select>
        <p class="text-sm text-zinc-500">Uses submitted snapshots and current active sections. Open an application to review time conflicts.</p>
      </div>
      <div class="flex flex-wrap gap-3">
<label>Student level<select id="qualificationLevel" class="input" onchange="renderKanban()"><option value="">All levels</option><option value="undergraduate">Undergraduate</option><option value="masters">Master’s</option><option value="phd">PhD</option></select></label>
<label>Skill<input id="qualificationSkill" class="input" oninput="renderKanban()" placeholder="e.g. Python"></label><label>Interest<input id="qualificationInterest" class="input" oninput="renderKanban()"></label>
<label>Teaching experience<select id="qualificationExperience" class="input" onchange="renderKanban()"><option value="">All applicants</option><option value="yes">Reported teaching experience</option><option value="no">None reported</option></select></label>
<label>Qualification match<select id="qualificationMatch" class="input" onchange="renderKanban()"><option value="">All results</option><option value="meets_required">Meets required criteria</option><option value="missing_required">Missing required criteria</option><option value="needs_confirmation">Needs confirmation</option></select></label>
<label>Sort<select id="qualificationSort" class="input" onchange="renderKanban()"><option value="newest">Newest application</option><option value="name">Applicant name</option><option value="score">Qualification match score</option></select></label>
      </div>
      <div class="pb-4">
        <div class="pipeline-board" id="kanban">
          <div class="text-center text-zinc-400 py-12 w-full">Loading...</div>
        </div>
      </div>
    </div>

    <!-- Candidate Detail Panel -->
    <div id="candidatePanel" class="fixed inset-0 z-50 hidden">
      <div class="absolute inset-0 bg-zinc-900/20 backdrop-blur-sm" onclick="closePanel()"></div>
      <div
        class="absolute inset-y-0 right-0 w-full md:w-[400px] bg-white border-l border-zinc-200 shadow-2xl flex flex-col">
        <div class="p-6 border-b border-zinc-200 flex items-center justify-between">
          <h2 class="text-lg font-bold text-zinc-900">Applicant Summary</h2>
          <button onclick="closePanel()" class="btn btn-ghost p-2"><i data-lucide="x" class="h-5 w-5"></i></button>
        </div>
        <div class="flex-1 overflow-y-auto p-6 space-y-6" id="panelContent"></div>
        <div class="p-6 border-t border-zinc-200 flex gap-3 bg-zinc-50">
          <a id="viewFullApp" href="#" class="btn btn-primary flex-1 text-center">View Full Application</a>
        </div>
      </div>
    </div>

    <jsp:include page="/common/layout-bottom.jsp" />
    <script src="<%= request.getContextPath() %>/static/js/section-matching.js"></script>
    <script>
      const ctx = document.querySelector('meta[name="ctx"]').content;
      // Format ISO date like "2026-05-20T12:42:00" → "2026.5.20 12:42"
      function formatDate(isoStr) {
        if (!isoStr) return 'TBD';
        try {
          const d = new Date(isoStr);
          if (isNaN(d.getTime())) return isoStr;
          const y = d.getFullYear();
          const m = d.getMonth() + 1;
          const day = d.getDate();
          const hh = String(d.getHours()).padStart(2, '0');
          const mm = String(d.getMinutes()).padStart(2, '0');
          return y + '.' + m + '.' + day + ' ' + hh + ':' + mm;
        } catch (e) { return isoStr; }
      }
      const COLUMNS = [
        { id: 'applied', title: 'Applied', color: 'bg-zinc-100' },
        { id: 'review', title: 'Review', color: 'bg-blue-100 text-blue-800' },
        { id: 'shortlisted', title: 'Shortlist', color: 'bg-purple-100 text-purple-800' },
        { id: 'offer', title: 'Offer', color: 'bg-emerald-100 text-emerald-800' },
        { id: 'rejected', title: 'Rejected', color: 'bg-red-100 text-red-800' },
      ];
      const STATUS_ORDER = { applied: 0, review: 1, shortlisted: 2, offer: 3, rejected: 4 };
      let allApps = [];
      let pipelineData = {};

      let pipelineRequest = 0;
      async function loadPipeline() {
        const sequence = ++pipelineRequest;
        try {
          const data = await organiserApi.getPipeline(undefined, document.getElementById('pipelinePeriod').value);
          if (sequence !== pipelineRequest) return;
          pipelineData = data;
          // Flatten to array for backward compatibility while preserving stage info
          allApps = Object.values(data).flat();

          const mods = [...new Set(allApps.map(a => a.role).filter(Boolean))];
          const sel = document.getElementById('moduleFilter');
          const current = sel.value;
          sel.replaceChildren(new Option('All Modules', ''));
          mods.forEach(m => sel.add(new Option(m, m)));
          if (mods.includes(current)) sel.value = current;

          refreshSectionFilter();
          renderKanban();
        } catch (e) {
          if (sequence !== pipelineRequest) return;
          console.error('Failed to load pipeline', e);
          document.getElementById('kanban').innerHTML = '<div class="text-center text-red-500 py-12 w-full">Failed to load pipeline</div>';
        }
      }

      function refreshSectionFilter() {
        const select = document.getElementById('sectionFilter');
        const previous = select.value;
        const course = document.getElementById('moduleFilter').value;
        const sections = new Map();
        allApps.filter(a => !course || a.role === course).forEach(a => {
          ((a.sectionMatching || {}).sections || []).forEach(s => sections.set(s.sectionId, s));
        });
        select.replaceChildren(new Option('All sections', ''));
        sections.forEach(s => select.add(new Option(s.moduleCode + ' · ' + s.name, s.sectionId)));
        if (sections.has(previous)) select.value = previous;
      }
      function filterView() {
        renderKanban();
      }

      function renderKanban() {
        const search = (document.getElementById('searchInput').value || '').toLowerCase();
        const course = document.getElementById('moduleFilter').value;
        const sectionId = document.getElementById('sectionFilter').value;
        const status = document.getElementById('matchFilter').value;
        const apps = allApps.filter(a => {
          const q=a.submittedQualifications||{},qm=a.qualificationMatching||{};
          const level=document.getElementById('qualificationLevel').value,skill=document.getElementById('qualificationSkill').value.trim().toLowerCase(),interest=document.getElementById('qualificationInterest').value.trim().toLowerCase(),experience=document.getElementById('qualificationExperience').value,qstatus=document.getElementById('qualificationMatch').value;
          if(level&&q.studentLevel!==level)return false;
          if(skill&&!(q.skills||[]).some(v=>v.toLowerCase().includes(skill)))return false;
          if(interest&&!(q.interests||[]).some(v=>v.toLowerCase().includes(interest)))return false;
          if(experience&&((!!(q.teachingExperience||'').trim())!==(experience==='yes')))return false;
          if(qstatus&&qm.status!==qstatus)return false;
          const match = SectionMatching.selected(a, sectionId);
          return (!course || a.role === course) && (!sectionId || !!match) && (!status || (match && match.status === status)) &&
            (!search || (a.name || '').toLowerCase().includes(search) || (a.role || '').toLowerCase().includes(search));
        });

        const sort=document.getElementById('qualificationSort').value;
        apps.sort((a,b)=>sort==='name'?(a.name||'').localeCompare(b.name||'','en'):sort==='score'?((b.qualificationMatching||{}).score??-1)-((a.qualificationMatching||{}).score??-1):(b.date||'').localeCompare(a.date||''));
        document.getElementById('kanban').innerHTML = COLUMNS.map(col => {
          const colApps = apps.filter(a => a.status === col.id);
          return '<div class="flex min-w-0 flex-col bg-zinc-50/50 rounded-2xl p-3 border border-transparent kanban-col" data-status="' + col.id + '" ondragover="onDragOver(event)" ondrop="onDrop(event, \'' + col.id + '\')">' +
            '<div class="flex items-center justify-between mb-4 px-2">' +
            '<h3 class="font-semibold text-zinc-900 flex items-center gap-2">' + col.title +
            ' <span class="px-2 py-0.5 rounded-full text-xs font-bold ' + col.color + '">' + colApps.length + '</span></h3>' +
            '</div>' +
            '<div class="flex-1 min-w-0 space-y-3">' +
            colApps.map(a => renderCard(a)).join('') +
            '</div></div>';
        }).join('');
        lucide.createIcons();
      }

      function renderCard(original) {
        const a=escapeDisplay(original);
        const movable = original.canApprove === true || original.canRecommend === true;
        return '<div class="bg-white p-4 rounded-xl border border-zinc-200/60 shadow-sm hover:shadow-md transition-all kanban-card" draggable="' + movable + '" data-id="' + a.id + '" ondragstart="onDragStart(event, \'' + a.id + '\')" onclick="showCandidate(\'' + a.id + '\')">' +
          '<div class="flex justify-between items-start mb-3">' +
          '<div class="flex items-center gap-3">' +
          '<div class="h-10 w-10 rounded-full bg-zinc-100 flex items-center justify-center font-bold text-zinc-700">' + ((a.name || 'A').charAt(0)) + '</div>' +
          '<div><h4 class="font-semibold text-sm text-zinc-900">' + (a.name || 'Applicant') + '</h4>' + '<p class="text-xs text-zinc-500">ID: ' + (a.id || '-') + '</p>' + '<p class="text-xs text-zinc-500">' + (a.role || '') + '</p></div>' +
          '</div>' +
          '</div>' +
          '<div class="mt-3">' + SectionMatching.badge(SectionMatching.selected(a, document.getElementById('sectionFilter').value)) + '</div>' +
          '<div class="flex items-center justify-between text-xs text-zinc-500 mt-4 pt-4 border-t border-zinc-100">' +
          '<span class="text-zinc-900 font-bold">Applied ' + formatDate(a.date) + '</span>' +
          '</div></div>';
      }

      let dragId = null;

      function onDragStart(e, id) {
        const app = allApps.find(a => a.id === id);
        if (!app || !(app.canApprove === true || app.canRecommend === true)) { dragId = null; e.preventDefault(); return; }
        dragId = id; e.dataTransfer.effectAllowed = 'move';
      }
      function onDragOver(e) { e.preventDefault(); e.currentTarget.classList.add('drag-over'); }
      function onDrop(e, status) {
        e.preventDefault();
        e.currentTarget.classList.remove('drag-over');
        if (dragId) {
          const app = allApps.find(a => a.id === dragId);
          if (app && STATUS_ORDER[status] < STATUS_ORDER[app.status] && status !== 'rejected') {
            showToast('Cannot move candidate backwards once they are in a later stage.', 'error');
            dragId = null;
            return;
          }
          updateStatus(dragId, status);
          dragId = null;
        }
      }

      function canChangeStatus(app, status) {
        if (status === 'rejected') return app.canApprove === true;
        if (!(app.canApprove === true || app.canRecommend === true)) return false;
        if (status === 'offer') return app.selectionApproved === true;
        return ['applied', 'review', 'shortlisted'].includes(status);
      }

      async function updateStatus(appId, newStatus) {
        const app = allApps.find(a => a.id === appId);
        if (!app || app.status === newStatus) return;
        if (!canChangeStatus(app, newStatus)) {
          showToast(newStatus === 'offer' ? 'Offer requires approved selection in Review Workspace and permission to update this application.' : 'You do not have permission for this status change.', 'error');
          return;
        }
        let note;
        if (newStatus === 'rejected') {
          const reason = prompt('Enter the decision reason for rejection (10–4000 characters):');
          if (reason === null) return;
          note = reason.trim();
          if (note.length < 10 || note.length > 4000) { showToast('A decision reason must contain 10–4000 characters.', 'error'); return; }
        }
        try {
          await organiserApi.updateCandidateStatus(appId, newStatus, note);
          if (app) app.status = newStatus;
          renderKanban();
          showToast('Status updated!');
        } catch (e) {
          showToast(e.message || 'Update failed', 'error');
        }
      }

      function showCandidate(id) {
        const original = allApps.find(a => a.id === id);
        const app=original?escapeDisplay(original):null;
        if (!app) return;
        const panel = document.getElementById('panelContent');
        panel.innerHTML = '<div class="flex items-center gap-4">' +
          '<div class="h-16 w-16 rounded-full bg-zinc-100 flex items-center justify-center text-xl font-bold text-zinc-700">' + ((app.name || 'A').charAt(0)) + '</div>' +
          '<div class="flex-1"><h3 class="text-xl font-bold text-zinc-900">' + (app.name || 'Applicant') + '</h3>' +
          '<p class="text-sm text-zinc-500">Application ID: ' + (app.id || '-') + '</p>' +
          '<p class="text-sm text-zinc-500">Applied for ' + (app.role || '') + '</p>' +
          '<span class="badge badge-neutral mt-2 capitalize">' + app.status + '</span></div></div>' +
          '<div class="grid grid-cols-2 gap-4">' +
          '<div class="p-4 rounded-xl bg-zinc-50 border border-zinc-100"><p class="text-xs text-zinc-500 mb-1">Applied On</p><p class="text-lg font-bold text-zinc-900">' + formatDate(app.date) + '</p></div>' +
          '<div class="p-4 rounded-xl bg-zinc-50 border border-zinc-100"><p class="text-xs text-zinc-500 mb-1">Status</p><p class="text-lg font-bold text-zinc-900 capitalize">' + app.status + '</p></div>' +
          '</div>' +
          (app.coverLetter ? '<div><h4 class="text-sm font-semibold text-zinc-900 mb-2">Cover Letter</h4><p class="text-sm text-zinc-600 leading-relaxed">' + app.coverLetter.substring(0, 200) + '...</p></div>' : '');

        const matching = document.createElement('section');
        SectionMatching.render(matching, app.sectionMatching); panel.appendChild(matching);
        document.getElementById('viewFullApp').href = ctx + '/organiser/application-detail.jsp?id=' + app.id;
        document.getElementById('candidatePanel').classList.remove('hidden');
        lucide.createIcons();
      }

      function closePanel() { document.getElementById('candidatePanel').classList.add('hidden'); }

      organiserApi.getApplicationPeriods().then(periods => {
        const select = document.getElementById('pipelinePeriod');
        periods.forEach(period => {
          const option = document.createElement('option');
          option.value = period.id; option.textContent = period.name; select.appendChild(option);
        });
      }).catch(error => showToast(error.message || 'Could not load application periods.', 'error'));
      loadPipeline();
    </script>