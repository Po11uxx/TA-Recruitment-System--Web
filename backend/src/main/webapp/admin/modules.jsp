<%@ page pageEncoding="UTF-8" %>
<%@ include file="/common/auth-check.jsp" %>
<jsp:include page="/common/layout-top.jsp"/>

<div class="space-y-8 max-w-6xl mx-auto">
  <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
    <div>
      <h1 class="text-3xl font-bold tracking-tight text-zinc-900">Global Modules</h1>
      <p class="text-zinc-500 mt-2">Manage all courses, assign Teachers and review jobs published under each module.</p>
    </div>
    <button onclick="openModuleModal()" class="btn btn-primary gap-2 shrink-0">
      <i data-lucide="plus" class="h-4 w-4"></i> Add Module
    </button>
  </div>

  <div class="flex flex-col sm:flex-row gap-4 justify-between items-center">
    <div class="relative w-full sm:w-96">
      <i data-lucide="search" class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400"></i>
      <input id="searchInput" type="text" placeholder="Search modules, names, or Teachers..." class="input pl-10" onkeyup="debounceSearch()">
    </div>
  </div>

  <div class="card overflow-hidden">
    <div class="overflow-x-auto">
      <table class="w-full text-sm text-left">
        <thead class="text-xs text-zinc-500 bg-zinc-50/50 border-b border-zinc-200 uppercase tracking-wider">
          <tr>
            <th class="px-4 py-4 font-medium w-10"></th>
            <th class="px-4 py-4 font-medium">Module</th>
            <th class="px-4 py-4 font-medium">Teacher</th>
            <th class="px-4 py-4 font-medium">Department</th>
            <th class="px-4 py-4 font-medium">Term</th>
            <th class="px-4 py-4 font-medium">Jobs</th>
            <th class="px-4 py-4 font-medium">Status</th>
            <th class="px-4 py-4 font-medium text-right">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-zinc-200" id="modulesBody">
          <tr><td colspan="8" class="p-6 text-center text-zinc-400">Loading...</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</div>

<!-- Module Modal -->
<div id="moduleModal" data-modal class="fixed inset-0 z-50 items-center justify-center bg-black/50 backdrop-blur-sm">
  <div class="bg-white rounded-2xl shadow-xl max-w-lg w-full mx-4">
    <div class="bg-zinc-50 border-b border-zinc-200/60 p-6">
      <h2 class="text-xl font-bold text-zinc-900" id="modModalTitle">Add Module</h2>
    </div>
    <div class="p-6 space-y-4">
      <input type="hidden" id="editModId">
      <div class="grid grid-cols-2 gap-4">
        <div><label class="block text-sm font-medium text-zinc-700 mb-1.5">Code</label><input id="mCode" class="input" placeholder="CS201"></div>
        <div><label class="block text-sm font-medium text-zinc-700 mb-1.5">Name</label><input id="mName" class="input" placeholder="Data Structures"></div>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-zinc-700 mb-1.5">Department</label>
          <select id="mDept" class="input">
            <option value="">Select department...</option>
          </select>
          <p id="mDeptHint" class="hidden text-xs text-amber-600 mt-1.5">No departments yet. Add one in Properties first.</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-zinc-700 mb-1.5">Term</label>
          <select id="mTerm" class="input">
            <option value="">Select term...</option>
          </select>
          <p id="mTermHint" class="hidden text-xs text-amber-600 mt-1.5">No terms yet. Add one in Properties first.</p>
        </div>
      </div>
      <div>
        <p id="teacherLabel" class="text-sm font-medium text-zinc-700 mb-1.5">Teacher</p>
        <details id="teacherPicker" class="relative">
          <summary class="input cursor-pointer" aria-labelledby="teacherLabel selectedTeacherName">
            <span id="selectedTeacherName">Choose a Teacher</span>
          </summary>
          <div class="absolute z-10 mt-1 w-full rounded-lg border bg-white p-2 shadow-lg">
            <input id="teacherSearch" type="search" class="input" placeholder="Search by name or email" aria-label="Search Teachers">
            <div id="teacherOptions" class="mt-2 max-h-40 overflow-y-auto"></div>
            <p id="teacherStatus" class="px-2 py-1 text-sm text-zinc-500" role="status"></p>
          </div>
        </details>
      </div>
      <div id="modError" class="hidden text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-4 py-3"></div>
    </div>
    <div class="p-6 border-t border-zinc-200 flex justify-end gap-3">
      <button onclick="closeModuleModal()" class="btn btn-secondary">Cancel</button>
      <button onclick="saveModule()" class="btn btn-primary" id="modSaveBtn">Create</button>
    </div>
  </div>
