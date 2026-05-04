import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '@/contexts/CartContext';
import { useAuth } from '@/contexts/AuthContext';
import { orderService } from '@/services/order';
import { BuyerLayout } from '@/components/buyer-layout';
import { Button } from '@/components/button';
import { Alert } from '@/components/alert';
import type { ConfirmedOrder } from '@/types/order';

const formatPrice = (amount: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);

const padQty = (n: number) => String(n).padStart(2, '0');

export const CartView: React.FC = () => {
  const navigate = useNavigate();
  const { items, updateQuantity, removeEvent, clearCart, totalAmount } = useCart();
  const { user, updateWalletBalance } = useAuth();
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const [checkingOut, setCheckingOut] = useState(false);

  const eventGroups = items.reduce<Record<number, typeof items>>((acc, item) => {
    if (!acc[item.eventId]) acc[item.eventId] = [];
    acc[item.eventId].push(item);
    return acc;
  }, {});

  const handleCheckout = async () => {
    if (!user) { navigate('/login'); return; }
    setCheckoutError(null);
    setCheckingOut(true);

    const confirmedOrders: ConfirmedOrder[] = [];
    let grandTotal = 0;

    try {
      for (const [eventId, eventItems] of Object.entries(eventGroups)) {
        const response = await orderService.createOrder({
          eventId: Number(eventId),
          items: eventItems.map(i => ({ tierId: i.tierId, quantity: i.quantity })),
        });

        confirmedOrders.push({
          orderId: response.orderId,
          status: response.status,
          eventTitle: eventItems[0].eventTitle,
          totalAmount: response.totalAmount,
          items: eventItems.map(i => ({
            tierName: i.tierName,
            quantity: i.quantity,
            unitPrice: i.unitPrice,
          })),
        });
        grandTotal += response.totalAmount;

        if (response.remainingBalance !== undefined) {
          updateWalletBalance(response.remainingBalance);
        }
      }

      clearCart();
      navigate('/orders/confirmation', { state: { orders: confirmedOrders, grandTotal } });
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to place order. Please try again.';
      setCheckoutError(msg);
    } finally {
      setCheckingOut(false);
    }
  };

  if (items.length === 0) {
    return (
      <BuyerLayout>
        <div className="flex items-center justify-center py-24">
          <div className="text-center">
            <p className="text-gray-600 mb-4">Your cart is empty.</p>
            <Button onClick={() => navigate('/events')}>Browse Events</Button>
          </div>
        </div>
      </BuyerLayout>
    );
  }

  return (
    <BuyerLayout>
      <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Your Cart</h1>

        <div className="space-y-4 mb-6">
          {Object.entries(eventGroups).map(([eventId, eventItems]) => (
            <div key={eventId} className="bg-white rounded-xl shadow p-5">
              <div className="flex items-start justify-between mb-4">
                <p className="font-semibold text-gray-900">{eventItems[0].eventTitle}</p>
                <button
                  onClick={() => removeEvent(Number(eventId))}
                  className="text-xs text-red-500 hover:text-red-700 font-medium ml-4 flex-shrink-0"
                >
                  Remove all
                </button>
              </div>

              <div className="space-y-3">
                {eventItems.map(item => (
                  <div key={item.tierId} className="flex items-center gap-3">
                    <span className="text-sm text-gray-700 flex-1">{item.tierName}</span>

                    {/* Stepper */}
                    <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden">
                      <button
                        onClick={() => updateQuantity(item.tierId, item.quantity - 1)}
                        disabled={item.quantity <= 0}
                        aria-label={`Decrease quantity for ${item.tierName}`}
                        className="w-9 h-9 flex items-center justify-center text-gray-600 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed border-r border-gray-200 text-lg font-medium"
                      >
                        −
                      </button>
                      <span className="w-10 text-center font-bold text-gray-900 text-sm select-none">
                        {padQty(item.quantity)}
                      </span>
                      <button
                        onClick={() => updateQuantity(item.tierId, item.quantity + 1)}
                        disabled={item.quantity >= item.maxQty}
                        aria-label={`Increase quantity for ${item.tierName}`}
                        className="w-9 h-9 flex items-center justify-center text-gray-600 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed border-l border-gray-200 text-lg font-medium"
                      >
                        +
                      </button>
                    </div>

                    <span className="text-sm font-medium text-gray-900 w-24 text-right tabular-nums">
                      {formatPrice(item.unitPrice * item.quantity)}
                    </span>
                  </div>
                ))}
              </div>

              <div className="border-t border-gray-100 mt-4 pt-3 flex justify-between text-sm font-semibold text-gray-900">
                <span>Subtotal</span>
                <span>{formatPrice(eventItems.reduce((s, i) => s + i.quantity * i.unitPrice, 0))}</span>
              </div>
            </div>
          ))}
        </div>

        <div className="bg-white rounded-xl shadow p-5 mb-6">
          <div className="flex justify-between font-bold text-gray-900 text-lg">
            <span>Total</span>
            <span>{formatPrice(totalAmount)}</span>
          </div>
        </div>

        {checkoutError && (
          <Alert variant="error" className="mb-4">{checkoutError}</Alert>
        )}

        <Button fullWidth disabled={checkingOut} onClick={handleCheckout}>
          {checkingOut ? 'Processing…' : 'Buy Now'}
        </Button>
      </div>
    </BuyerLayout>
  );
};
