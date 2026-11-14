import { useState } from 'react';
import { useAuth } from '../auth.jsx';
import { api } from '../api.js';

export default function Login() {
  const { login } = useAuth();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      if (mode === 'register') {
        await api.register(form);
      }
      await login(form.username, form.password);
    } catch (err) {
      setError([err.message, ...(err.details || [])].join(' — '));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card narrow" onSubmit={submit}>
      <h1>{mode === 'login' ? 'Sign in' : 'Create an account'}</h1>
      {error && <p className="error">{error}</p>}

      <label>Username
        <input value={form.username} onChange={set('username')} required autoFocus />
      </label>
      {mode === 'register' && (
        <label>Email
          <input type="email" value={form.email} onChange={set('email')} required />
        </label>
      )}
      <label>Password
        <input type="password" value={form.password} onChange={set('password')} required minLength={mode === 'register' ? 8 : undefined} />
      </label>

      <button className="primary" disabled={busy}>
        {mode === 'login' ? 'Sign in' : 'Register'}
      </button>
      <button type="button" className="link" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); }}>
        {mode === 'login' ? 'No account yet? Register' : 'Already registered? Sign in'}
      </button>
    </form>
  );
}
