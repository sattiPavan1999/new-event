import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import LoginView from './views/login';
import BuyerRegistrationView from './views/buyer-registration';
import OrganiserRegistrationView from './views/organiser-registration';
import EventListingView from './views/event-listing';
import EventDetailView from './views/event-detail';
import { AdminDashboardView } from './views/admin-dashboard';
import { AdminEventsView } from './views/admin-events';
import { AdminEventDetailView } from './views/admin-event-detail';
import { AdminEventCreateView } from './views/admin-event-create';
import { OrderConfirmationView } from './views/order-confirmation';
import { MyBookingsView } from './views/my-bookings';
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
    ? user?.role === 'ORGANISER' || user?.role === 'ADMIN'
      ? '/admin/dashboard'
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
        element={isAuthenticated ? <Navigate to="/admin/dashboard" replace /> : <OrganiserRegistrationView />}
      />

      {/* Public event routes */}
      <Route path="/events" element={<EventListingView />} />
      <Route path="/events/:eventId" element={<EventDetailView />} />

      {/* Order confirmation — requires BUYER login */}
      <Route
        path="/orders/:orderId"
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

      {/* Protected admin/organiser routes */}
      <Route
        path="/admin/dashboard"
        element={
          <ProtectedRoute allowedRoles={['ORGANISER', 'ADMIN']}>
            <AdminDashboardView />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/events"
        element={
          <ProtectedRoute allowedRoles={['ORGANISER', 'ADMIN']}>
            <AdminEventsView />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/events/create"
        element={
          <ProtectedRoute allowedRoles={['ORGANISER', 'ADMIN']}>
            <AdminEventCreateView />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/events/:eventId"
        element={
          <ProtectedRoute allowedRoles={['ORGANISER', 'ADMIN']}>
            <AdminEventDetailView />
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
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