</div>

<jsp:include page="/common/layout-bottom.jsp"/>
<script>
let allModules = [];
let allTerms = [];
let allDepartments = [];
let searchTimer;
const expandedModules = new Set();

function debounceSearch() { clearTimeout(searchTimer); searchTimer = setTimeout(loadModules, 300); }

async function loadModules() {
  try {
    const kw = document.getElementById('searchInput').value.trim();
    const data = await adminApi.getModules(kw || undefined);
    allModules = data.modules || data || [];
    renderModules(allModules);
  } catch (e) { console.error('Failed to load modules', e); }
}

async function loadProperties() {
  try {
    const [termsData, deptsData] = await Promise.all([adminApi.getTerms(), adminApi.getDepartments()]);
    allTerms = termsData.terms || termsData || [];
    allDepartments = deptsData.departments || deptsData || [];
  } catch (e) {
    console.error('Failed to load terms/departments', e);
    allTerms = [];
    allDepartments = [];
  }
}

function populatePropertyDropdowns(selectedDept, selectedTerm) {
  const deptSel = document.getElementById('mDept');
  const termSel = document.getElementById('mTerm');
  const deptHint = document.getElementById('mDeptHint');
  const termHint = document.getElementById('mTermHint');

  if (allDepartments.length === 0) {
    deptSel.innerHTML = '<option value="">No departments available</option>';
    deptSel.disabled = true;
    deptHint.classList.remove('hidden');
  } else {
    deptSel.disabled = false;
    deptHint.classList.add('hidden');
    const selDept = selectedDept || '';
    const includesSelected = selDept && allDepartments.some(d => d.name === selDept);
    const opts = ['<option value="">Select department...</option>']
      .concat(allDepartments.map(d => {
        const isSel = d.name === selDept ? ' selected' : '';
        return '<option value="' + escapeHtml(d.name) + '"' + isSel + '>' + escapeHtml(d.name) + '</option>';
      }));
    if (selDept && !includesSelected) {
      opts.push('<option value="' + escapeHtml(selDept) + '" selected>' + escapeHtml(selDept) + ' (legacy)</option>');
    }
    deptSel.innerHTML = opts.join('');
  }

  if (allTerms.length === 0) {
    termSel.innerHTML = '<option value="">No terms available</option>';
    termSel.disabled = true;
    termHint.classList.remove('hidden');
  } else {
    termSel.disabled = false;
    termHint.classList.add('hidden');
    const selTerm = selectedTerm || '';
    const includesSelected = selTerm && allTerms.some(t => t.name === selTerm);
    const opts = ['<option value="">Select term...</option>']
      .concat(allTerms.map(t => {
        const isSel = t.name === selTerm ? ' selected' : '';
        return '<option value="' + escapeHtml(t.name) + '"' + isSel + '>' + escapeHtml(t.name) + '</option>';
      }));
    if (selTerm && !includesSelected) {
      opts.push('<option value="' + escapeHtml(selTerm) + '" selected>' + escapeHtml(selTerm) + ' (legacy)</option>');
    }
    termSel.innerHTML = opts.join('');
  }
}

function escapeHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function jobStatusBadge(status) {
  const s = (status || '').toLowerCase();
  if (s === 'active') return 'badge-success';
  if (s === 'draft') return 'badge-warning';
  if (s === 'closed') return 'badge-neutral';
  return 'badge-outline';
}

