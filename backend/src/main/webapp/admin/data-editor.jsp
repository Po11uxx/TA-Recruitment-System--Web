<%@ page pageEncoding="UTF-8" %>
<%@ include file="/common/auth-check.jsp" %>
<jsp:include page="/common/layout-top.jsp"/>

<div class="space-y-6 max-w-[90rem] mx-auto">
  <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
    <div>
      <h1 class="text-3xl font-bold tracking-tight text-zinc-900">Data Editor</h1>
      <p class="text-zinc-500 mt-2">Directly edit all data collections in the system with full CRUD operations.</p>
    </div>
    <div class="flex items-center gap-3">
      <button onclick="refreshData()" class="btn btn-secondary gap-2">
        <i data-lucide="refresh-cw" class="h-4 w-4"></i> Refresh
      </button>
      <button id="newDataRecord" onclick="openCreateModal()" class="btn btn-primary gap-2">
        <i data-lucide="plus" class="h-4 w-4"></i> New Record
      </button>
    </div>
  </div>

  <p class="text-sm text-zinc-500">Manage accounts and account access on the <a class="underline" href="<%= request.getContextPath() %>/admin/users.jsp">Users page</a>.</p>

  <!-- Collection Tabs -->
  <div class="border-b border-zinc-200">
    <nav class="flex gap-1 overflow-x-auto pb-px scrollbar-hide" id="collectionTabs">
      <button class="tab-btn active" data-collection="profiles">Profiles</button>
      <button class="tab-btn" data-collection="jobs">Jobs</button>
      <button class="tab-btn" data-collection="applications">Applications</button>
      <button class="tab-btn" data-collection="modules">Modules</button>
      <button class="tab-btn" data-collection="notifications">Notifications</button>
      <button class="tab-btn" data-collection="terms">Terms</button>
      <button class="tab-btn" data-collection="departments">Departments</button>
      <button class="tab-btn" data-collection="logs">Logs</button>
      <button class="tab-btn" data-collection="workloadAssignments">Workload</button>
    </nav>
  </div>

  <!-- Search & Stats -->
  <div class="flex flex-col sm:flex-row gap-4 justify-between items-center">
    <div class="relative w-full sm:w-96">
      <i data-lucide="search" class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400"></i>
      <input id="searchInput" type="text" placeholder="Search in current collection..." class="input pl-10" oninput="filterTable()">
    </div>
    <div class="flex items-center gap-4 text-sm text-zinc-500">
      <span id="recordCount">0 records</span>
      <span class="text-zinc-300">|</span>
      <span id="currentCollection" class="font-medium text-zinc-700">profiles</span>
    </div>
  </div>

  <!-- Data Table -->
  <div class="card overflow-hidden">
    <div class="overflow-x-auto">
      <table class="w-full text-sm text-left" id="dataTable">
        <thead class="text-xs text-zinc-500 bg-zinc-50/50 border-b border-zinc-200 uppercase tracking-wider" id="tableHead">
          <tr><th class="px-4 py-3">Loading...</th></tr>
        </thead>
        <tbody class="divide-y divide-zinc-100" id="tableBody">
          <tr><td class="px-4 py-8 text-center text-zinc-400">Select a collection to view data</td></tr>
        </tbody>
      </table>
    </div>
  </div>

  <!-- Pagination -->
  <div class="flex items-center justify-between text-sm text-zinc-500" id="paginationBar">
    <span id="showingText"></span>
    <div class="flex gap-2">
      <button onclick="prevPage()" class="btn btn-secondary btn-sm" id="prevBtn" disabled>Previous</button>
      <button onclick="nextPage()" class="btn btn-secondary btn-sm" id="nextBtn" disabled>Next</button>
    </div>
  </div>
</div>

