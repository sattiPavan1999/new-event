import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { User, AuthResponse } from '@/types/auth';
import { setupAuthInterceptor } from '@/services/auth';
import { setupEventApiInterceptor } from '@/services/event';
import { setupOrderApiInterceptor } from '@/services/order';

interface AuthContextType {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
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
  const [refreshToken, setRefreshToken] = useState<string | null>(() =>
    localStorage.getItem('refreshToken')
  );

  useEffect(() => {
    setupAuthInterceptor(() => accessToken);
    setupEventApiInterceptor(() => accessToken, () => user?.id ?? null);
    setupOrderApiInterceptor(() => accessToken);
  }, [accessToken, user]);

  const login = (authData: AuthResponse) => {
    setUser(authData.user);
    setAccessToken(authData.accessToken);
    setRefreshToken(authData.refreshToken);

    localStorage.setItem('user', JSON.stringify(authData.user));
    localStorage.setItem('accessToken', authData.accessToken);
    localStorage.setItem('refreshToken', authData.refreshToken);
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
    setRefreshToken(null);

    localStorage.removeItem('user');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  };

  const value = {
    user,
    accessToken,
    refreshToken,
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
