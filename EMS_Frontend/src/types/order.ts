export interface OrderItemRequest {
  tierId: number;
  quantity: number;
}

export interface CreateOrderRequest {
  eventId: number;
  items: OrderItemRequest[];
}

export interface OrderItemResponse {
  tierId: number;
  quantity: number;
  price: number;
}

export interface CreateOrderResponse {
  orderId: number;
  status: string;
  totalAmount: number;
  items: OrderItemResponse[];
  remainingBalance?: number;
}

export interface ConfirmedOrder {
  orderId: number;
  status: string;
  eventTitle: string;
  totalAmount: number;
  items: Array<{
    tierName: string;
    quantity: number;
    unitPrice: number;
  }>;
}

export interface OrderConfirmationState {
  orders: ConfirmedOrder[];
  grandTotal: number;
}

export interface OrderItemSummary {
  orderItemId: number;
  tierName: string;
  eventTitle: string;
  eventDate: string;
  quantity: number;
  unitPrice: number;
  venueName: string | null;
}

export interface OrderSummary {
  orderId: number;
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

export interface CancelOrderResponse {
  orderId: number;
  status: string;
  message: string;
  remainingBalance?: number;
}
