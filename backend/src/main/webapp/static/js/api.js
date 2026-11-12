const CTX = document.querySelector('meta[name="ctx"]')?.content || '';
const BASE = CTX + '/api';

async function request(url, options = {}) {
  const resp = await fetch(BASE + url, {
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  const data = await resp.json().catch(() => ({}));
  if (!resp.ok) throw { status: resp.status, message: data.error || 'HTTP ' + resp.status };
  return data;
}

const authApi = {
  login: (email, password) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  signup: (name, email, password) =>
    request('/auth/signup', { method: 'POST', body: JSON.stringify({ name, email, password, role: 'applicant' }) }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  me: () => request('/auth/me'),
};

const applicantApi = {
  getDashboard: (periodId) => request('/applicant/dashboard'+(periodId?'?periodId='+encodeURIComponent(periodId):'')),
  getApplicationPeriod: () => request('/applicant/application-period'),
  getAvailabilityPeriods: () => request('/applicant/availability-periods'),
  getAvailability: (periodId) => request('/applicant/availability/' + encodeURIComponent(periodId)),
  saveAvailability: (periodId, data) => request('/applicant/availability/' + encodeURIComponent(periodId), { method:'PUT', body:JSON.stringify(data) }),
  getNotifications: () => request('/applicant/notifications'),
  markNotificationRead: (id) => request('/applicant/notifications/' + id + '/read', { method: 'PUT' }),
  markAllNotificationsRead: () => request('/applicant/notifications/read-all', { method: 'PUT' }),
  getJobs: (params) => {
    const q = params ? '?' + new URLSearchParams(params).toString() : '';
    return request('/applicant/jobs' + q);
  },
  getJobById: (id) => request('/applicant/jobs/' + id),
  applyForJob: (jobId, coverLetter, documentIds, periodId) =>
    request('/applicant/jobs/' + jobId + '/apply', { method: 'POST', body: JSON.stringify({ coverLetter, documentIds, periodId }) }),
  getApplications: (periodId) => request('/applicant/applications' + (periodId ? '?periodId=' + encodeURIComponent(periodId) : '')),
  getApplicationById: (id) => request('/applicant/applications/' + id),
  withdrawApplication: (id) => request('/applicant/applications/' + id, { method: 'DELETE' }),
  getProfile: () => request('/applicant/profile'),
  updateProfile: (data) => request('/applicant/profile', { method: 'PUT', body: JSON.stringify(data) }),
  uploadDocument: (fileName, fileType, fileSize) =>
    request('/applicant/profile/documents', { method: 'POST', body: JSON.stringify({ fileName, fileType, fileSize }) }),
  deleteDocument: (docId) => request('/applicant/profile/documents/' + docId, { method: 'DELETE' }),
};

const organiserApi = {
  getDashboard: () => request('/organiser/dashboard'),
  getMyModules: () => request('/organiser/modules'),
  getJobs: () => request('/organiser/jobs'),
  createJob: (data) => request('/organiser/jobs', { method: 'POST', body: JSON.stringify(data) }),
  updateJob: (id, data) => request('/organiser/jobs/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  deleteJob: (id) => request('/organiser/jobs/' + id, { method: 'DELETE' }),
  getApplicationPeriods: () => request('/organiser/application-periods'),
  getPipeline: (module, periodId) => {
    const params = new URLSearchParams();
    if (module && module !== 'All') params.set('module', module);
    if (periodId) params.set('periodId', periodId);
    const q = params.size ? '?' + params.toString() : '';
    return request('/organiser/pipeline' + q);
  },
  updateCandidateStatus: (appId, status, note) =>
    request('/organiser/pipeline/' + appId + '/status', { method: 'PUT', body: JSON.stringify({ status, note }) }),
  getApplicationDetails: (id) => request('/organiser/applications/' + id),
  applicationAction: (id, action, note) =>
    request('/organiser/applications/' + id + '/action', { method: 'PUT', body: JSON.stringify({ action, note }) }),
  getNotifications: () => request('/organiser/notifications'),
  markNotificationRead: (id) => request('/organiser/notifications/' + id + '/read', { method: 'PUT' }),
  markAllNotificationsRead: () => request('/organiser/notifications/read-all', { method: 'PUT' }),
};

const adminApi = {
  getApplicationPeriods: () => request('/admin/application-periods'),
  createApplicationPeriod: (data) => request('/admin/application-periods', { method: 'POST', body: JSON.stringify(data) }),
  updateApplicationPeriod: (id, data) => request('/admin/application-periods/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  changeApplicationPeriodStatus: (id, action) => request('/admin/application-periods/' + id + '/' + action, { method: 'POST' }),
  getPeriodApplications: (id) => request('/admin/application-periods/' + id + '/applications'),
  getDashboard: () => request('/admin/dashboard'),
  getUsers: (params) => {
    const q = params ? '?' + new URLSearchParams(params).toString() : '';
    return request('/admin/users' + q);
  },
  createUser: (data) => request('/admin/users', { method: 'POST', body: JSON.stringify(data) }),
  updateUser: (id, data) => request('/admin/users/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  deleteUser: (id) => request('/admin/users/' + id, { method: 'DELETE' }),
  getModules: (keyword) => request('/admin/modules' + (keyword ? '?keyword=' + keyword : '')),
  createModule: (data) => request('/admin/modules', { method: 'POST', body: JSON.stringify(data) }),
  updateModule: (id, data) => request('/admin/modules/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  deleteModule: (id) => request('/admin/modules/' + id, { method: 'DELETE' }),
  getTerms: () => request('/admin/properties/terms'),
  createTerm: (data) => request('/admin/properties/terms', { method: 'POST', body: JSON.stringify(data) }),
  getDepartments: () => request('/admin/properties/departments'),
  createDepartment: (data) => request('/admin/properties/departments', { method: 'POST', body: JSON.stringify(data) }),
  getLogs: (params) => {
    const q = params ? '?' + new URLSearchParams(params).toString() : '';
    return request('/admin/logs' + q);
  },
  getWorkload: (params) => {
    const q = params ? '?' + new URLSearchParams(params).toString() : '';
    return request('/admin/workload' + q);
  },
  reassignWorkload: (taId, data) =>
    request('/admin/workload/' + taId + '/reassign', { method: 'PUT', body: JSON.stringify(data) }),
};

const messageApi = {
  getContacts: () => request('/messages/contacts'),
  getConversations: () => request('/messages/conversations'),
  getMessages: (conversationId) => request('/messages/conversations/' + conversationId),
  sendMessage: (payload) => request('/messages/send', { method: 'POST', body: JSON.stringify(payload) }),
  editMessage: (messageId, content) => request('/messages/edit/' + messageId, { method: 'PUT', body: JSON.stringify({ content }) }),
  recallMessage: (messageId) => request('/messages/recall/' + messageId, { method: 'PUT' }),
  markRead: (conversationId) => request('/messages/read/' + conversationId, { method: 'PUT' }),
  getUnreadCount: () => request('/messages/unread-count'),
  getUserInfo: (userId) => request('/messages/user-info/' + userId),
  uploadFile: (file) => {
    const form = new FormData();
    form.append('file', file);
    return fetch(BASE + '/messages/upload', {
      method: 'POST',
      credentials: 'same-origin',
      body: form,
    }).then(async (resp) => {
      const data = await resp.json().catch(() => ({}));
      if (!resp.ok) throw { status: resp.status, message: data.error || 'HTTP ' + resp.status };
      return data;
    });
  },
};

const dataEditorApi = {
  getCollections: () => request('/admin/data-editor/collections'),
  getData: (collection) => request('/admin/data-editor/' + collection),
  createItem: (collection, data) => request('/admin/data-editor/' + collection, { method: 'POST', body: JSON.stringify(data) }),
  updateItem: (collection, id, data) => request('/admin/data-editor/' + collection + '/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  deleteItem: (collection, id) => request('/admin/data-editor/' + collection + '/' + id, { method: 'DELETE' }),
};

function showToast(msg, type) {
  const t = document.createElement('div');
  t.className = 'fixed top-4 right-4 z-50 px-4 py-3 rounded-lg shadow-lg text-sm font-medium fade-in ' +
    (type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' : 'bg-emerald-50 text-emerald-800 border border-emerald-200');
  t.textContent = msg;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3000);
}
