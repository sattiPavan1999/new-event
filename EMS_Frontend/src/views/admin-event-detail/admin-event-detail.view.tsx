import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { eventService } from '@/services/event';
import { AdminLayout } from '@/components/admin-layout';
import { Button } from '@/components/button';
import { Input } from '@/components/input';
import { Alert } from '@/components/alert';
import { EventStatus, EventCategory } from '@/types/event';
import type { TicketTier, CreateTierRequest } from '@/types/event';

// ─── Helpers ────────────────────────────────────────────────────────────────

const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const colors: Record<string, string> = {
    DRAFT: 'bg-yellow-100 text-yellow-800',
    PUBLISHED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
  };
  return (
    <span className={`px-3 py-1 rounded-full text-sm font-medium ${colors[status] ?? 'bg-gray-100 text-gray-800'}`}>
      {status}
    </span>
  );
};

const formatCurrency = (n: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(n);

const toDatetimeLocal = (iso: string) => {
  if (!iso) return '';
  return iso.slice(0, 16);
};

// ─── Tier Form ───────────────────────────────────────────────────────────────

interface TierFormData {
  name: string;
  description: string;
  price: string;
  totalQty: string;
  maxPerOrder: string;
  saleStartsAt: string;
  saleEndsAt: string;
}

const emptyTierForm = (): TierFormData => ({
  name: '',
  description: '',
  price: '',
  totalQty: '',
  maxPerOrder: '10',
  saleStartsAt: '',
  saleEndsAt: '',
});

const tierFromExisting = (t: TicketTier): TierFormData => ({
  name: t.name,
  description: t.description ?? '',
  price: String(t.price),
  totalQty: String(t.totalQty),
  maxPerOrder: String(t.maxPerOrder),
  saleStartsAt: t.saleStartsAt ? toDatetimeLocal(t.saleStartsAt) : '',
  saleEndsAt: t.saleEndsAt ? toDatetimeLocal(t.saleEndsAt) : '',
});

interface TierFormPanelProps {
  tier?: TicketTier;
  eventId: string;
  onDone: () => void;
}

const TierFormPanel: React.FC<TierFormPanelProps> = ({ tier, eventId, onDone }) => {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<TierFormData>(tier ? tierFromExisting(tier) : emptyTierForm());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);

  const hasOrders = tier ? tier.remainingQty < tier.totalQty : false;

  const buildPayload = (): CreateTierRequest => ({
    name: form.name,
    description: form.description || undefined,
    price: parseFloat(form.price),
    totalQty: parseInt(form.totalQty, 10),
    maxPerOrder: parseInt(form.maxPerOrder, 10),
    saleStartsAt: form.saleStartsAt || undefined,
    saleEndsAt: form.saleEndsAt || undefined,
  });

  const saveMutation = useMutation({
    mutationFn: () =>
      tier
        ? eventService.updateTier(eventId, tier.id, buildPayload())
        : eventService.createTier(eventId, buildPayload()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminEvent', eventId] });
      onDone();
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setServerError(msg ?? 'Failed to save tier');
    },
  });

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.name.trim()) e.name = 'Name is required';
    if (!form.price || isNaN(parseFloat(form.price)) || parseFloat(form.price) < 0) e.price = 'Valid price required';
    if (!form.totalQty || isNaN(parseInt(form.totalQty)) || parseInt(form.totalQty) < 1) e.totalQty = 'Valid quantity required';
    if (!form.maxPerOrder || isNaN(parseInt(form.maxPerOrder)) || parseInt(form.maxPerOrder) < 1) e.maxPerOrder = 'Valid max per order required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);
    if (validate()) saveMutation.mutate();
  };

  const set = (field: keyof TierFormData, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: '' }));
  };

  return (
    <form onSubmit={handleSubmit} className="border border-gray-200 rounded-lg p-5 bg-gray-50 space-y-4">
      <h4 className="font-medium text-gray-900">{tier ? 'Edit Tier' : 'New Tier'}</h4>

      {serverError && <Alert variant="error">{serverError}</Alert>}
      {hasOrders && (
        <Alert variant="warning">This tier has confirmed orders — price and quantity cannot be changed.</Alert>
      )}

      <div className="grid grid-cols-2 gap-4">
        <div className="col-span-2">
          <Input label="Tier Name" value={form.name} onChange={e => set('name', e.target.value)} error={errors.name} fullWidth />
        </div>
        <div className="col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
          <textarea
            value={form.description}
            onChange={e => set('description', e.target.value)}
            rows={2}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            placeholder="Optional description"
          />
        </div>
        <Input
          label="Price (INR)"
          type="number"
          min="0"
          step="0.01"
          value={form.price}
          onChange={e => set('price', e.target.value)}
          error={errors.price}
          disabled={hasOrders}
          fullWidth
        />
        <Input
          label="Total Quantity"
          type="number"
          min="1"
          value={form.totalQty}
          onChange={e => set('totalQty', e.target.value)}
          error={errors.totalQty}
          disabled={hasOrders}
          fullWidth
        />
        <Input
          label="Max Per Order"
          type="number"
          min="1"
          value={form.maxPerOrder}
          onChange={e => set('maxPerOrder', e.target.value)}
          error={errors.maxPerOrder}
          fullWidth
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Sale Starts At</label>
          <input
            type="datetime-local"
            value={form.saleStartsAt}
            onChange={e => set('saleStartsAt', e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Sale Ends At</label>
          <input
            type="datetime-local"
            value={form.saleEndsAt}
            onChange={e => set('saleEndsAt', e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      <div className="flex gap-2">
        <Button type="submit" isLoading={saveMutation.isPending}>
          {tier ? 'Save Changes' : 'Add Tier'}
        </Button>
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </form>
  );
};

// ─── Main View ───────────────────────────────────────────────────────────────

export const AdminEventDetailView: React.FC = () => {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [editingEvent, setEditingEvent] = useState(false);
  const [addingTier, setAddingTier] = useState(false);
  const [editingTierId, setEditingTierId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Event form state
  const [eventForm, setEventForm] = useState({
    title: '',
    description: '',
    category: '',
    eventDate: '',
    venueId: '',
    bannerImageUrl: '',
  });
  const [eventFormErrors, setEventFormErrors] = useState<Record<string, string>>({});

  const { data: event, isLoading, error } = useQuery({
    queryKey: ['adminEvent', eventId],
    queryFn: () => eventService.getAdminEvent(eventId!),
    enabled: !!eventId,
  });

  const { data: summary } = useQuery({
    queryKey: ['salesSummary', eventId],
    queryFn: () => eventService.getSalesSummary(eventId!),
    enabled: !!eventId,
  });

  const { data: venues = [] } = useQuery({
    queryKey: ['venues'],
    queryFn: () => eventService.getVenues(),
  });

  const updateMutation = useMutation({
    mutationFn: () =>
      eventService.updateEvent(eventId!, {
        title: eventForm.title,
        description: eventForm.description || undefined,
        category: eventForm.category as typeof EventCategory[keyof typeof EventCategory],
        eventDate: eventForm.eventDate,
        venueId: eventForm.venueId,
        bannerImageUrl: eventForm.bannerImageUrl || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminEvent', eventId] });
      queryClient.invalidateQueries({ queryKey: ['adminEvents'] });
      setEditingEvent(false);
      setActionSuccess('Event updated successfully');
      setActionError(null);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(msg ?? 'Failed to update event');
    },
  });

  const publishMutation = useMutation({
    mutationFn: () => eventService.publishEvent(eventId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminEvent', eventId] });
      queryClient.invalidateQueries({ queryKey: ['adminEvents'] });
      setActionSuccess('Event published successfully');
      setActionError(null);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(msg ?? 'Failed to publish event. Make sure you have at least one active ticket tier.');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: () => eventService.cancelEvent(eventId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminEvent', eventId] });
      queryClient.invalidateQueries({ queryKey: ['adminEvents'] });
      setActionSuccess('Event cancelled');
      setActionError(null);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(msg ?? 'Failed to cancel event');
    },
  });

  const deleteTierMutation = useMutation({
    mutationFn: (tierId: string) => eventService.deleteTier(eventId!, tierId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminEvent', eventId] });
      queryClient.invalidateQueries({ queryKey: ['salesSummary', eventId] });
      setActionSuccess('Tier deleted');
      setActionError(null);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(msg ?? 'Cannot delete tier — orders may exist for it.');
    },
  });

  const startEditEvent = () => {
    if (!event) return;
    setEventForm({
      title: event.title,
      description: event.description ?? '',
      category: event.category,
      eventDate: toDatetimeLocal(event.eventDate),
      venueId: event.venue?.id ?? '',
      bannerImageUrl: event.bannerImageUrl ?? '',
    });
    setEditingEvent(true);
  };

  const validateEventForm = () => {
    const e: Record<string, string> = {};
    if (!eventForm.title.trim()) e.title = 'Title is required';
    if (!eventForm.category) e.category = 'Category is required';
    if (!eventForm.eventDate) e.eventDate = 'Event date is required';
    if (!eventForm.venueId) e.venueId = 'Venue is required';
    setEventFormErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleUpdateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setActionError(null);
    if (validateEventForm()) updateMutation.mutate();
  };

  const isPublished = event?.status === EventStatus.PUBLISHED;
  const isCancelled = event?.status === EventStatus.CANCELLED;
  const isDraft = event?.status === EventStatus.DRAFT;

  if (isLoading) {
    return (
      <AdminLayout>
        <div className="max-w-3xl mx-auto space-y-4 animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/2" />
          <div className="h-48 bg-gray-200 rounded-xl" />
          <div className="h-32 bg-gray-200 rounded-xl" />
        </div>
      </AdminLayout>
    );
  }

  if (error || !event) {
    return (
      <AdminLayout>
        <div className="max-w-3xl mx-auto">
          <Alert variant="error">Event not found or you don't have permission to view it.</Alert>
          <Button className="mt-4" onClick={() => navigate('/admin/events')}>Back to Events</Button>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div className="max-w-3xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <button onClick={() => navigate('/admin/events')} className="text-gray-400 hover:text-gray-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{event.title}</h1>
              <p className="text-sm text-gray-500 mt-0.5">{event.venue?.name} · {event.venue?.city}</p>
            </div>
          </div>
          <StatusBadge status={event.status} />
        </div>

        {/* Alerts */}
        {actionError && <Alert variant="error">{actionError}</Alert>}
        {actionSuccess && <Alert variant="success">{actionSuccess}</Alert>}

        {/* Actions */}
        {!isCancelled && (
          <div className="flex gap-2">
            {isDraft && (
              <Button
                onClick={() => publishMutation.mutate()}
                isLoading={publishMutation.isPending}
              >
                Publish Event
              </Button>
            )}
            {!isCancelled && (
              <Button
                variant="danger"
                onClick={() => cancelMutation.mutate()}
                isLoading={cancelMutation.isPending}
              >
                Cancel Event
              </Button>
            )}
            {!isCancelled && (
              <Button variant="secondary" onClick={startEditEvent}>
                Edit Details
              </Button>
            )}
          </div>
        )}

        {isDraft && (event.tiers?.length ?? 0) === 0 && (
          <Alert variant="warning">Add at least one ticket tier before you can publish this event.</Alert>
        )}

        {/* Event Details / Edit Form */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-4">Event Details</h2>

          {editingEvent ? (
            <form onSubmit={handleUpdateSubmit} className="space-y-4">
              <Input
                label="Title"
                value={eventForm.title}
                onChange={e => setEventForm(p => ({ ...p, title: e.target.value }))}
                error={eventFormErrors.title}
                fullWidth
              />
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  value={eventForm.description}
                  onChange={e => setEventForm(p => ({ ...p, description: e.target.value }))}
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                  <select
                    value={eventForm.category}
                    onChange={e => setEventForm(p => ({ ...p, category: e.target.value }))}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    {Object.values(EventCategory).map(cat => (
                      <option key={cat} value={cat}>{cat}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Event Date
                    {isPublished && <span className="text-xs text-orange-500 ml-1">(locked after publish)</span>}
                  </label>
                  <input
                    type="datetime-local"
                    value={eventForm.eventDate}
                    onChange={e => setEventForm(p => ({ ...p, eventDate: e.target.value }))}
                    disabled={isPublished}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
                  />
                  {eventFormErrors.eventDate && <p className="text-red-600 text-xs mt-1">{eventFormErrors.eventDate}</p>}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Venue
                  {isPublished && <span className="text-xs text-orange-500 ml-1">(locked after publish)</span>}
                </label>
                <select
                  value={eventForm.venueId}
                  onChange={e => setEventForm(p => ({ ...p, venueId: e.target.value }))}
                  disabled={isPublished}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
                >
                  {venues.map(v => (
                    <option key={v.id} value={v.id}>{v.name} — {v.city}</option>
                  ))}
                </select>
              </div>
              <Input
                label="Banner Image URL"
                value={eventForm.bannerImageUrl}
                onChange={e => setEventForm(p => ({ ...p, bannerImageUrl: e.target.value }))}
                placeholder="https://..."
                fullWidth
              />
              <div className="flex gap-2 pt-1">
                <Button type="submit" isLoading={updateMutation.isPending}>Save</Button>
                <Button type="button" variant="secondary" onClick={() => setEditingEvent(false)}>Cancel</Button>
              </div>
            </form>
          ) : (
            <dl className="grid grid-cols-2 gap-x-6 gap-y-4 text-sm">
              <div>
                <dt className="text-gray-500">Category</dt>
                <dd className="font-medium text-gray-900 mt-0.5">{event.category}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Date</dt>
                <dd className="font-medium text-gray-900 mt-0.5">
                  {new Date(event.eventDate).toLocaleDateString('en-US', { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500">Venue</dt>
                <dd className="font-medium text-gray-900 mt-0.5">{event.venue?.name}</dd>
              </div>
              <div>
                <dt className="text-gray-500">City</dt>
                <dd className="font-medium text-gray-900 mt-0.5">{event.venue?.city}</dd>
              </div>
              {event.description && (
                <div className="col-span-2">
                  <dt className="text-gray-500">Description</dt>
                  <dd className="font-medium text-gray-900 mt-0.5">{event.description}</dd>
                </div>
              )}
            </dl>
          )}
        </div>

        {/* Ticket Tiers */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold text-gray-900">Ticket Tiers ({event.tiers?.length ?? 0}/10)</h2>
            {!isCancelled && !addingTier && !editingTierId && (event.tiers?.length ?? 0) < 10 && (
              <Button variant="secondary" onClick={() => setAddingTier(true)}>+ Add Tier</Button>
            )}
          </div>

          {addingTier && (
            <div className="mb-4">
              <TierFormPanel eventId={eventId!} onDone={() => setAddingTier(false)} />
            </div>
          )}

          {(event.tiers?.length ?? 0) === 0 && !addingTier ? (
            <p className="text-sm text-gray-500">No ticket tiers yet. Add at least one to publish the event.</p>
          ) : (
            <div className="space-y-3">
              {event.tiers?.map(tier => (
                <div key={tier.id}>
                  {editingTierId === tier.id ? (
                    <TierFormPanel
                      tier={tier}
                      eventId={eventId!}
                      onDone={() => setEditingTierId(null)}
                    />
                  ) : (
                    <div className="border border-gray-200 rounded-lg p-4 flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <p className="text-sm font-semibold text-gray-900">{tier.name}</p>
                          <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                            tier.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                            tier.status === 'SOLD_OUT' ? 'bg-red-100 text-red-700' :
                            'bg-gray-100 text-gray-600'
                          }`}>{tier.status}</span>
                        </div>
                        {tier.description && <p className="text-xs text-gray-500 mb-2">{tier.description}</p>}
                        <div className="flex flex-wrap gap-4 text-xs text-gray-600">
                          <span><strong>{formatCurrency(tier.price)}</strong> / ticket</span>
                          <span>{tier.remainingQty} / {tier.totalQty} remaining</span>
                          <span>Max {tier.maxPerOrder} per order</span>
                        </div>
                      </div>
                      {!isCancelled && (
                        <div className="flex gap-2 ml-4 shrink-0">
                          <button
                            onClick={() => setEditingTierId(tier.id)}
                            className="text-xs text-blue-600 hover:text-blue-800 font-medium"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => {
                              if (tier.remainingQty < tier.totalQty) {
                                setActionError('Cannot delete tier — confirmed orders exist for it.');
                                return;
                              }
                              deleteTierMutation.mutate(tier.id);
                            }}
                            className="text-xs text-red-600 hover:text-red-800 font-medium"
                          >
                            Delete
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Sales Summary */}
        {summary && (
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h2 className="text-base font-semibold text-gray-900 mb-4">Sales Summary</h2>
            <div className="grid grid-cols-2 gap-4 mb-6">
              <div className="bg-blue-50 rounded-lg p-4">
                <p className="text-xs text-blue-600 font-medium uppercase tracking-wide">Total Orders</p>
                <p className="text-2xl font-bold text-blue-900 mt-1">{summary.totalOrders}</p>
              </div>
              <div className="bg-green-50 rounded-lg p-4">
                <p className="text-xs text-green-600 font-medium uppercase tracking-wide">Total Revenue</p>
                <p className="text-2xl font-bold text-green-900 mt-1">{formatCurrency(summary.totalRevenue)}</p>
              </div>
            </div>

            {summary.tiers.length > 0 && (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100">
                    <th className="pb-2 text-left text-xs font-semibold text-gray-500 uppercase">Tier</th>
                    <th className="pb-2 text-right text-xs font-semibold text-gray-500 uppercase">Sold</th>
                    <th className="pb-2 text-right text-xs font-semibold text-gray-500 uppercase">Remaining</th>
                    <th className="pb-2 text-right text-xs font-semibold text-gray-500 uppercase">Revenue</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {summary.tiers.map(t => (
                    <tr key={t.tierId}>
                      <td className="py-2 font-medium text-gray-900">{t.tierName}</td>
                      <td className="py-2 text-right text-gray-600">{t.soldQty} / {t.totalQty}</td>
                      <td className="py-2 text-right text-gray-600">{t.remainingQty}</td>
                      <td className="py-2 text-right font-medium text-gray-900">{formatCurrency(t.revenue)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    </AdminLayout>
  );
};
