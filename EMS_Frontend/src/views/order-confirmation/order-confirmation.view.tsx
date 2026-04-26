import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import type { OrderConfirmationState } from '@/types/order';
import { BuyerLayout } from '@/components/buyer-layout';
import { Button } from '@/components/button';

const formatPrice = (amount: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);

export const OrderConfirmationView: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as OrderConfirmationState | null;

  if (!state) {
    return (
      <BuyerLayout>
        <div className="flex items-center justify-center py-24">
          <div className="text-center">
            <p className="text-gray-600 mb-4">Order details not found.</p>
            <Button onClick={() => navigate('/events')}>Back to Events</Button>
          </div>
        </div>
      </BuyerLayout>
    );
  }

  return (
    <BuyerLayout>
    <div className="flex items-center justify-center px-4 py-12">
      <div className="bg-white rounded-2xl shadow-lg max-w-md w-full p-8">
        {/* Success banner */}
        <div className="flex flex-col items-center mb-8">
          <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
            <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Payment Successful</h1>
          <p className="text-gray-500 text-sm mt-1">Your tickets have been confirmed</p>
        </div>

        {/* Order details */}
        <div className="border border-gray-100 rounded-xl p-4 mb-6 space-y-3">
          <div>
            <p className="text-xs font-medium text-gray-400 uppercase tracking-wide">Event</p>
            <p className="text-gray-900 font-semibold">{state.eventTitle}</p>
          </div>

          <div>
            <p className="text-xs font-medium text-gray-400 uppercase tracking-wide mb-2">Tickets</p>
            {state.items.map((item, i) => (
              <div key={i} className="flex justify-between text-sm text-gray-700 py-0.5">
                <span>{item.tierName} × {item.quantity}</span>
                <span>{formatPrice(item.unitPrice * item.quantity)}</span>
              </div>
            ))}
          </div>

          <div className="border-t border-gray-100 pt-3 flex justify-between font-semibold text-gray-900">
            <span>Total</span>
            <span>{formatPrice(state.totalAmount)}</span>
          </div>
        </div>

        {/* Status + Order ID */}
        <div className="flex items-center justify-between mb-8 text-sm">
          <span className="px-3 py-1 bg-green-100 text-green-700 rounded-full font-medium">
            {state.status}
          </span>
          <span className="text-gray-400 font-mono text-xs truncate ml-4">{state.orderId}</span>
        </div>

        <Button fullWidth onClick={() => navigate('/events')}>
          Back to Events
        </Button>
      </div>
    </div>
    </BuyerLayout>
  );
};