function renderJobsRow(m) {
  const jobs = m.jobs || [];
  if (jobs.length === 0) {
    return '<tr id="jobs-' + m.id + '" class="bg-zinc-50/50"><td></td><td colspan="7" class="px-4 py-4 text-sm text-zinc-500 italic">No jobs published under this module yet.</td></tr>';
  }
  const rows = jobs.map(j => {
    return '<tr class="hover:bg-zinc-100/60">' +
      '<td class="px-4 py-2.5 text-zinc-500 text-xs font-mono">' + escapeHtml(j.id) + '</td>' +
      '<td class="px-4 py-2.5">' +
        '<div class="font-medium text-zinc-900">' + escapeHtml(j.title || '-') + '</div>' +
        '<div class="text-xs text-zinc-500">' + escapeHtml(j.type || '') + (j.hoursPerWeek ? ' · ' + j.hoursPerWeek + 'h/wk' : '') + '</div>' +
      '</td>' +
      '<td class="px-4 py-2.5 text-zinc-600">' + (j.positions != null ? j.positions : '-') + '</td>' +
      '<td class="px-4 py-2.5 text-zinc-600">' + (j.applicationCount != null ? j.applicationCount : 0) + '</td>' +
      '<td class="px-4 py-2.5 text-zinc-600">' + escapeHtml(j.deadline || '-') + '</td>' +
      '<td class="px-4 py-2.5"><span class="badge ' + jobStatusBadge(j.status) + ' capitalize">' + escapeHtml(j.status || 'active') + '</span></td>' +
    '</tr>';
  }).join('');

  return '<tr id="jobs-' + m.id + '" class="bg-zinc-50/40">' +
    '<td></td>' +
    '<td colspan="7" class="px-4 py-4">' +
      '<div class="rounded-lg border border-zinc-200 bg-white overflow-hidden">' +
        '<div class="px-4 py-2.5 text-xs font-medium text-zinc-500 uppercase tracking-wider border-b border-zinc-200 bg-zinc-50/70">' +
          'Jobs published under ' + escapeHtml(m.code) + ' (' + jobs.length + ')' +
        '</div>' +
        '<table class="w-full text-sm">' +
          '<thead class="text-xs text-zinc-500 bg-zinc-50/50 border-b border-zinc-200">' +
            '<tr>' +
              '<th class="px-4 py-2 text-left font-medium">Job ID</th>' +
              '<th class="px-4 py-2 text-left font-medium">Title</th>' +
              '<th class="px-4 py-2 text-left font-medium">Positions</th>' +
              '<th class="px-4 py-2 text-left font-medium">Applications</th>' +
              '<th class="px-4 py-2 text-left font-medium">Deadline</th>' +
              '<th class="px-4 py-2 text-left font-medium">Status</th>' +
            '</tr>' +
          '</thead>' +
          '<tbody class="divide-y divide-zinc-100">' + rows + '</tbody>' +
        '</table>' +
      '</div>' +
    '</td>' +
  '</tr>';
}

