import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { STATUSES, fmt } from './Tickets.jsx';

// Must mirror TicketStatus.allowedTransitions() on the backend.
const ALLOWED = {
  OPEN: ['IN_PROGRESS', 'CLOSED'],
  IN_PROGRESS: ['OPEN', 'RESOLVED', 'CLOSED'],
  RESOLVED: ['IN_PROGRESS', 'CLOSED'],
  CLOSED: [],
};

export default function TicketDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isStaff, isAdmin } = useAuth();

  const [ticket, setTicket] = useState(null);
  const [comments, setComments] = useState([]);
  const [staff, setStaff] = useState([]);
  const [text, setText] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const [t, c] = await Promise.all([api.ticket(id), api.comments(id)]);
      setTicket(t);
      setComments(c);
    } catch (e) {
      setError(e.status === 403 ? 'You do not have access to this ticket.' : e.message);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { if (isStaff) api.staff().then(setStaff).catch(() => {}); }, [isStaff]);

  const run = async (action) => {
    setError('');
    try {
      await action();
      await load();
    } catch (e) {
      setError([e.message, ...(e.details || [])].join(' — '));
    }
  };

  const addComment = (e) => {
    e.preventDefault();
    run(async () => {
      await api.addComment(id, text);
      setText('');
    });
  };

  const remove = async () => {
    if (!window.confirm('Delete this ticket permanently?')) return;
    try {
      await api.deleteTicket(id);
      navigate('/');
    } catch (e) {
      setError(e.message);
    }
  };

  if (error && !ticket) return <p className="error">{error}</p>;
  if (!ticket) return <p className="muted">Loading…</p>;

  const closed = ticket.status === 'CLOSED';

  return (
    <>
      <div className="row between">
        <h1>#{ticket.id} — {ticket.title}</h1>
        {isAdmin && <button className="danger" onClick={remove}>Delete</button>}
      </div>
      {error && <p className="error">{error}</p>}

      <div className="card">
        <div className="row meta">
          <span className={`badge status-${ticket.status}`}>{ticket.status.replace('_', ' ')}</span>
          <span className={`badge prio-${ticket.priority}`}>{ticket.priority}</span>
          <span className="badge">{ticket.category}</span>
          <span className="muted">by {ticket.createdBy} · {fmt(ticket.createdAt)}</span>
        </div>
        <p className="description">{ticket.description}</p>
        <p className="muted">Assigned to: {ticket.assignedTo || 'nobody yet'} · Last update {fmt(ticket.updatedAt)}</p>

        {isStaff && (
          <div className="row staff-controls">
            <label>Status
              <select value={ticket.status} onChange={(e) => run(() => api.changeStatus(id, e.target.value))}>
                {STATUSES.filter((s) => s === ticket.status || (ALLOWED[ticket.status] || []).includes(s)).map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
            <label>Assign to
              <select
                value={staff.find((s) => s.username === ticket.assignedTo)?.id ?? ''}
                onChange={(e) => e.target.value && run(() => api.assign(id, Number(e.target.value)))}
              >
                <option value="">— choose —</option>
                {staff.map((s) => <option key={s.id} value={s.id}>{s.username} ({s.role})</option>)}
              </select>
            </label>
          </div>
        )}
      </div>

      <h2>Comments</h2>
      <div className="comments">
        {comments.length === 0 && <p className="muted">No comments yet.</p>}
        {comments.map((c) => (
          <div key={c.id} className="comment">
            <div className="muted"><strong>{c.author}</strong> · {fmt(c.createdAt)}</div>
            <p>{c.content}</p>
          </div>
        ))}
      </div>

      {closed ? (
        <p className="muted">This ticket is closed: no more comments.</p>
      ) : (
        <form className="card" onSubmit={addComment}>
          <label>Add a comment
            <textarea rows="3" value={text} onChange={(e) => setText(e.target.value)} maxLength={2000} required />
          </label>
          <button className="primary">Post comment</button>
        </form>
      )}
    </>
  );
}