<!-- Edit/Create Modal -->
<div id="editorModal" data-modal class="fixed inset-0 z-50 items-center justify-center bg-black/50 backdrop-blur-sm">
  <div class="bg-white rounded-2xl shadow-xl max-w-2xl w-full mx-4 max-h-[85vh] flex flex-col">
    <div class="bg-zinc-50 border-b border-zinc-200/60 p-6 flex items-center justify-between shrink-0">
      <h2 class="text-xl font-bold text-zinc-900" id="modalTitle">Edit Record</h2>
      <button onclick="closeEditorModal()" class="p-2 hover:bg-zinc-200 rounded-lg transition-colors">
        <i data-lucide="x" class="h-5 w-5 text-zinc-500"></i>
      </button>
    </div>
    <div id="modalWarning" class="hidden px-6 pt-4"></div>
    <div class="p-6 space-y-4 overflow-y-auto flex-1" id="modalFields"></div>
    <div class="p-6 border-t border-zinc-200 flex justify-between items-center shrink-0">
      <div id="modalError" class="hidden text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2 max-w-sm truncate"></div>
      <div class="flex gap-3 ml-auto">
        <button onclick="closeEditorModal()" class="btn btn-secondary">Cancel</button>
        <button onclick="saveRecord()" class="btn btn-primary" id="modalSaveBtn">Save</button>
      </div>
    </div>
  </div>
</div>

<!-- Delete Confirmation Modal -->
<div id="deleteModal" data-modal class="fixed inset-0 z-50 items-center justify-center bg-black/50 backdrop-blur-sm">
  <div class="bg-white rounded-2xl shadow-xl max-w-lg w-full mx-4">
    <div class="p-6">
      <div class="flex items-start gap-4">
        <div class="h-12 w-12 rounded-full bg-red-100 flex items-center justify-center shrink-0">
          <i data-lucide="alert-triangle" class="h-6 w-6 text-red-600"></i>
        </div>
        <div class="flex-1">
          <h3 class="text-lg font-bold text-zinc-900" id="deleteTitle">Confirm Deletion</h3>
          <p class="text-sm text-zinc-600 mt-2" id="deleteMessage">Are you sure you want to delete this record?</p>
          <div id="deleteCascadeInfo" class="hidden mt-3 p-3 bg-amber-50 border border-amber-200 rounded-lg">
            <p class="text-sm font-medium text-amber-800 flex items-center gap-2">
              <i data-lucide="alert-circle" class="h-4 w-4"></i>
              <span>Cascading Impact</span>
            </p>
            <ul class="mt-2 text-xs text-amber-700 space-y-1" id="deleteCascadeList"></ul>
          </div>
        </div>
      </div>
    </div>
    <div class="p-6 border-t border-zinc-200 flex justify-end gap-3">
      <button onclick="closeDeleteModal()" class="btn btn-secondary">Cancel</button>
      <button onclick="confirmDelete()" class="btn bg-red-600 text-white hover:bg-red-700" id="deleteConfirmBtn">Delete</button>
    </div>
  </div>
</div>

<!-- JSON Viewer Modal -->
<div id="jsonModal" data-modal class="fixed inset-0 z-50 items-center justify-center bg-black/50 backdrop-blur-sm">
  <div class="bg-white rounded-2xl shadow-xl max-w-3xl w-full mx-4 max-h-[85vh] flex flex-col">
    <div class="bg-zinc-50 border-b border-zinc-200/60 p-6 flex items-center justify-between shrink-0">
      <h2 class="text-xl font-bold text-zinc-900">JSON View</h2>
      <button onclick="document.getElementById('jsonModal').classList.remove('show')" class="p-2 hover:bg-zinc-200 rounded-lg transition-colors">
        <i data-lucide="x" class="h-5 w-5 text-zinc-500"></i>
      </button>
    </div>
    <div class="p-6 overflow-y-auto flex-1">
      <pre class="bg-zinc-900 text-emerald-400 p-4 rounded-xl text-xs font-mono overflow-x-auto whitespace-pre-wrap" id="jsonContent"></pre>
    </div>
  </div>
</div>

