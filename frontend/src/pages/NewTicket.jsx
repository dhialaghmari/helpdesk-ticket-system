import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { CATEGORIES, PRIORITIES } from './Tickets.jsx';

export default function NewTicket() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: '', description: '', priority: 'MEDIUM', category: 'OTHER' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const ticket = await api.createTicket(form);
      navigate(`/tickets/${ticket.id}`);
    } catch (err) {
      setError([err.message, ...(err.details || [])].join(' — '));
      setBusy(false);
    }
  };

  return (
    <form className="card" onSubmit={submit}>
      <h1>New ticket</h1>
      {error && <p className="error">{error}</p>}
      <label>Title
        <input value={form.title} onChange={set('title')} maxLength={150} required autoFocus />
      </label>
      <label>Description
        <textarea rows="6" value={form.description} onChange={set('description')} maxLength={5000} required />
      </label>
      <div className="row">
        <label>Priority
          <select value={form.priority} onChange={set('priority')}>
            {PRIORITIES.map((p) => <option key={p}>{p}</option>)}
          </select>
        </label>
        <label>Category
          <select value={form.category} onChange={set('category')}>
            {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
          </select>
        </label>
      </div>
      <button className="primary" disabled={busy}>Submit ticket</button>
    </form>
  );
}
