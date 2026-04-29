import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { User, AuthResponse } from '@/types/auth';
import { setAuthToken } from '@/services/auth';
import { setEventApiToken } from '@/services/event';
import { setOrderApiToken } from '@/services/order';

interface AuthContextType {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  login: (authData: AuthResponse) => void;
  logout: () => void;
  updateWalletBalance: (newBalance: number) => void;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const stored = localStorage.getItem('user');
    return stored ? (JSON.parse(stored) as User) : null;
  });
  const [accessToken, setAccessToken] = useState<string | null>(() =>
    localStorage.getItem('accessToken')
  );

  // Sync persisted token into service modules on first render
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    setAuthToken(token);
    setEventApiToken(token);
    setOrderApiToken(token);
  }, []);

  const login = (authData: AuthResponse) => {
    setUser(authData.user);
    setAccessToken(authData.accessToken);
    setAuthToken(authData.accessToken);
    setEventApiToken(authData.accessToken);
    setOrderApiToken(authData.accessToken);

    localStorage.setItem('user', JSON.stringify(authData.user));
    localStorage.setItem('accessToken', authData.accessToken);
  };

  const updateWalletBalance = (newBalance: number) => {
    setUser((prev) => {
      if (!prev) return prev;
      const updated = { ...prev, walletBalance: newBalance };
      localStorage.setItem('user', JSON.stringify(updated));
      return updated;
    });
  };

  const logout = () => {
    setUser(null);
    setAccessToken(null);
    setAuthToken(null);
    setEventApiToken(null);
    setOrderApiToken(null);

    localStorage.removeItem('user');
    localStorage.removeItem('accessToken');
  };

  const value = {
    user,
    accessToken,
    isAuthenticated: !!user && !!accessToken,
    login,
    logout,
    updateWalletBalance,
    isLoading: false,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