<style>
.tab-btn {
  padding: 0.5rem 1rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #71717a;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: all 0.15s;
}
.tab-btn:hover { color: #18181b; }
.tab-btn.active {
  color: #18181b;
  border-bottom-color: #18181b;
}
.scrollbar-hide::-webkit-scrollbar { display: none; }
.scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
.cell-truncate { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.btn-sm { padding: 0.375rem 0.75rem; font-size: 0.75rem; }
</style>

<jsp:include page="/common/layout-bottom.jsp"/>
<script>
const COLLECTION_SCHEMAS = {
  profiles: { fields: ['id','userId','studentId','degree','year','gpa','maxGpa','profileCompletion','skills','coursework','experience','documents','maxWeeklyHours','currentWeeklyHours'], key: 'id', display: ['id','userId','studentId','degree','year','gpa','skills','documents'] },
  jobs: { fields: ['id','moduleId','moduleCode','title','description','professor','department','type','hoursPerWeek','positions','deadline','status','tags','requirements','responsibilities','workDates','organiserId','createdAt'], key: 'id', display: ['id','moduleCode','title','department','type','positions','status','deadline','workDates'] },
  applications: { fields: ['id','jobId','applicantId','status','coverLetter','selectedDocIds','matchScore','riskFlag','aiSummary','appliedAt','updatedAt'], key: 'id', display: ['id','jobId','applicantId','status','matchScore','riskFlag','appliedAt'] },
  modules: { fields: ['id','code','name','department','organiserId','organiserName','term','status'], key: 'id', display: ['id','code','name','department','organiserName','term','status'] },
  notifications: { fields: ['id','userId','type','title','message','relatedId','createdAt','read'], key: 'id', display: ['id','userId','type','title','read','createdAt'] },
  terms: { fields: ['id','name','startDate','endDate','status'], key: 'id', display: ['id','name','startDate','endDate','status'] },
  departments: { fields: ['id','name','moduleCount','status'], key: 'id', display: ['id','name','moduleCount','status'] },
  logs: { fields: ['id','timestamp','action','actor','actorId','type','details','ip'], key: 'id', display: ['id','timestamp','action','actor','type','details'] },
  workloadAssignments: { fields: ['id','taUserId','moduleCode','hoursPerWeek','term','status'], key: 'id', display: ['id','taUserId','moduleCode','hoursPerWeek','term','status'] }
};

let currentCollection = 'profiles';
let allData = [];
let filteredData = [];
let currentPage = 0;
const PAGE_SIZE = 20;
let editingId = null;
let pendingDeleteId = null;

document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentCollection = btn.dataset.collection;
    document.getElementById('currentCollection').textContent = currentCollection;
    document.getElementById('searchInput').value = '';
    currentPage = 0;
    loadCollectionData();
  });
});

async function loadCollectionData() {
  try {
    const data = await request('/admin/data-editor/' + currentCollection);
    allData = Array.isArray(data) ? data : [];
    filteredData = allData;
    renderTable();
  } catch (e) {
    console.error('Load failed', e);
    document.getElementById('tableBody').innerHTML = '<tr><td colspan="99" class="px-4 py-8 text-center text-red-500">Failed to load: ' + (e.message || '') + '</td></tr>';
  }
}

function filterTable() {
  const q = document.getElementById('searchInput').value.toLowerCase();
  if (!q) { filteredData = allData; }
  else {
    filteredData = allData.filter(item =>
      Object.values(item).some(v => String(v).toLowerCase().includes(q))
    );
  }
  currentPage = 0;
  renderTable();
}

