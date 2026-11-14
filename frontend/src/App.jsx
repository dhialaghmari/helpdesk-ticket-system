import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Login from './pages/Login.jsx';
import Tickets from './pages/Tickets.jsx';
import NewTicket from './pages/NewTicket.jsx';
import TicketDetail from './pages/TicketDetail.jsx';
import Users from './pages/Users.jsx';

function Protected({ children, adminOnly = false }) {
  const { user, loading, isAdmin } = useAuth();
  if (loading) return <p className="muted">Loading…</p>;
  if (!user) return <Navigate to="/login" replace />;
  if (adminOnly && !isAdmin) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  const { user, logout, isAdmin } = useAuth();

  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">IT Helpdesk</Link>
        {user && (
          <nav>
            <Link to="/">Tickets</Link>
            <Link to="/tickets/new">New ticket</Link>
            {isAdmin && <Link to="/users">Users</Link>}
            <span className="who">
              {user.username} <span className={`badge role-${user.role}`}>{user.role}</span>
            </span>
            <button className="link" onClick={logout}>Log out</button>
          </nav>
        )}
      </header>
      <main>
        <Routes>
          <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
          <Route path="/" element={<Protected><Tickets /></Protected>} />
          <Route path="/tickets/new" element={<Protected><NewTicket /></Protected>} />
          <Route path="/tickets/:id" element={<Protected><TicketDetail /></Protected>} />
          <Route path="/users" element={<Protected adminOnly><Users /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
