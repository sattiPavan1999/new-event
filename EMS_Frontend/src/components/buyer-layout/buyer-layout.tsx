import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { LogoutDialog } from '@/components/logout-dialog/logout-dialog';

interface BuyerLayoutProps {
  children: React.ReactNode;
}

export const BuyerLayout: React.FC<BuyerLayoutProps> = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
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
