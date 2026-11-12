<%@ page pageEncoding="UTF-8" %>
<%@ include file="/common/auth-check.jsp" %>
<jsp:include page="/common/layout-top.jsp"/>

<div class="space-y-8 max-w-5xl mx-auto">
  <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
    <div>
      <h1 class="text-3xl font-bold tracking-tight text-zinc-900">Properties & Settings</h1>
      <p class="text-zinc-500 mt-2">Configure system-wide attributes and terminology.</p>
    </div>
  </div>

  <div class="space-y-8">
    <!-- Terms Section -->
    <div class="card fade-in">
      <div class="p-6 flex flex-row items-center justify-between border-b border-zinc-200">
        <div class="flex items-center gap-4">
          <div class="h-10 w-10 rounded-full bg-zinc-100 flex items-center justify-center">
            <i data-lucide="calendar" class="h-5 w-5 text-zinc-600"></i>
          </div>
          <div>
            <h2 class="text-lg font-semibold text-zinc-900">Global Terms</h2>
            <p class="text-sm text-zinc-500">Manage academic terms and registration periods.</p>
          </div>
        </div>
        <button onclick="addTerm()" class="btn btn-outline text-sm gap-2"><i data-lucide="plus" class="h-3.5 w-3.5"></i> Add New</button>
      </div>
      <div class="divide-y divide-zinc-200" id="termsList">
        <div class="p-6 text-center text-zinc-400">Loading...</div>
      </div>
    </div>

    <!-- Departments Section -->
    <div class="card fade-in">
      <div class="p-6 flex flex-row items-center justify-between border-b border-zinc-200">
        <div class="flex items-center gap-4">
          <div class="h-10 w-10 rounded-full bg-zinc-100 flex items-center justify-center">
            <i data-lucide="map-pin" class="h-5 w-5 text-zinc-600"></i>
          </div>
          <div>
            <h2 class="text-lg font-semibold text-zinc-900">Departments & Faculties</h2>
            <p class="text-sm text-zinc-500">Define organizational structures for module categorization.</p>
          </div>
        </div>
        <button onclick="addDepartment()" class="btn btn-outline text-sm gap-2"><i data-lucide="plus" class="h-3.5 w-3.5"></i> Add New</button>
      </div>
      <div class="divide-y divide-zinc-200" id="deptsList">
        <div class="p-6 text-center text-zinc-400">Loading...</div>
      </div>
    </div>

    <!-- Roles Section (static) -->
    <div class="card fade-in">
      <div class="p-6 flex flex-row items-center justify-between border-b border-zinc-200">
        <div class="flex items-center gap-4">
          <div class="h-10 w-10 rounded-full bg-zinc-100 flex items-center justify-center">
            <i data-lucide="shield" class="h-5 w-5 text-zinc-600"></i>
          </div>
          <div>
            <h2 class="text-lg font-semibold text-zinc-900">Role Designations</h2>
            <p class="text-sm text-zinc-500">System roles available for staff and applicants.</p>
          </div>
        </div>
      </div>
      <div class="divide-y divide-zinc-200">
        <div class="flex items-center justify-between py-4 px-6 hover:bg-zinc-50/50 transition-colors">
          <div class="flex items-center gap-4"><span class="font-medium text-zinc-900">Teacher</span><span class="badge badge-success">Active</span></div>
          <span class="text-sm text-zinc-500">Manage TA hiring for assigned modules</span>
        </div>
        <div class="flex items-center justify-between py-4 px-6 hover:bg-zinc-50/50 transition-colors">
          <div class="flex items-center gap-4"><span class="font-medium text-zinc-900">admin</span><span class="badge badge-success">Active</span></div>
          <span class="text-sm text-zinc-500">Platform-wide configuration and overview</span>
        </div>
        <div class="flex items-center justify-between py-4 px-6 hover:bg-zinc-50/50 transition-colors">
          <div class="flex items-center gap-4"><span class="font-medium text-zinc-900">Teaching Assistant</span><span class="badge badge-success">Active</span></div>
          <span class="text-sm text-zinc-500">Applicant role</span>
        </div>
      </div>
    </div>
  </div>
</div>

<jsp:include page="/common/layout-bottom.jsp"/>
<script>
async function loadProperties() {
  try {
    const [termsData, deptsData] = await Promise.all([adminApi.getTerms(), adminApi.getDepartments()]);
    const terms = termsData.terms || termsData || [];
    const depts = deptsData.departments || deptsData || [];

    document.getElementById('termsList').innerHTML = terms.length === 0 ? '<div class="p-6 text-center text-zinc-400">No terms found</div>' :
      terms.map(t =>
        '<div class="flex items-center justify-between py-4 px-6 hover:bg-zinc-50/50 transition-colors group">' +
          '<div class="flex items-center gap-4"><span class="font-medium text-zinc-900">' + t.name + '</span>' +
          '<span class="badge ' + (t.status === 'active' ? 'badge-success' : 'badge-neutral') + ' capitalize">' + (t.status || 'active') + '</span></div>' +
          '<span class="text-sm text-zinc-500">' + (t.startDate || '') + ' - ' + (t.endDate || '') + '</span></div>'
      ).join('');

    document.getElementById('deptsList').innerHTML = depts.length === 0 ? '<div class="p-6 text-center text-zinc-400">No departments found</div>' :
      depts.map(d =>
        '<div class="flex items-center justify-between py-4 px-6 hover:bg-zinc-50/50 transition-colors group">' +
          '<div class="flex items-center gap-4"><span class="font-medium text-zinc-900">' + d.name + '</span>' +
          '<span class="badge badge-success">Active</span></div></div>'
      ).join('');
  } catch (e) { console.error('Failed to load properties', e); }
}

async function addTerm() {
  const name = prompt('Term name (e.g. Fall 2026):');
  if (!name) return;
  const start = prompt('Start date (YYYY-MM-DD):');
  const end = prompt('End date (YYYY-MM-DD):');
  try {
    await adminApi.createTerm({ name, startDate: start, endDate: end });
    showToast('Term created!');
    loadProperties();
  } catch (e) { showToast(e.message, 'error'); }
}

async function addDepartment() {
  const name = prompt('Department name:');
  if (!name) return;
  try {
    await adminApi.createDepartment({ name });
    showToast('Department created!');
    loadProperties();
  } catch (e) { showToast(e.message, 'error'); }
}

loadProperties();
</script>
