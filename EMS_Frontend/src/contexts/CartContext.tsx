import React, { createContext, useContext, useState } from 'react';

export interface CartItem {
  eventId: number;
  eventTitle: string;
  tierId: number;
  tierName: string;
  quantity: number;
  unitPrice: number;
  maxQty: number;
}

interface CartContextValue {
  items: CartItem[];
  addItems: (
    eventId: number,
    eventTitle: string,
    selections: Array<{ tierId: number; tierName: string; quantity: number; unitPrice: number; maxQty: number }>
  ) => void;
  updateQuantity: (tierId: number, quantity: number) => void;
  removeEvent: (eventId: number) => void;
  clearCart: () => void;
  totalCount: number;
  totalAmount: number;
}

const CartContext = createContext<CartContextValue | null>(null);

export const CartProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [items, setItems] = useState<CartItem[]>([]);

  const addItems = (
    eventId: number,
    eventTitle: string,
    selections: Array<{ tierId: number; tierName: string; quantity: number; unitPrice: number; maxQty: number }>
  ) => {
    setItems(prev => [
      ...prev.filter(i => i.eventId !== eventId),
      ...selections.map(s => ({ ...s, eventId, eventTitle })),
    ]);
  };

  const updateQuantity = (tierId: number, quantity: number) =>
    setItems(prev =>
      quantity <= 0
        ? prev.filter(i => i.tierId !== tierId)
        : prev.map(i => i.tierId === tierId ? { ...i, quantity } : i)
    );

  const removeEvent = (eventId: number) =>
    setItems(prev => prev.filter(i => i.eventId !== eventId));

  const clearCart = () => setItems([]);

  const totalCount = items.reduce((sum, i) => sum + i.quantity, 0);
  const totalAmount = items.reduce((sum, i) => sum + i.quantity * i.unitPrice, 0);

  return (
    <CartContext.Provider value={{ items, addItems, updateQuantity, removeEvent, clearCart, totalCount, totalAmount }}>
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
};
