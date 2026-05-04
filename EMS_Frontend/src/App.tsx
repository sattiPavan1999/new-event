import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { CartProvider } from './contexts/CartContext';
import LoginView from './views/login';
import BuyerRegistrationView from './views/buyer-registration';
import OrganiserRegistrationView from './views/organiser-registration';
import EventListingView from './views/event-listing';
import EventDetailView from './views/event-detail';
import { OrganiserDashboardView } from './views/organiser-dashboard';
import { OrganiserEventsView } from './views/organiser-events';
import { OrganiserEventDetailView } from './views/organiser-event-detail';
import { OrganiserEventCreateView } from './views/organiser-event-create';
import { OrderConfirmationView } from './views/order-confirmation';
import { MyBookingsView } from './views/my-bookings';
import { CartView } from './views/cart';
import './App.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const ProtectedRoute: React.FC<{ children: React.ReactNode; allowedRoles?: string[] }> = ({
  children,
  allowedRoles,
}) => {
  const { isAuthenticated, user, isLoading } = useAuth();

  if (isLoading) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

function AppRoutes() {
  const { isAuthenticated, user } = useAuth();

  const homeRedirect = isAuthenticated
    ? user?.role === 'ORGANISER'
      ? '/organiser/dashboard'
      : '/events'
    : '/login';

  return (
    <Routes>
      {/* Public routes */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to={homeRedirect} replace /> : <LoginView />}
      />
      <Route
        path="/register/buyer"
        element={isAuthenticated ? <Navigate to="/events" replace /> : <BuyerRegistrationView />}
      />
      <Route
        path="/register/organiser"
        element={isAuthenticated ? <Navigate to="/organiser/dashboard" replace /> : <OrganiserRegistrationView />}
      />

      {/* Public event routes */}
      <Route path="/events" element={<EventListingView />} />
      <Route path="/events/:eventId" element={<EventDetailView />} />

      {/* Cart — requires BUYER login */}
      <Route
        path="/cart"
        element={
          <ProtectedRoute allowedRoles={['BUYER']}>
            <CartView />
          </ProtectedRoute>
        }
      />

      {/* Order confirmation — requires BUYER login */}
      <Route
        path="/orders/confirmation"
        element={
          <ProtectedRoute allowedRoles={['BUYER']}>
            <OrderConfirmationView />
          </ProtectedRoute>
        }
      />

      {/* My Bookings — requires BUYER login */}
      <Route
        path="/my-bookings"
        element={
          <ProtectedRoute allowedRoles={['BUYER']}>
            <MyBookingsView />
          </ProtectedRoute>
        }
      />

      {/* Protected organiser routes */}
      <Route
        path="/organiser/dashboard"
        element={
          <ProtectedRoute allowedRoles={['ORGANISER']}>
            <OrganiserDashboardView />
          </ProtectedRoute>
        }
      />
      <Route
        path="/organiser/events"
        element={
          <ProtectedRoute allowedRoles={['ORGANISER']}>
            <OrganiserEventsView />
          </ProtectedRoute>
        }
      />
      <Route
        path="/organiser/events/create"
        element={
          <ProtectedRoute allowedRoles={['ORGANISER']}>
            <OrganiserEventCreateView />
          </ProtectedRoute>
        }
      />
      <Route
        path="/organiser/events/:eventId"
        element={
          <ProtectedRoute allowedRoles={['ORGANISER']}>
            <OrganiserEventDetailView />
          </ProtectedRoute>
        }
      />

      {/* Default route */}
      <Route path="/" element={<Navigate to={homeRedirect} replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <CartProvider>
            <AppRoutes />
          </CartProvider>
        </AuthProvider>
      </QueryClientProvider>
    </BrowserRouter>
  );
}

export default App;