function renderTable() {
  const readOnly = ['modules','jobs','applications','workloadAssignments','logs'].includes(currentCollection);
  document.getElementById('newDataRecord').classList.toggle('hidden',readOnly);
  const schema = COLLECTION_SCHEMAS[currentCollection];
  const displayFields = schema.display;

  const thead = document.getElementById('tableHead');
  thead.innerHTML = '<tr>' +
    displayFields.map(f => '<th class="px-4 py-3 font-medium">' + f + '</th>').join('') +
    '<th class="px-4 py-3 font-medium text-right">Actions</th></tr>';

  const start = currentPage * PAGE_SIZE;
  const pageData = filteredData.slice(start, start + PAGE_SIZE);
  const tbody = document.getElementById('tableBody');

  if (pageData.length === 0) {
    tbody.innerHTML = '<tr><td colspan="' + (displayFields.length + 1) + '" class="px-4 py-8 text-center text-zinc-400">No records found</td></tr>';
  } else {
    tbody.innerHTML = pageData.map(item => {
      const cells = displayFields.map(f => {
        let val = item[f];
        if (val === null || val === undefined) val = '<span class="text-zinc-300">null</span>';
        else if (typeof val === 'boolean') val = val ? '<span class="text-emerald-600">true</span>' : '<span class="text-zinc-400">false</span>';
        else if (Array.isArray(val)) val = '<span class="text-blue-600 text-xs">[' + val.length + ' items]</span>';
        else if (typeof val === 'object') val = '<span class="text-purple-600 text-xs">{object}</span>';
        else {
          val = escapeHtml(String(val));
          if (String(val).length > 40) val = val.substring(0, 40) + '...';
        }
        return '<td class="px-4 py-3 cell-truncate">' + val + '</td>';
      }).join('');
      const id = item[schema.key] || '';
      return '<tr class="hover:bg-zinc-50/70 transition-colors">' + cells +
        '<td class="px-4 py-3 text-right whitespace-nowrap">' +
          '<button onclick="viewJson(\'' + escapeAttr(id) + '\')" class="p-1.5 text-zinc-400 hover:text-blue-600 rounded" title="View JSON"><i data-lucide="code" class="h-4 w-4"></i></button>' +
          (readOnly ? '' : '<button onclick="editRecord(\'' + escapeAttr(id) + '\')" class="p-1.5 text-zinc-400 hover:text-zinc-900 rounded" title="Edit"><i data-lucide="edit-2" class="h-4 w-4"></i></button>' +
          '<button onclick="deleteRecord(\'' + escapeAttr(id) + '\')" class="p-1.5 text-zinc-400 hover:text-red-600 rounded" title="Delete"><i data-lucide="trash-2" class="h-4 w-4"></i></button>') +
        '</td></tr>';
    }).join('');
  }

  document.getElementById('recordCount').textContent = filteredData.length + ' records';
  const totalPages = Math.ceil(filteredData.length / PAGE_SIZE);
  document.getElementById('showingText').textContent =
    filteredData.length > 0 ? 'Showing ' + (start + 1) + '-' + Math.min(start + PAGE_SIZE, filteredData.length) + ' of ' + filteredData.length : '';
  document.getElementById('prevBtn').disabled = currentPage === 0;
  document.getElementById('nextBtn').disabled = currentPage >= totalPages - 1;

  lucide.createIcons();
}

function prevPage() { if (currentPage > 0) { currentPage--; renderTable(); } }
function nextPage() { const totalPages = Math.ceil(filteredData.length / PAGE_SIZE); if (currentPage < totalPages - 1) { currentPage++; renderTable(); } }

function openCreateModal() {
  editingId = null;
  document.getElementById('modalTitle').textContent = 'Create Record - ' + currentCollection;
  document.getElementById('modalSaveBtn').textContent = 'Create';
  document.getElementById('modalError').classList.add('hidden');
  document.getElementById('modalWarning').classList.add('hidden');
  const schema = COLLECTION_SCHEMAS[currentCollection];
  renderModalFields(schema.fields, {});
  showCreateWarnings();
  document.getElementById('editorModal').classList.add('show');
}

function showCreateWarnings() {
  const warningDiv = document.getElementById('modalWarning');
  let msg = '';
  if (currentCollection === 'jobs') {
    msg = 'A valid moduleId (existing module) and organiserId (existing organiser user) are required.';
  } else if (currentCollection === 'applications') {
    msg = 'A valid jobId (existing job) and applicantId (existing applicant user) are required.';

  } else if (currentCollection === 'workloadAssignments') {
    msg = 'The taUserId must reference an existing user and moduleCode must match an existing module.';
  } else if (currentCollection === 'profiles') {
    msg = 'The userId must reference an existing user with role "applicant".';
  } else if (currentCollection === 'notifications') {
    msg = 'The userId must reference an existing user.';

  } else if (currentCollection === 'logs') {
    msg = 'actorId (if set) must reference an existing user.';
  }
  if (msg) {
    warningDiv.innerHTML = '<div class="p-3 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-800 flex items-start gap-2"><i data-lucide="info" class="h-4 w-4 mt-0.5 shrink-0"></i><span>' + msg + '</span></div>';
    warningDiv.classList.remove('hidden');
    lucide.createIcons();
  } else {
    warningDiv.classList.add('hidden');
  }
}

