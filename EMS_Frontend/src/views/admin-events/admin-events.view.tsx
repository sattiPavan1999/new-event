import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { eventService } from '@/services/event';
import { AdminLayout } from '@/components/admin-layout';
import { Button } from '@/components/button';
import { Alert } from '@/components/alert';
import { EventStatus } from '@/types/event';
import type { AdminEvent } from '@/types/event';

const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const colors: Record<string, string> = {
    DRAFT: 'bg-yellow-100 text-yellow-800',
    PUBLISHED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
  };
  return (
    <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${colors[status] ?? 'bg-gray-100 text-gray-800'}`}>
      {status}
    </span>
  );
};

type FilterStatus = 'ALL' | 'DRAFT' | 'PUBLISHED' | 'CANCELLED';

export const AdminEventsView: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<FilterStatus>('ALL');
  const [actionError, setActionError] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['adminEvents', { page }],
    queryFn: () => eventService.getAdminEvents(page, 10),
  });

  const publishMutation = useMutation({
    mutationFn: (id: string) => eventService.publishEvent(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminEvents'] });
      setActionError(null);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(msg ?? 'Failed to publish event');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => eventService.cancelEvent(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminEvents'] });
      setActionError(null);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(msg ?? 'Failed to cancel event');
    },
  });

  const allEvents: AdminEvent[] = data?.content ?? [];
  const events = filter === 'ALL' ? allEvents : allEvents.filter(e => e.status === filter);

  const formatDate = (dateString: string) =>
    new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });

  return (
    <AdminLayout>
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Events</h1>
          <Button onClick={() => navigate('/admin/events/create')}>+ Create Event</Button>
        </div>

        {actionError && (
          <Alert variant="error" className="mb-4">{actionError}</Alert>
        )}

        {/* Status filter tabs */}
        <div className="flex gap-2 mb-4">
          {(['ALL', 'DRAFT', 'PUBLISHED', 'CANCELLED'] as FilterStatus[]).map(s => (
            <button
              key={s}
              onClick={() => setFilter(s)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
                filter === s
                  ? 'bg-blue-600 text-white'
                  : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
              }`}
            >
              {s}
            </button>
          ))}
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          {isLoading ? (
            <div className="p-6 space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-14 bg-gray-100 rounded animate-pulse" />
              ))}
            </div>
          ) : events.length === 0 ? (
            <div className="p-12 text-center">
              <p className="text-gray-500 mb-4">
                {filter === 'ALL' ? 'No events yet.' : `No ${filter.toLowerCase()} events.`}
              </p>
              {filter === 'ALL' && (
                <Button onClick={() => navigate('/admin/events/create')}>Create your first event</Button>
              )}
            </div>
          ) : (
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-100">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Event</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Date</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Tiers</th>
                  <th className="px-6 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {events.map(event => (
                  <tr key={event.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div>
                        <p className="text-sm font-medium text-gray-900">{event.title}</p>
                        <p className="text-xs text-gray-500">{event.venue?.city} · {event.category}</p>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">{formatDate(event.eventDate)}</td>
                    <td className="px-6 py-4"><StatusBadge status={event.status} /></td>
                    <td className="px-6 py-4 text-sm text-gray-600">{event.tiers?.length ?? 0}</td>
                    <td className="px-6 py-4">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => navigate(`/admin/events/${event.id}`)}
                          className="text-sm text-blue-600 hover:text-blue-800 font-medium"
                        >
                          View
                        </button>
                        {event.status === EventStatus.DRAFT && (
                          <button
                            onClick={() => publishMutation.mutate(event.id)}
                            disabled={publishMutation.isPending}
                            className="text-sm text-green-600 hover:text-green-800 font-medium disabled:opacity-50"
                          >
                            Publish
                          </button>
                        )}
                        {(event.status === EventStatus.DRAFT || event.status === EventStatus.PUBLISHED) && (
                          <button
                            onClick={() => cancelMutation.mutate(event.id)}
                            disabled={cancelMutation.isPending}
                            className="text-sm text-red-600 hover:text-red-800 font-medium disabled:opacity-50"
                          >
                            Cancel
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex justify-center gap-2 mt-4">
            <Button variant="secondary" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>
              Previous
            </Button>
            <span className="px-4 py-2 text-sm text-gray-600">Page {page + 1} of {data.totalPages}</span>
            <Button variant="secondary" onClick={() => setPage(p => p + 1)} disabled={page >= data.totalPages - 1}>
              Next
            </Button>
          </div>
        )}
      </div>
    </AdminLayout>
  );
};