function renderModules(modules) {
  const tbody = document.getElementById('modulesBody');
  if (modules.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="p-8 text-center text-zinc-500">No modules found.</td></tr>';
    return;
  }
  tbody.innerHTML = modules.map(m => {
    const statusBadge = m.status === 'active' ? 'badge-success' : m.status === 'draft' ? 'badge-warning' : 'badge-neutral';
    const totalJobs = m.jobCount != null ? m.jobCount : (m.jobs ? m.jobs.length : 0);
    const activeJobs = m.activeJobCount != null ? m.activeJobCount : 0;
    const isOpen = expandedModules.has(m.id);
    const caretIcon = isOpen ? 'chevron-down' : 'chevron-right';

    const mainRow =
      '<tr class="table-row hover:bg-zinc-50/50 transition-colors cursor-pointer" onclick="toggleModuleJobs(\'' + m.id + '\')">' +
        '<td class="px-4 py-4 text-zinc-400"><i data-lucide="' + caretIcon + '" class="h-4 w-4"></i></td>' +
        '<td class="px-4 py-4 whitespace-nowrap"><div class="flex items-center gap-3">' +
          '<div class="h-8 w-8 rounded-md bg-zinc-100 flex items-center justify-center text-zinc-500"><i data-lucide="book-open" class="h-4 w-4"></i></div>' +
          '<div><div class="font-semibold text-zinc-900">' + escapeHtml(m.code) + '</div><div class="text-zinc-500 text-xs">' + escapeHtml(m.name || '') + '</div></div></div></td>' +
        '<td class="px-4 py-4 whitespace-nowrap text-zinc-700">' + escapeHtml(m.organiserName || m.organiserId || '-') + '</td>' +
        '<td class="px-4 py-4 whitespace-nowrap text-zinc-600">' + escapeHtml(m.department || '-') + '</td>' +
        '<td class="px-4 py-4 whitespace-nowrap text-zinc-600">' + escapeHtml(m.term || '-') + '</td>' +
        '<td class="px-4 py-4 whitespace-nowrap">' +
          '<div class="flex items-center gap-1.5">' +
            '<span class="badge badge-outline">' + totalJobs + ' total</span>' +
            (activeJobs > 0 ? '<span class="badge badge-success">' + activeJobs + ' active</span>' : '') +
          '</div>' +
        '</td>' +
        '<td class="px-4 py-4 whitespace-nowrap"><span class="badge ' + statusBadge + ' capitalize">' + escapeHtml(m.status || 'active') + '</span></td>' +
        '<td class="px-4 py-4 whitespace-nowrap text-right" onclick="event.stopPropagation()">' +
          '<div class="actions flex items-center justify-end gap-2">' +
            '<button onclick="editModule(\'' + m.id + '\')" class="btn btn-ghost p-2 text-zinc-500 hover:text-zinc-900"><i data-lucide="edit-2" class="h-4 w-4"></i></button>' +
            '<button onclick="deleteModule(\'' + m.id + '\')" class="btn btn-ghost p-2 text-zinc-500 hover:text-red-600"><i data-lucide="trash-2" class="h-4 w-4"></i></button>' +
          '</div>' +
        '</td>' +
      '</tr>';

    const jobsRow = isOpen ? renderJobsRow(m) : '';
    return mainRow + jobsRow;
  }).join('');
  lucide.createIcons();
}

function toggleModuleJobs(moduleId) {
  if (expandedModules.has(moduleId)) {
    expandedModules.delete(moduleId);
  } else {
    expandedModules.add(moduleId);
  }
  renderModules(allModules);
}

const teacherPicker = document.getElementById('teacherPicker');
const teacherSearch = document.getElementById('teacherSearch');
const teacherOptions = document.getElementById('teacherOptions');
const teacherStatus = document.getElementById('teacherStatus');
const teacherSummary = teacherPicker.querySelector('summary');
let teachers = [];
let selectedTeacher = null;
let teacherLoadStatus = '';
let teacherLoadVersion = 0;

function renderTeachers() {
  const query = teacherSearch.value.trim().toLowerCase();
  const matches = teachers.filter(teacher =>
    [teacher.name, teacher.email].some(value => (value || '').toLowerCase().includes(query)));
  teacherOptions.replaceChildren();
  for (const teacher of matches) {
    const option = document.createElement('button');
    option.type = 'button';
    option.className = 'block w-full rounded px-3 py-2 text-left hover:bg-blue-50 focus-visible:bg-blue-50';
    option.setAttribute('aria-pressed', String(selectedTeacher?.id === teacher.id));
    const name = document.createElement('span');
    name.className = 'block font-medium';
    name.textContent = teacher.name || teacher.email;
    const email = document.createElement('span');
    email.className = 'block text-xs text-zinc-500';
    email.textContent = teacher.email || '';
    option.append(name, email);
    option.addEventListener('click', () => {
      selectedTeacher = teacher;
      document.getElementById('selectedTeacherName').textContent = teacher.name || teacher.email;
      teacherPicker.open = false;
      teacherSummary.focus();
    });
    teacherOptions.append(option);
  }
  teacherStatus.textContent = teacherLoadStatus ||
    (matches.length ? '' : query ? 'No matching Teachers.' : 'No active Teachers available.');
}

