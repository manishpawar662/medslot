import { createContext, useContext, useState, type ReactNode } from 'react';
import type { AuthResponse, UserRole } from '../types/api';

interface AuthUser {
  userId: string;
  email: string;
  role: UserRole;
  token: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  loginSuccess: (auth: AuthResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_USER_KEY = 'medslot_user';
const STORAGE_TOKEN_KEY = 'medslot_token';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem(STORAGE_USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  });

  const loginSuccess = (auth: AuthResponse) => {
    const authUser: AuthUser = {
      userId: auth.userId,
      email: auth.email,
      role: auth.role,
      token: auth.accessToken,
    };
    localStorage.setItem(STORAGE_TOKEN_KEY, auth.accessToken);
    localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(authUser));
    setUser(authUser);
  };

  const logout = () => {
    localStorage.removeItem(STORAGE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loginSuccess, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}