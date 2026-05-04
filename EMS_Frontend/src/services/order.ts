import axios from 'axios';
import type { CancelOrderResponse, CreateOrderRequest, CreateOrderResponse, OrderHistoryResponse } from '@/types/order';

const orderApi = axios.create({
  baseURL: '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let _accessToken: string | null = null;

export const setOrderApiToken = (token: string | null) => {
  _accessToken = token;
};

orderApi.interceptors.request.use(
  (config) => {
    if (_accessToken) {
      config.headers.Authorization = `Bearer ${_accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

orderApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      setOrderApiToken(null);
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export const orderService = {
  createOrder: async (data: CreateOrderRequest): Promise<CreateOrderResponse> => {
    const response = await orderApi.post<CreateOrderResponse>('/api/orders', data);
    return response.data;
  },

  getMyBookings: async (page = 0, size = 20): Promise<OrderHistoryResponse> => {
    const response = await orderApi.get<OrderHistoryResponse>('/api/orders/my', {
      params: { page, size },
    });
    return response.data;
  },

  cancelOrder: async (orderId: number): Promise<CancelOrderResponse> => {
    const response = await orderApi.post<CancelOrderResponse>(`/api/orders/${orderId}/cancel`);
    return response.data;
  },
};
