import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { eventService } from '@/services/event';
import { useAuth } from '@/contexts/AuthContext';
import { AdminLayout } from '@/components/admin-layout';
import { Button } from '@/components/button';
import { EventStatus } from '@/types/event';
import type { AdminEvent } from '@/types/event';

const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const colors: Record<string, string> = {
    DRAFT: 'bg-yellow-100 text-yellow-800',
    PUBLISHED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
  };
  return (
    <span className={`px-2 py-0.5 rounded text-xs font-medium ${colors[status] ?? 'bg-gray-100 text-gray-800'}`}>
      {status}
    </span>
  );
};

export const AdminDashboardView: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data, isLoading } = useQuery({
    queryKey: ['adminEvents', { page: 0, size: 100 }],
    queryFn: () => eventService.getAdminEvents(0, 100),
  });

  const events: AdminEvent[] = data?.content ?? [];

  const stats = {
    total: events.length,
    published: events.filter(e => e.status === EventStatus.PUBLISHED).length,
    draft: events.filter(e => e.status === EventStatus.DRAFT).length,
    cancelled: events.filter(e => e.status === EventStatus.CANCELLED).length,
  };

  const recentEvents = [...events]
    .sort((a, b) => new Date(b.eventDate).getTime() - new Date(a.eventDate).getTime())
    .slice(0, 5);

  const formatDate = (dateString: string) =>
    new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });

  return (
    <AdminLayout>
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
            <p className="text-gray-500 mt-1">Welcome back, {user?.fullName}</p>
          </div>
          <Button onClick={() => navigate('/admin/events/create')}>
            + Create Event
          </Button>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          {[
            { label: 'Total Events', value: stats.total, color: 'text-gray-900' },
            { label: 'Published', value: stats.published, color: 'text-green-600' },
            { label: 'Draft', value: stats.draft, color: 'text-yellow-600' },
            { label: 'Cancelled', value: stats.cancelled, color: 'text-red-600' },
          ].map(stat => (
            <div key={stat.label} className="bg-white rounded-xl shadow-sm p-5 border border-gray-100">
              <p className="text-sm text-gray-500">{stat.label}</p>
              {isLoading ? (
                <div className="h-8 w-12 bg-gray-200 rounded animate-pulse mt-1" />
              ) : (
                <p className={`text-3xl font-bold mt-1 ${stat.color}`}>{stat.value}</p>
              )}
            </div>
          ))}
        </div>

        {/* Recent Events */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
            <h2 className="text-base font-semibold text-gray-900">Recent Events</h2>
            <button
              onClick={() => navigate('/admin/events')}
              className="text-sm text-blue-600 hover:text-blue-800"
            >
              View all
            </button>
          </div>

          {isLoading ? (
            <div className="p-6 space-y-3">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="h-10 bg-gray-100 rounded animate-pulse" />
              ))}
            </div>
          ) : recentEvents.length === 0 ? (
            <div className="p-12 text-center">
              <p className="text-gray-500 mb-4">No events yet.</p>
              <Button onClick={() => navigate('/admin/events/create')}>Create your first event</Button>
            </div>
          ) : (
            <ul className="divide-y divide-gray-50">
              {recentEvents.map(event => (
                <li
                  key={event.id}
                  className="flex items-center justify-between px-6 py-4 hover:bg-gray-50 cursor-pointer"
                  onClick={() => navigate(`/admin/events/${event.id}`)}
                >
                  <div>
                    <p className="text-sm font-medium text-gray-900">{event.title}</p>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {event.venue?.city} · {formatDate(event.eventDate)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <StatusBadge status={event.status} />
                    <span className="text-xs text-gray-400">{event.tiers?.length ?? 0} tiers</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </AdminLayout>
  );
};