function editRecord(id) {
  const item = allData.find(x => x[COLLECTION_SCHEMAS[currentCollection].key] === id);
  if (!item) return;
  editingId = id;
  document.getElementById('modalTitle').textContent = 'Edit Record - ' + id;
  document.getElementById('modalSaveBtn').textContent = 'Update';
  document.getElementById('modalError').classList.add('hidden');
  document.getElementById('modalWarning').classList.add('hidden');
  const schema = COLLECTION_SCHEMAS[currentCollection];
  renderModalFields(schema.fields, item);
  document.getElementById('editorModal').classList.add('show');
}

function renderModalFields(fields, data) {
  const container = document.getElementById('modalFields');
  container.innerHTML = fields.map(f => {
    const val = data[f];
    const isArray = Array.isArray(val);
    const isObj = val !== null && typeof val === 'object' && !isArray;
    const displayVal = isArray || isObj ? JSON.stringify(val, null, 2) : (val != null ? val : '');
    const isLong = isArray || isObj || String(displayVal).length > 80;
    const idAttr = 'field_' + f;
    const readonlyAttr = (editingId && (f === 'id' || (currentCollection === 'applications' && f === 'status'))) ? 'readonly' : '';

    if (isLong) {
      return '<div><label class="block text-sm font-medium text-zinc-700 mb-1.5">' + f +
        (isArray ? ' <span class="text-xs text-blue-500">(JSON array)</span>' : '') +
        (isObj ? ' <span class="text-xs text-purple-500">(JSON object)</span>' : '') +
        '</label><textarea id="' + idAttr + '" class="input font-mono text-xs min-h-[100px]" ' + readonlyAttr + '>' + escapeHtml(String(displayVal)) + '</textarea></div>';
    }
    if (typeof val === 'boolean') {
      return '<div class="flex items-center gap-3"><label class="text-sm font-medium text-zinc-700">' + f + '</label>' +
        '<select id="' + idAttr + '" class="input w-auto"><option value="true"' + (val ? ' selected' : '') + '>true</option><option value="false"' + (!val ? ' selected' : '') + '>false</option></select></div>';
    }
    return '<div><label class="block text-sm font-medium text-zinc-700 mb-1.5">' + f + '</label>' +
      '<input id="' + idAttr + '" class="input" value="' + escapeAttr(String(displayVal)) + '" ' + readonlyAttr + '></div>';
  }).join('');
}

function closeEditorModal() { document.getElementById('editorModal').classList.remove('show'); }

async function saveRecord() {
  const schema = COLLECTION_SCHEMAS[currentCollection];
  const errDiv = document.getElementById('modalError');
  errDiv.classList.add('hidden');

  const payload = {};
  schema.fields.forEach(f => {
    const el = document.getElementById('field_' + f);
    if (!el) return;
    let val = el.value;
    if (val === '') return;
    if (val === 'true') { payload[f] = true; return; }
    if (val === 'false') { payload[f] = false; return; }
    try {
      const parsed = JSON.parse(val);
      if (Array.isArray(parsed) || (typeof parsed === 'object' && parsed !== null)) {
        payload[f] = parsed;
        return;
      }
    } catch(e) {}
    if (!isNaN(Number(val)) && val.trim() !== '' && !val.includes('@') && !val.includes('-') && !val.includes('/') && f !== 'id' && f !== 'phone' && f !== 'password') {
      if (Number.isInteger(Number(val))) payload[f] = parseInt(val);
      else payload[f] = parseFloat(val);
    } else {
      payload[f] = val;
    }
  });

  try {
    if (editingId) {
      await request('/admin/data-editor/' + currentCollection + '/' + editingId, { method: 'PUT', body: JSON.stringify(payload) });
      showToast('Record updated successfully');
    } else {
      await request('/admin/data-editor/' + currentCollection, { method: 'POST', body: JSON.stringify(payload) });
      showToast('Record created successfully');
    }
    closeEditorModal();
    loadCollectionData();
  } catch (e) {
    errDiv.textContent = e.message || 'Operation failed';
    errDiv.classList.remove('hidden');
  }
}

