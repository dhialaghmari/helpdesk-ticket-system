import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';

export const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const CATEGORIES = ['HARDWARE', 'SOFTWARE', 'NETWORK', 'ACCOUNT', 'OTHER'];

export const fmt = (iso) => new Date(iso).toLocaleString();

export default function Tickets() {
  const { isStaff } = useAuth();
  const [filters, setFilters] = useState({ status: '', priority: '', category: '' });
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.tickets({ ...filters, page, size: 10, sort: 'createdAt,desc' })
      .then(setData)
      .catch((e) => setError(e.message));
  }, [filters, page]);

  useEffect(() => {
    if (isStaff) api.stats().then(setStats).catch(() => {});
  }, [isStaff]);

  const setFilter = (field) => (e) => {
    setPage(0);
    setFilters({ ...filters, [field]: e.target.value });
  };

  return (
    <>
      <div className="row between">
        <h1>{isStaff ? 'All tickets' : 'My tickets'}</h1>
        <Link to="/tickets/new" className="button primary">New ticket</Link>
      </div>

      {stats && (
        <div className="stats">
          {STATUSES.map((s) => (
            <div key={s} className="stat">
              <strong>{stats[s] ?? 0}</strong>
              <span>{s.replace('_', ' ')}</span>
            </div>
          ))}
        </div>
      )}

      <div className="row filters">
        <select value={filters.status} onChange={setFilter('status')}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s}>{s}</option>)}
        </select>
        <select value={filters.priority} onChange={setFilter('priority')}>
          <option value="">All priorities</option>
          {PRIORITIES.map((s) => <option key={s}>{s}</option>)}
        </select>
        <select value={filters.category} onChange={setFilter('category')}>
          <option value="">All categories</option>
          {CATEGORIES.map((s) => <option key={s}>{s}</option>)}
        </select>
      </div>

      {error && <p className="error">{error}</p>}
      {!data && !error && <p className="muted">Loading…</p>}

      {data && (
        <>
          <table>
            <thead>
              <tr><th>#</th><th>Title</th><th>Status</th><th>Priority</th><th>Category</th><th>Created by</th><th>Assigned to</th><th>Created</th></tr>
            </thead>
            <tbody>
              {data.content.map((t) => (
                <tr key={t.id}>
                  <td>{t.id}</td>
                  <td><Link to={`/tickets/${t.id}`}>{t.title}</Link></td>
                  <td><span className={`badge status-${t.status}`}>{t.status.replace('_', ' ')}</span></td>
                  <td><span className={`badge prio-${t.priority}`}>{t.priority}</span></td>
                  <td>{t.category}</td>
                  <td>{t.createdBy}</td>
                  <td>{t.assignedTo || <span className="muted">—</span>}</td>
                  <td>{fmt(t.createdAt)}</td>
                </tr>
              ))}
              {data.content.length === 0 && (
                <tr><td colSpan="8" className="muted center">No tickets found</td></tr>
              )}
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
