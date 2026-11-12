<%@ page pageEncoding="UTF-8" %>
<%@ include file="/common/auth-check.jsp" %>
<jsp:include page="/common/layout-top.jsp"/>

<div class="space-y-8 max-w-6xl mx-auto">
  <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
    <div>
      <h1 class="text-3xl font-bold tracking-tight text-zinc-900">User Management</h1>
      <p class="text-zinc-500 mt-2">Manage accounts, roles, and access permissions across the platform.</p>
    </div>
    <button onclick="openUserModal()" class="btn btn-primary gap-2 shrink-0">
      <i data-lucide="plus" class="h-4 w-4"></i> Invite User
    </button>
  </div>

  <div class="flex flex-col sm:flex-row gap-4 justify-between items-center">
    <div class="relative w-full sm:w-96">
      <i data-lucide="search" class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400"></i>
      <input id="searchInput" type="text" placeholder="Search by name or email..." class="input pl-10" onkeyup="filterUsers()">
    </div>
    <select id="roleFilter" class="input w-auto" onchange="loadUsers()">
      <option value="">All Roles</option>
      <option value="applicant">Applicant</option>
      <option value="organiser">Teacher</option><option value="reviewer">Advanced review role: Reviewer</option><option value="decision_maker">Advanced review role: Decision-maker</option><option value="viewer">Advanced review role: Viewer</option>
      <option value="admin">admin</option>
    </select>
  </div>

  <div class="card overflow-hidden">
    <div class="overflow-x-auto">
      <table class="w-full text-sm text-left">
        <thead class="text-xs text-zinc-500 bg-zinc-50/50 border-b border-zinc-200 uppercase tracking-wider">
          <tr>
            <th class="px-6 py-4 font-medium">User Details</th>
            <th class="px-6 py-4 font-medium">Role</th>
            <th class="px-6 py-4 font-medium">Status</th>
            <th class="px-6 py-4 font-medium">Last Login</th>
            <th class="px-6 py-4 font-medium text-right">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-zinc-200" id="usersBody">
          <tr><td colspan="5" class="p-6 text-center text-zinc-400">Loading...</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</div>

<!-- User Modal -->
<div id="userModal" data-modal class="fixed inset-0 z-50 items-center justify-center bg-black/50 backdrop-blur-sm">
  <div class="bg-white rounded-2xl shadow-xl max-w-lg w-full mx-4">
    <div class="bg-zinc-50 border-b border-zinc-200/60 p-6">
      <h2 class="text-xl font-bold text-zinc-900" id="userModalTitle">Create User</h2>
    </div>
    <div class="p-6 space-y-4">
      <input type="hidden" id="editUserId">
      <div><label for="uName" class="block text-sm font-medium text-zinc-700 mb-1.5">Name</label><input id="uName" class="input" placeholder="Full name"></div>
      <div><label for="uEmail" class="block text-sm font-medium text-zinc-700 mb-1.5">Email</label><input id="uEmail" type="email" class="input" placeholder="name@university.edu"></div>
      <div><label for="uPassword" class="block text-sm font-medium text-zinc-700 mb-1.5">Password</label><input id="uPassword" type="password" class="input" placeholder="••••••••"></div>
      <div class="grid grid-cols-2 gap-4">
        <div><label for="uRole" class="block text-sm font-medium text-zinc-700 mb-1.5">Role</label>
          <select id="uRole" class="input"><option value="applicant">Applicant</option><option value="organiser">Teacher</option><option value="reviewer">Advanced review role: Reviewer</option><option value="decision_maker">Advanced review role: Decision-maker</option><option value="viewer">Advanced review role: Viewer</option><option value="admin">admin</option></select>
        </div>
        <div><label for="uStatus" class="block text-sm font-medium text-zinc-700 mb-1.5">Status</label>
          <select id="uStatus" class="input"><option value="active">Active</option><option value="suspended">Suspended</option></select>
        </div>
      </div>
      <div id="userError" class="hidden text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-4 py-3"></div>
    </div>
    <div class="p-6 border-t border-zinc-200 flex justify-end gap-3">
      <button onclick="closeUserModal()" class="btn btn-secondary">Cancel</button>
      <button onclick="saveUser()" class="btn btn-primary" id="userSaveBtn">Create</button>
    </div>
  </div>
</div>

<jsp:include page="/common/layout-bottom.jsp"/>
<script>
let allUsers = [];

async function loadUsers() {
  try {
    const params = {};
    const role = document.getElementById('roleFilter').value;
    if (role) params.role = role;
    const data = await adminApi.getUsers(Object.keys(params).length ? params : undefined);
    allUsers = data.users || data || [];
    renderUsers(allUsers);
  } catch (e) { console.error('Failed to load users', e); }
}

function filterUsers() {
  const q = document.getElementById('searchInput').value.toLowerCase();
  renderUsers(allUsers.filter(u => (u.name || '').toLowerCase().includes(q) || (u.email || '').toLowerCase().includes(q)));
}

