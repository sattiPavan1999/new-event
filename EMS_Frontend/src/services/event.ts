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
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

eventApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

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

  getOrganiserEvent: async (eventId: number): Promise<OrganiserEvent> => {
    const response = await eventApi.get(`/api/organiser/events/${eventId}`);
    return response.data;
  },

  createEvent: async (data: CreateEventRequest): Promise<OrganiserEvent> => {
    const response = await eventApi.post("/api/organiser/events", data);
    return response.data;
  },

  updateEvent: async (
    eventId: number,
    data: UpdateEventRequest,
  ): Promise<OrganiserEvent> => {
    const response = await eventApi.put(`/api/organiser/events/${eventId}`, data);
    return response.data;
  },

  cancelEvent: async (eventId: number): Promise<OrganiserEvent> => {
    const response = await eventApi.patch(`/api/organiser/events/${eventId}/cancel`);
    return response.data;
  },

  publishEvent: async (eventId: number): Promise<OrganiserEvent> => {
    const response = await eventApi.patch(`/api/organiser/events/${eventId}/publish`);
    return response.data;
  },

  getSalesSummary: async (eventId: number): Promise<SalesSummary> => {
    const response = await eventApi.get(`/api/organiser/events/${eventId}/summary`);
    return response.data;
  },

  // Tier endpoints
  createTier: async (
    eventId: number,
    data: CreateTierRequest,
  ): Promise<TicketTier> => {
    const response = await eventApi.post(`/api/organiser/events/${eventId}/tiers`, data);
    return response.data;
  },

  updateTier: async (
    eventId: number,
    tierId: number,
    data: UpdateTierRequest,
  ): Promise<TicketTier> => {
    const response = await eventApi.put(
      `/api/organiser/events/${eventId}/tiers/${tierId}`,
      data,
    );
    return response.data;
  },

  deleteTier: async (eventId: number, tierId: number): Promise<void> => {
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

  getPublicEvent: async (eventId: number): Promise<OrganiserEvent> => {
    const response = await eventApi.get(`/api/events/${eventId}`);
    return response.data;
  },
};
