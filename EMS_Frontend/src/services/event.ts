import axios from "axios";
import type {
  OrganiserEvent,
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
  // Organiser endpoints
  getOrganiserEvents: async (
    page = 0,
    size = 10,
  ): Promise<PaginatedResponse<OrganiserEvent>> => {
    const response = await eventApi.get("/api/organiser/events", {
      params: { page, size },
    });
    return response.data;
  },

  getOrganiserEvent: async (eventId: string): Promise<OrganiserEvent> => {
    const response = await eventApi.get(`/api/organiser/events/${eventId}`);
    return response.data;
  },

  createEvent: async (data: CreateEventRequest): Promise<OrganiserEvent> => {
    const response = await eventApi.post("/api/organiser/events", data);
    return response.data;
  },

  updateEvent: async (
    eventId: string,
    data: UpdateEventRequest,
  ): Promise<OrganiserEvent> => {
    const response = await eventApi.put(`/api/organiser/events/${eventId}`, data);
    return response.data;
  },

  cancelEvent: async (eventId: string): Promise<OrganiserEvent> => {
    const response = await eventApi.patch(
      `/api/organiser/events/${eventId}/cancel`,
    );
    return response.data;
  },

  publishEvent: async (eventId: string): Promise<OrganiserEvent> => {
    const response = await eventApi.patch(
      `/api/organiser/events/${eventId}/publish`,
    );
    return response.data;
  },

  getSalesSummary: async (eventId: string): Promise<SalesSummary> => {
    const response = await eventApi.get(`/api/organiser/events/${eventId}/summary`);
    return response.data;
  },

  // Tier endpoints
  createTier: async (
    eventId: string,
    data: CreateTierRequest,
  ): Promise<TicketTier> => {
    const response = await eventApi.post(
      `/api/organiser/events/${eventId}/tiers`,
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
      `/api/organiser/events/${eventId}/tiers/${tierId}`,
      data,
    );
    return response.data;
  },

  deleteTier: async (eventId: string, tierId: string): Promise<void> => {
    await eventApi.delete(`/api/organiser/events/${eventId}/tiers/${tierId}`);
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

  getPublicEvent: async (eventId: string): Promise<OrganiserEvent> => {
    const response = await eventApi.get(`/api/events/${eventId}`);
    return response.data;
  },
};
