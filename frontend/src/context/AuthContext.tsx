import React, { createContext, useContext, useState, useEffect } from 'react';
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../types';
import { authService } from '../services/authService';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    // Rehydrate session from localStorage
    const storedToken = localStorage.getItem('smartprocure_token');
    const storedUser = localStorage.getItem('smartprocure_user');

    if (storedToken && storedUser) {
      try {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
      } catch (err) {
        console.error('Failed to parse cached user session:', err);
        localStorage.removeItem('smartprocure_token');
        localStorage.removeItem('smartprocure_user');
      }
    }
    setIsLoading(false);
  }, []);

  const handleAuthSuccess = (data: AuthResponse) => {
    const sessionUser: User = {
      id: data.id,
      username: data.username,
      email: data.email,
      role: data.role,
      departmentId: data.departmentId,
      departmentName: data.departmentName,
    };

    setToken(data.token);
    setUser(sessionUser);
    localStorage.setItem('smartprocure_token', data.token);
    localStorage.setItem('smartprocure_user', JSON.stringify(sessionUser));
  };

  const login = async (credentials: LoginRequest) => {
    setIsLoading(true);
    try {
      const data = await authService.login(credentials);
      handleAuthSuccess(data);
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: RegisterRequest) => {
    setIsLoading(true);
    try {
      const resp = await authService.register(data);
      handleAuthSuccess(resp);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('smartprocure_token');
    localStorage.removeItem('smartprocure_user');
    window.location.href = '/login';
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
