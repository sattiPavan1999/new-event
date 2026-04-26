export interface OrderItemRequest {
  tierId: string;
  quantity: number;
}

export interface CreateOrderRequest {
  eventId: string;
  items: OrderItemRequest[];
}

export interface OrderItemResponse {
  tierId: string;
  quantity: number;
  price: number;
}

export interface CreateOrderResponse {
  orderId: string;
  status: string;
  totalAmount: number;
  items: OrderItemResponse[];
}

export interface OrderConfirmationState {
  orderId: string;
  status: string;
  eventTitle: string;
  totalAmount: number;
  items: Array<{
    tierName: string;
    quantity: number;
    unitPrice: number;
  }>;
}

export interface OrderItemSummary {
  orderItemId: string;
  tierName: string;
  eventTitle: string;
  eventDate: string;
  quantity: number;
  unitPrice: number;
  venueName: string | null;
}

export interface OrderSummary {
  orderId: string;
  status: string;
  totalAmount: number;
  createdAt: string;
  items: OrderItemSummary[];
}

export interface OrderHistoryResponse {
  content: OrderSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
