import axios from "axios";
import type { AuthResponse, LoginRequest, RegisterRequest, User } from "@/types/auth";

const authApi = axios.create({
  baseURL: "",
  timeout: 30000,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

authApi.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url as string | undefined;
    const isAuthEndpoint = url === "/api/auth/login" || url === "/api/auth/me";
    if (error.response?.status === 401 && !isAuthEndpoint) {
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

export const authService = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await authApi.post<AuthResponse>("/api/auth/login", credentials);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await authApi.post<AuthResponse>("/api/auth/register", data);
    return response.data;
  },

  logout: async (): Promise<{ message: string }> => {
    const response = await authApi.post<{ message: string }>("/api/auth/logout");
    return response.data;
  },

  getMe: async (): Promise<User> => {
    const response = await authApi.get<User>("/api/auth/me");
    return response.data;
  },
};