function resetTeacherPicker(course = null) {
  const version = ++teacherLoadVersion;
  selectedTeacher = null;
  teacherPicker.open = false;
  teacherSearch.value = '';
  document.getElementById('selectedTeacherName').textContent =
    course?.organiserName || (course?.organiserId ? 'Current Teacher' : 'Choose a Teacher');
  teachers = [];
  teacherLoadStatus = 'Loading Teachers…';
  renderTeachers();
  adminApi.getUsers({role:'organiser', status:'active'}).then(users => {
    if (version !== teacherLoadVersion) return;
    teachers = users.sort((first, second) => (first.name || '').localeCompare(second.name || ''));
    const current = teachers.find(teacher => teacher.id === course?.organiserId);
    if (current && !selectedTeacher) {
      document.getElementById('selectedTeacherName').textContent = current.name || current.email;
    }
    teacherLoadStatus = '';
    renderTeachers();
  }).catch(() => {
    if (version !== teacherLoadVersion) return;
    teacherLoadStatus = 'Could not load Teachers. Reopen the course form to retry.';
    renderTeachers();
  });
}

teacherSearch.addEventListener('input', renderTeachers);
teacherPicker.addEventListener('toggle', () => {
  if (!teacherPicker.open) return;
  teacherSearch.value = '';
  renderTeachers();
  teacherSearch.focus();
});
teacherPicker.addEventListener('keydown', event => {
  if (event.key === 'Escape' && teacherPicker.open) {
    event.preventDefault();
    event.stopPropagation();
    teacherPicker.open = false;
    teacherSummary.focus();
  }
});
document.addEventListener('click', event => {
  if (!teacherPicker.contains(event.target)) teacherPicker.open = false;
});

async function openModuleModal() {
  document.getElementById('editModId').value = '';
  document.getElementById('modModalTitle').textContent = 'Add Module';
  document.getElementById('modSaveBtn').textContent = 'Create';
  ['mCode','mName'].forEach(id => document.getElementById(id).value = '');
  document.getElementById('modError').classList.add('hidden');
  await loadProperties();
  populatePropertyDropdowns('', '');
  resetTeacherPicker();
  document.getElementById('moduleModal').classList.add('show');
}

async function editModule(id) {
  const m = allModules.find(x => x.id === id);
  if (!m) return;
  document.getElementById('editModId').value = id;
  document.getElementById('modModalTitle').textContent = 'Edit Module';
  document.getElementById('modSaveBtn').textContent = 'Update';
  document.getElementById('mCode').value = m.code || '';
  document.getElementById('mName').value = m.name || '';
  document.getElementById('modError').classList.add('hidden');
  await loadProperties();
  populatePropertyDropdowns(m.department || '', m.term || '');
  resetTeacherPicker(m);
  document.getElementById('moduleModal').classList.add('show');
}

function closeModuleModal() { document.getElementById('moduleModal').classList.remove('show'); }

async function saveModule() {
  const errDiv = document.getElementById('modError');
  errDiv.classList.add('hidden');
  const id = document.getElementById('editModId').value;
  const dept = document.getElementById('mDept').value.trim();
  const term = document.getElementById('mTerm').value.trim();
  if (dept && allDepartments.length > 0 && !allDepartments.some(d => d.name === dept)) {
    errDiv.textContent = 'Please pick a department from the list.';
    errDiv.classList.remove('hidden');
    return;
  }
  if (term && allTerms.length > 0 && !allTerms.some(t => t.name === term)) {
    errDiv.textContent = 'Please pick a term from the list.';
    errDiv.classList.remove('hidden');
    return;
  }
  const payload = {
    code: document.getElementById('mCode').value.trim(),
    name: document.getElementById('mName').value.trim(),
    department: dept,
    term: term,
  };
  if (selectedTeacher) {
    payload.organiserId = selectedTeacher.id;
    if (!id) payload.organiserName = selectedTeacher.name || '';
  }
  try {
    if (id) { await adminApi.updateModule(id, payload); }
    else { await adminApi.createModule(payload); }
    closeModuleModal();
    showToast(id ? 'Module updated!' : 'Module created!');
    loadModules();
  } catch (e) { errDiv.textContent = e.message || 'Save failed'; errDiv.classList.remove('hidden'); }
}

async function deleteModule(id) {
  if (!confirm('Delete this module?')) return;
  try { await adminApi.deleteModule(id); showToast('Module deleted'); loadModules(); }
  catch (e) { showToast(e.message, 'error'); }
}

(async function init() {
  await loadProperties();
  await loadModules();
})();
</script>
