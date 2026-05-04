export interface User {
  id: number;
  email: string;
  fullName: string;
  role: 'BUYER' | 'ORGANISER';
  isActive: boolean;
  createdAt?: string;
  walletBalance?: number;
}

export interface AuthResponse {
  accessToken: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  fullName: string;
  password: string;
  role: 'BUYER' | 'ORGANISER';
}