function renderUsers(users) {
  const tbody = document.getElementById('usersBody');
  if (users.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="p-8 text-center text-zinc-500">No users found.</td></tr>';
    return;
  }
  const roleIcons = { admin: 'shield', organiser: 'briefcase', applicant: 'user' };
  const roleColors = { admin: 'text-purple-500', organiser: 'text-blue-500', applicant: 'text-emerald-500' };
  tbody.innerHTML = users.map(original => {
    const u=escapeDisplay(original);
    const statusBadge = u.status === 'active' ? 'badge-success' : 'badge-neutral';
    return '<tr class="table-row hover:bg-zinc-50/50 transition-colors">' +
      '<td class="px-6 py-4 whitespace-nowrap"><div class="flex items-center gap-3">' +
        '<div class="h-9 w-9 rounded-full bg-zinc-100 border border-zinc-200 flex items-center justify-center font-bold text-zinc-600">' + (u.name || 'U').charAt(0) + '</div>' +
        '<div><div class="font-semibold text-zinc-900">' + (u.name || '') + '</div><div class="text-zinc-500 text-xs flex items-center gap-1 mt-0.5"><i data-lucide="mail" class="h-3 w-3"></i> ' + (u.email || '') + '</div></div></div></td>' +
      '<td class="px-6 py-4 whitespace-nowrap"><div class="flex items-center gap-2 text-zinc-700"><i data-lucide="' + (roleIcons[u.role] || 'user') + '" class="h-4 w-4 ' + (roleColors[u.role] || '') + '"></i><span>' + displayRole(u.role) + '</span></div></td>' +
      '<td class="px-6 py-4 whitespace-nowrap"><span class="badge ' + statusBadge + ' capitalize">' + (u.status || 'active') + '</span></td>' +
      '<td class="px-6 py-4 whitespace-nowrap text-zinc-500 text-xs">' + (u.lastLogin || '-') + '</td>' +
      '<td class="px-6 py-4 whitespace-nowrap text-right"><div class="actions flex items-center justify-end gap-2">' +
        '<button onclick="editUser(\'' + u.id + '\')" class="btn btn-ghost p-2 text-zinc-500 hover:text-zinc-900"><i data-lucide="edit-2" class="h-4 w-4"></i></button>' +
        '<button onclick="deleteUser(\'' + u.id + '\')" class="btn btn-ghost p-2 text-zinc-500 hover:text-red-600"><i data-lucide="trash-2" class="h-4 w-4"></i></button>' +
      '</div></td></tr>';
  }).join('');
  lucide.createIcons();
}

function openUserModal() {
  document.getElementById('editUserId').value = '';
  document.getElementById('userModalTitle').textContent = 'Create User';
  document.getElementById('userSaveBtn').textContent = 'Create';
  ['uName','uEmail','uPassword'].forEach(id => document.getElementById(id).value = '');
  document.getElementById('uRole').value = 'applicant';
  document.getElementById('uStatus').value = 'active';
  document.getElementById('userModal').classList.add('show');
}

function editUser(id) {
  const u = allUsers.find(x => x.id === id);
  if (!u) return;
  document.getElementById('editUserId').value = id;
  document.getElementById('userModalTitle').textContent = 'Edit User';
  document.getElementById('userSaveBtn').textContent = 'Update';
  document.getElementById('uName').value = u.name || '';
  document.getElementById('uEmail').value = u.email || '';
  document.getElementById('uPassword').value = '';
  document.getElementById('uRole').value = u.role || 'applicant';
  document.getElementById('uStatus').value = u.status || 'active';
  document.getElementById('userModal').classList.add('show');
}

function closeUserModal() { document.getElementById('userModal').classList.remove('show'); }

async function saveUser() {
  const errDiv = document.getElementById('userError');
  errDiv.classList.add('hidden');
  const id = document.getElementById('editUserId').value;
  const payload = {
    name: document.getElementById('uName').value.trim(),
    email: document.getElementById('uEmail').value.trim(),
    role: document.getElementById('uRole').value,
    status: document.getElementById('uStatus').value,
  };
  const pwd = document.getElementById('uPassword').value;
  if (pwd) payload.password = pwd;
  try {
    if (id) { await adminApi.updateUser(id, payload); }
    else { if (!pwd) throw { message: 'Password is required' }; payload.password = pwd; await adminApi.createUser(payload); }
    closeUserModal();
    showToast(id ? 'User updated!' : 'User created!');
    loadUsers();
  } catch (e) {
    errDiv.textContent = e.message || 'Save failed';
    errDiv.classList.remove('hidden');
  }
}

async function deleteUser(id) {
  if (!confirm('Delete this user?')) return;
  try { await adminApi.deleteUser(id); showToast('User deleted'); loadUsers(); }
  catch (e) { showToast(e.message, 'error'); }
}

loadUsers();
</script>
