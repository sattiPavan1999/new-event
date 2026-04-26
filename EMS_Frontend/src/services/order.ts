import axios from 'axios';
import type { CreateOrderRequest, CreateOrderResponse, OrderHistoryResponse } from '@/types/order';

const orderApi = axios.create({
  baseURL: '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let getToken: (() => string | null) | null = null;

export const setupOrderApiInterceptor = (tokenGetter: () => string | null) => {
  getToken = tokenGetter;
  orderApi.interceptors.request.use(
    (config) => {
      const token = getToken?.();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error),
  );
};

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
};