async function deleteRecord(id) {
  pendingDeleteId = id;
  const titleEl = document.getElementById('deleteTitle');
  const msgEl = document.getElementById('deleteMessage');
  const cascadeDiv = document.getElementById('deleteCascadeInfo');
  const cascadeList = document.getElementById('deleteCascadeList');

  titleEl.textContent = 'Delete from ' + currentCollection;
  msgEl.textContent = 'You are about to permanently delete record "' + id + '". This action cannot be undone.';

  const impacts = getCascadeImpact(id);
  if (impacts.length > 0) {
    cascadeList.innerHTML = impacts.map(i => '<li class="flex items-start gap-1.5"><span class="text-amber-500 font-bold">*</span> ' + i + '</li>').join('');
    cascadeDiv.classList.remove('hidden');
  } else {
    cascadeDiv.classList.add('hidden');
  }

  document.getElementById('deleteModal').classList.add('show');
  lucide.createIcons();
}

function getCascadeImpact(id) {
  const impacts = [];

  if (currentCollection === 'modules') {
    const item = allData.find(x => x.id === id);
    if (item) {
      const jobs = allCollectionCache.jobs || [];
      const relatedJobs = jobs.filter(j => j.moduleId === id || j.moduleCode === item.code);
      if (relatedJobs.length) {
        impacts.push(relatedJobs.length + ' job(s) under this module will be deleted');
        const apps = allCollectionCache.applications || [];
        const jobIds = relatedJobs.map(j => j.id);
        const relatedApps = apps.filter(a => jobIds.includes(a.jobId));
        if (relatedApps.length) impacts.push(relatedApps.length + ' application(s) for those jobs will be deleted');
        const appIds = relatedApps.map(a => a.id);
      }
      const workload = allCollectionCache.workloadAssignments || [];
      const relatedWorkload = workload.filter(w => w.moduleCode === item.code);
      if (relatedWorkload.length) impacts.push(relatedWorkload.length + ' workload assignment(s) will be deleted');
    }

  } else if (currentCollection === 'jobs') {
    const apps = allCollectionCache.applications || [];
    const relatedApps = apps.filter(a => a.jobId === id);
    if (relatedApps.length) {
      impacts.push(relatedApps.length + ' application(s) for this job will be deleted');
      const appIds = relatedApps.map(a => a.id);
    }

  } else if (currentCollection === 'applications') {

  } else if (currentCollection === 'departments') {
    const item = allData.find(x => x.id === id);
    if (item) {
      const modules = allCollectionCache.modules || [];
      const relatedModules = modules.filter(m => m.department === item.name);
      if (relatedModules.length) impacts.push(relatedModules.length + ' module(s) in this department (department field will become orphaned)');
    }

  } else if (currentCollection === 'terms') {
    const item = allData.find(x => x.id === id);
    if (item) {
      const modules = allCollectionCache.modules || [];
      const relatedModules = modules.filter(m => m.term === item.name);
      if (relatedModules.length) impacts.push(relatedModules.length + ' module(s) referencing this term (term field will become orphaned)');
    }
  }

  return impacts;
}

function closeDeleteModal() {
  document.getElementById('deleteModal').classList.remove('show');
  pendingDeleteId = null;
}

async function confirmDelete() {
  if (!pendingDeleteId) return;
  const id = pendingDeleteId;
  closeDeleteModal();
  try {
    await request('/admin/data-editor/' + currentCollection + '/' + id + '?cascade=true', { method: 'DELETE' });
    showToast('Record deleted successfully');
    loadCollectionData();
    preloadRelatedCollections();
  } catch (e) {
    showToast(e.message || 'Delete failed', 'error');
  }
}

function viewJson(id) {
  const item = allData.find(x => x[COLLECTION_SCHEMAS[currentCollection].key] === id);
  if (!item) return;
  document.getElementById('jsonContent').textContent = JSON.stringify(item, null, 2);
  document.getElementById('jsonModal').classList.add('show');
}

function refreshData() {
  loadCollectionData();
  preloadRelatedCollections();
}

let allCollectionCache = {};

async function preloadRelatedCollections() {
  const collections = ['users','profiles','modules','jobs','applications','notifications','terms','departments','logs','workloadAssignments'];
  for (const c of collections) {
    try {
      const data = await request('/admin/data-editor/' + c);
      allCollectionCache[c] = Array.isArray(data) ? data : [];
    } catch(e) { allCollectionCache[c] = []; }
  }
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(str));
  return div.innerHTML;
}
function escapeAttr(str) {
  return str.replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/'/g,'&#39;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

loadCollectionData();
preloadRelatedCollections();
</script>
