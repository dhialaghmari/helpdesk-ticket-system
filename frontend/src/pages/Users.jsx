import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { fmt } from './Tickets.jsx';

const ROLES = ['USER', 'AGENT', 'ADMIN'];

export default function Users() {
  const { user: me } = useAuth();
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api.users(page).then(setData).catch((e) => setError(e.message));
  }, [page]);

  useEffect(() => { load(); }, [load]);

  const run = async (action) => {
    setError('');
    try {
      await action();
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const remove = (u) => {
    if (window.confirm(`Delete user ${u.username}?`)) run(() => api.deleteUser(u.id));
  };

  return (
    <>
      <h1>Users</h1>
      {error && <p className="error">{error}</p>}
      {!data && !error && <p className="muted">Loading…</p>}
      {data && (
        <>
          <table>
            <thead><tr><th>#</th><th>Username</th><th>Email</th><th>Role</th><th>Created</th><th></th></tr></thead>
            <tbody>
              {data.content.map((u) => {
                const self = u.username === me.username;
                return (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.username}</td>
                    <td>{u.email}</td>
                    <td>
                      <select value={u.role} disabled={self} onChange={(e) => run(() => api.updateRole(u.id, e.target.value))}>
                        {ROLES.map((r) => <option key={r}>{r}</option>)}
                      </select>
                    </td>
                    <td>{fmt(u.createdAt)}</td>
                    <td>{!self && <button className="danger small" onClick={() => remove(u)}>Delete</button>}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          <div className="row pager">
            <button disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
            <span className="muted">Page {data.page.number + 1} of {Math.max(data.page.totalPages, 1)}</span>
            <button disabled={page + 1 >= data.page.totalPages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </>
      )}
    </>
  );
}
