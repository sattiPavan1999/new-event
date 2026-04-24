import axios from "axios";
import type {
  AdminEvent,
  EventSummary,
  PaginatedResponse,
  CreateEventRequest,
  UpdateEventRequest,
  TicketTier,
  CreateTierRequest,
  UpdateTierRequest,
  SalesSummary,
  Venue,
} from "@/types/event";

const eventApi = axios.create({
  baseURL: "",
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

let getToken: (() => string | null) | null = null;
let getUserId: (() => string | null) | null = null;

export const setupEventApiInterceptor = (
  tokenGetter: () => string | null,
  userIdGetter?: () => string | null,
) => {
  getToken = tokenGetter;
  if (userIdGetter) getUserId = userIdGetter;
  eventApi.interceptors.request.use(
    (config) => {
      const token = getToken?.();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      const userId = getUserId?.();
      if (userId) {
        config.headers["X-User-Id"] = userId;
      }
      return config;
    },
    (error) => Promise.reject(error),
  );
};

export const eventService = {
  // Admin endpoints
  getAdminEvents: async (
    page = 0,
    size = 10,
  ): Promise<PaginatedResponse<AdminEvent>> => {
    const response = await eventApi.get("/api/admin/events", {
      params: { page, size },
    });
    return response.data;
  },

  getAdminEvent: async (eventId: string): Promise<AdminEvent> => {
    const response = await eventApi.get(`/api/admin/events/${eventId}`);
    return response.data;
  },

  createEvent: async (data: CreateEventRequest): Promise<AdminEvent> => {
    const response = await eventApi.post("/api/admin/events", data);
    return response.data;
  },

  updateEvent: async (
    eventId: string,
    data: UpdateEventRequest,
  ): Promise<AdminEvent> => {
    const response = await eventApi.put(`/api/admin/events/${eventId}`, data);
    return response.data;
  },

  cancelEvent: async (eventId: string): Promise<AdminEvent> => {
    const response = await eventApi.patch(
      `/api/admin/events/${eventId}/cancel`,
    );
    return response.data;
  },

  publishEvent: async (eventId: string): Promise<AdminEvent> => {
    const response = await eventApi.patch(
      `/api/admin/events/${eventId}/publish`,
    );
    return response.data;
  },

  getSalesSummary: async (eventId: string): Promise<SalesSummary> => {
    const response = await eventApi.get(`/api/admin/events/${eventId}/summary`);
    return response.data;
  },

  // Tier endpoints
  createTier: async (
    eventId: string,
    data: CreateTierRequest,
  ): Promise<TicketTier> => {
    const response = await eventApi.post(
      `/api/admin/events/${eventId}/tiers`,
      data,
    );
    return response.data;
  },

  updateTier: async (
    eventId: string,
    tierId: string,
    data: UpdateTierRequest,
  ): Promise<TicketTier> => {
    const response = await eventApi.put(
      `/api/admin/events/${eventId}/tiers/${tierId}`,
      data,
    );
    return response.data;
  },

  deleteTier: async (eventId: string, tierId: string): Promise<void> => {
    await eventApi.delete(`/api/admin/events/${eventId}/tiers/${tierId}`);
  },

  getVenues: async (): Promise<Venue[]> => {
    const response = await eventApi.get("/api/venues");
    return response.data;
  },

  // Public endpoints
  getPublicEvents: async (params: {
    category?: string;
    city?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Promise<PaginatedResponse<EventSummary>> => {
    const response = await eventApi.get("/api/events", { params });
    return response.data;
  },

  getPublicEvent: async (eventId: string): Promise<AdminEvent> => {
    const response = await eventApi.get(`/api/events/${eventId}`);
    return response.data;
  },
};
