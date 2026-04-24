import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { eventService } from '@/services/event';
import { AdminLayout } from '@/components/admin-layout';
import { Button } from '@/components/button';
import { Input } from '@/components/input';
import { Alert } from '@/components/alert';
import { EventCategory } from '@/types/event';

export const AdminEventCreateView: React.FC = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    title: '',
    description: '',
    category: '' as string,
    eventDate: '',
    venueId: '',
    bannerImageUrl: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);

  const { data: venues = [] } = useQuery({
    queryKey: ['venues'],
    queryFn: () => eventService.getVenues(),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      eventService.createEvent({
        title: form.title,
        description: form.description || undefined,
        category: form.category as typeof EventCategory[keyof typeof EventCategory],
        eventDate: form.eventDate,
        venueId: form.venueId,
        bannerImageUrl: form.bannerImageUrl || undefined,
      }),
    onSuccess: (event) => {
      navigate(`/admin/events/${event.id}`);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setServerError(msg ?? 'Failed to create event. Please try again.');
    },
  });

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!form.title.trim()) newErrors.title = 'Title is required';
    if (!form.category) newErrors.category = 'Category is required';
    if (!form.eventDate) newErrors.eventDate = 'Event date is required';
    if (!form.venueId) newErrors.venueId = 'Venue is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);
    if (validate()) createMutation.mutate();
  };

  const handleChange = (field: string, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: '' }));
  };

  return (
    <AdminLayout>
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <button
            onClick={() => navigate('/admin/events')}
            className="text-gray-400 hover:text-gray-600"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h1 className="text-2xl font-bold text-gray-900">Create Event</h1>
        </div>

        {serverError && (
          <Alert variant="error" className="mb-4">{serverError}</Alert>
        )}

        <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 space-y-5">
          <Input
            label="Event Title"
            value={form.title}
            onChange={e => handleChange('title', e.target.value)}
            error={errors.title}
            placeholder="Enter event title"
            fullWidth
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea
              value={form.description}
              onChange={e => handleChange('description', e.target.value)}
              rows={3}
              placeholder="Describe your event..."
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm resize-none"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
              <select
                value={form.category}
                onChange={e => handleChange('category', e.target.value)}
                className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm ${errors.category ? 'border-red-500' : 'border-gray-300'}`}
              >
                <option value="">Select category</option>
                {Object.values(EventCategory).map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
              {errors.category && <p className="text-red-600 text-xs mt-1">{errors.category}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Event Date & Time</label>
              <input
                type="datetime-local"
                value={form.eventDate}
                onChange={e => handleChange('eventDate', e.target.value)}
                className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm ${errors.eventDate ? 'border-red-500' : 'border-gray-300'}`}
              />
              {errors.eventDate && <p className="text-red-600 text-xs mt-1">{errors.eventDate}</p>}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Venue</label>
            <select
              value={form.venueId}
              onChange={e => handleChange('venueId', e.target.value)}
              className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm ${errors.venueId ? 'border-red-500' : 'border-gray-300'}`}
            >
              <option value="">Select venue</option>
              {venues.map(venue => (
                <option key={venue.id} value={venue.id}>
                  {venue.name} — {venue.city}
                </option>
              ))}
            </select>
            {errors.venueId && <p className="text-red-600 text-xs mt-1">{errors.venueId}</p>}
          </div>

          <Input
            label="Banner Image URL (optional)"
            value={form.bannerImageUrl}
            onChange={e => handleChange('bannerImageUrl', e.target.value)}
            placeholder="https://..."
            fullWidth
          />

          <div className="flex gap-3 pt-2">
            <Button type="submit" isLoading={createMutation.isPending}>
              Create Event
            </Button>
            <Button type="button" variant="secondary" onClick={() => navigate('/admin/events')}>
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </AdminLayout>
  );
};
