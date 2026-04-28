import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderService } from '@/services/order';
import { useAuth } from '@/contexts/AuthContext';
import { BuyerLayout } from '@/components/buyer-layout';
import { Button } from '@/components/button';
import { Alert } from '@/components/alert';
import type { OrderHistoryResponse } from '@/types/order';

const formatPrice = (amount: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);

const formatDate = (dateString: string) =>
  new Date(dateString).toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

const canCancel = (eventDate: string) =>
  new Date(eventDate).getTime() - Date.now() > 24 * 60 * 60 * 1000;

export const MyBookingsView: React.FC = () => {
  const navigate = useNavigate();
  const { updateWalletBalance } = useAuth();
  const queryClient = useQueryClient();
  const [cancellingOrderId, setCancellingOrderId] = useState<string | null>(null);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const { data, isLoading, error, refetch } = useQuery<OrderHistoryResponse>({
    queryKey: ['myBookings'],
    queryFn: () => orderService.getMyBookings(),
  });

  const cancelMutation = useMutation({
    mutationFn: (orderId: string) => orderService.cancelOrder(orderId),
    onSuccess: (response) => {
      setCancellingOrderId(null);
      setCancelError(null);
      if (response.remainingBalance !== undefined) {
        updateWalletBalance(response.remainingBalance);
      }
      queryClient.invalidateQueries({ queryKey: ['myBookings'] });
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setCancellingOrderId(null);
      setCancelError(
        err.response?.data?.message ?? 'Failed to cancel order. Please try again.',
      );
    },
  });

  const handleCancel = (orderId: string) => {
    if (!window.confirm('Are you sure you want to cancel this order?')) return;
    setCancelError(null);
    setCancellingOrderId(orderId);
    cancelMutation.mutate(orderId);
  };

  return (
    <BuyerLayout>
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">My Bookings</h1>

        {/* Loading skeleton */}
        {isLoading && (
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="bg-white rounded-lg shadow p-6 animate-pulse space-y-3">
                <div className="h-5 bg-gray-300 rounded w-1/2"></div>
                <div className="h-4 bg-gray-300 rounded w-1/3"></div>
                <div className="h-4 bg-gray-300 rounded w-2/3"></div>
                <div className="h-4 bg-gray-300 rounded w-1/4"></div>
              </div>
            ))}
          </div>
        )}

        {/* Error state */}
        {error && (
          <div className="space-y-3">
            <Alert variant="error">Failed to load bookings. Please try again.</Alert>
            <Button onClick={() => refetch()}>Retry</Button>
          </div>
        )}

        {/* Cancel error */}
        {cancelError && (
          <Alert variant="error" className="mb-4">{cancelError}</Alert>
        )}

        {/* Empty state */}
        {!isLoading && !error && data && data.content.length === 0 && (
          <div className="text-center py-16">
            <p className="text-gray-500 text-lg mb-6">You haven't booked any tickets yet.</p>
            <Button onClick={() => navigate('/events')}>Browse Events</Button>
          </div>
        )}

        {/* Bookings list */}
        {!isLoading && !error && data && data.content.length > 0 && (
          <div className="space-y-4">
            {data.content.map((order) => {
              const firstItem = order.items[0];
              const totalTickets = order.items.reduce((sum, item) => sum + item.quantity, 0);
              const showCancel =
                order.status === 'CONFIRMED' &&
                firstItem?.eventDate != null &&
                canCancel(firstItem.eventDate);

              return (
                <div key={order.orderId} className="bg-white rounded-lg shadow p-6">
                  {/* Order header */}
                  <div className="flex items-start justify-between mb-4">
                    <div className="min-w-0 flex-1">
                      <h2 className="text-lg font-semibold text-gray-900 truncate">
                        {firstItem?.eventTitle ?? 'Order'}
                      </h2>
                      {firstItem?.eventDate && (
                        <p className="text-sm text-gray-500 mt-0.5">
                          {formatDate(firstItem.eventDate)}
                        </p>
                      )}
                      {firstItem?.venueName && (
                        <p className="text-sm text-gray-500">{firstItem.venueName}</p>
                      )}
                    </div>
                    <span className="ml-4 flex-shrink-0 px-3 py-1 bg-green-100 text-green-700 text-sm font-medium rounded-full">
                      {order.status}
                    </span>
                  </div>

                  {/* Ticket breakdown */}
                  <div className="border-t border-gray-100 pt-3 space-y-1.5">
                    {order.items.map((item) => (
                      <div key={item.orderItemId} className="flex justify-between text-sm text-gray-700">
                        <span>{item.tierName} &times; {item.quantity}</span>
                        <span>{formatPrice(item.unitPrice * item.quantity)}</span>
                      </div>
                    ))}
                  </div>

                  {/* Footer: total tickets + total amount + cancel */}
                  <div className="border-t border-gray-100 mt-3 pt-3 flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      {totalTickets} ticket{totalTickets !== 1 ? 's' : ''}
                    </span>
                    <div className="flex items-center gap-3">
                      <span className="font-semibold text-gray-900">{formatPrice(order.totalAmount)}</span>
                      {showCancel && (
                        <Button
                          variant="danger"
                          disabled={cancellingOrderId === order.orderId}
                          onClick={() => handleCancel(order.orderId)}
                        >
                          {cancellingOrderId === order.orderId ? 'Cancelling…' : 'Cancel Order'}
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </BuyerLayout>
  );
};
