import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useCart } from '@/contexts/CartContext';
import { LogoutDialog } from '@/components/logout-dialog/logout-dialog';

interface BuyerLayoutProps {
  children: React.ReactNode;
}

export const BuyerLayout: React.FC<BuyerLayoutProps> = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
  const { totalCount } = useCart();
  const [isLogoutOpen, setIsLogoutOpen] = useState(false);

  const isBuyer = isAuthenticated && user?.role === 'BUYER';

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-medium pb-0.5 border-b-2 transition-colors ${
      isActive
        ? 'border-blue-600 text-blue-600'
        : 'border-transparent text-gray-600 hover:text-gray-900'
    }`;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <div className="flex items-center gap-6">
            <span className="text-lg font-bold text-gray-900">EMS</span>
            <NavLink to="/events" end className={navLinkClass}>
              Browse Events
            </NavLink>
            {isBuyer && (
              <NavLink to="/my-bookings" className={navLinkClass}>
                My Bookings
              </NavLink>
            )}
          </div>

          {isAuthenticated && user ? (
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600">Hi, {user.fullName}</span>
              {user.role === 'BUYER' && user.walletBalance !== undefined && (
                <span className="px-2 py-0.5 bg-green-100 text-green-800 text-xs font-semibold rounded-full">
                  ₹{user.walletBalance.toLocaleString('en-IN')}
                </span>
              )}
              {isBuyer && (
                <NavLink to="/cart" className="relative text-gray-600 hover:text-gray-900 transition-colors">
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                  {totalCount > 0 && (
                    <span className="absolute -top-1 -right-1 bg-blue-600 text-white text-xs font-bold rounded-full w-4 h-4 flex items-center justify-center leading-none">
                      {totalCount > 9 ? '9+' : totalCount}
                    </span>
                  )}
                </NavLink>
              )}
              <button
                onClick={() => setIsLogoutOpen(true)}
                className="text-sm font-medium text-red-600 hover:text-red-800 transition-colors"
              >
                Logout
              </button>
            </div>
          ) : null}
        </div>
      </header>

      <main>{children}</main>

      <LogoutDialog isOpen={isLogoutOpen} onClose={() => setIsLogoutOpen(false)} />
    </div>
  );
};
