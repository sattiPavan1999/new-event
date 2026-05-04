import axios from "axios";
import type { AuthResponse, LoginRequest, RegisterRequest } from "@/types/auth";

const authApi = axios.create({
  baseURL: "",
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

let _accessToken: string | null = null;

export const setAuthToken = (token: string | null) => {
  _accessToken = token;
};

authApi.interceptors.request.use(
  (config) => {
    if (_accessToken) {
      config.headers.Authorization = `Bearer ${_accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

authApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && error.config?.url !== "/api/auth/login") {
      setAuthToken(null);
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

export const authService = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await authApi.post<AuthResponse>(
      "/api/auth/login",
      credentials,
    );
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await authApi.post<AuthResponse>(
      "/api/auth/register",
      data,
    );
    return response.data;
  },

  logout: async (): Promise<{ message: string }> => {
    const response = await authApi.post<{ message: string }>("/api/auth/logout");
    return response.data;
  },
};
