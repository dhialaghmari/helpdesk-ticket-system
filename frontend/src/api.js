const KEY = 'helpdesk_auth';

export const getAuth = () => sessionStorage.getItem(KEY);
export const clearAuth = () => sessionStorage.removeItem(KEY);
export const setAuth = (username, password) =>
  sessionStorage.setItem(KEY, 'Basic ' + btoa(unescape(encodeURIComponent(`${username}:${password}`))));

async function request(path, { method = 'GET', body } = {}) {
  // X-Requested-With stops the browser from opening its native Basic-auth popup on 401.
  const headers = { 'X-Requested-With': 'XMLHttpRequest' };
  const auth = getAuth();
  if (auth) headers.Authorization = auth;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch('/api' + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return null;

  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const message = res.status === 401 ? 'Invalid username or password' : data?.message || res.statusText;
    const err = new Error(message);
    err.status = res.status;
    err.details = data?.details || [];
    throw err;
  }
  return data;
}

const qs = (params) => {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => v !== '' && v != null && p.set(k, v));
  const s = p.toString();
  return s ? `?${s}` : '';
};

export const api = {
  register: (body) => request('/auth/register', { method: 'POST', body }),
  me: () => request('/users/me'),
  staff: () => request('/users/staff'),
  users: (page = 0) => request(`/users?page=${page}&size=20`),
  updateRole: (id, role) => request(`/users/${id}/role`, { method: 'PUT', body: { role } }),
  deleteUser: (id) => request(`/users/${id}`, { method: 'DELETE' }),

  tickets: (params) => request('/tickets' + qs(params)),
  ticket: (id) => request(`/tickets/${id}`),
  createTicket: (body) => request('/tickets', { method: 'POST', body }),
  updateTicket: (id, body) => request(`/tickets/${id}`, { method: 'PUT', body }),
  changeStatus: (id, status) => request(`/tickets/${id}/status`, { method: 'PATCH', body: { status } }),
  assign: (id, assigneeId) => request(`/tickets/${id}/assignee`, { method: 'PATCH', body: { assigneeId } }),
  deleteTicket: (id) => request(`/tickets/${id}`, { method: 'DELETE' }),
  stats: () => request('/tickets/stats'),
  comments: (id) => request(`/tickets/${id}/comments`),
  addComment: (id, content) => request(`/tickets/${id}/comments`, { method: 'POST', body: { content } }),
};
