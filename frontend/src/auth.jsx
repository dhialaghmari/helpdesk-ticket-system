import { createContext, useContext, useEffect, useState } from 'react';
import { api, clearAuth, getAuth, setAuth } from './api.js';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(getAuth()));

  // Restore the session after a page refresh.
  useEffect(() => {
    if (!getAuth()) return;
    api.me()
      .then(setUser)
      .catch(() => clearAuth())
      .finally(() => setLoading(false));
  }, []);

  const login = async (username, password) => {
    setAuth(username, password);
    try {
      setUser(await api.me());
    } catch (e) {
      clearAuth();
      throw e;
    }
  };

  const logout = () => {
    clearAuth();
    setUser(null);
  };

  const isStaff = user?.role === 'ADMIN' || user?.role === 'AGENT';
  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, isStaff, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}
