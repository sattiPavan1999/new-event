import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { PaginatedResponse, OrganiserEvent } from '@/types/event'

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}))
vi.mock('@/services/event', () => ({
  eventService: { getOrganiserEvents: vi.fn() },
}))
vi.mock('@/components/organiser-layout', () => ({
  OrganiserLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

import { OrganiserDashboardView } from './organiser-dashboard.view'
import { useAuth } from '@/contexts/AuthContext'
import { eventService } from '@/services/event'

const mockUseAuth = vi.mocked(useAuth)
const mockGetOrganiserEvents = vi.mocked(eventService.getOrganiserEvents)

function renderView() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <OrganiserDashboardView />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

const emptyResponse: PaginatedResponse<OrganiserEvent> = {
  content: [], totalElements: 0, totalPages: 0,
}

const eventsResponse: PaginatedResponse<OrganiserEvent> = {
  content: [{
    id: 'evt-1',
    title: 'Summer Fest',
    status: 'PUBLISHED',
    category: 'CONCERT',
    eventDate: '2026-08-01T18:00:00Z',
    venue: { id: 'v1', name: 'Arena', city: 'Mumbai', country: 'India' },
    tiers: [],
  }],
  totalElements: 1, totalPages: 1,
}

describe('OrganiserDashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuth.mockReturnValue({
      user: { id: '1', email: 'alice@x.com', fullName: 'Alice', role: 'ORGANISER', isActive: true },
      isAuthenticated: true, accessToken: 'tok', refreshToken: 'ref',
      login: vi.fn(), logout: vi.fn(), isLoading: false,
    })
    mockGetOrganiserEvents.mockResolvedValue(emptyResponse)
  })

  it('renders the Dashboard heading', () => {
    renderView()
    expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument()
  })

  it('shows the welcome message with the user name', () => {
    renderView()
    expect(screen.getByText(/Welcome back, Alice/)).toBeInTheDocument()
  })

  it('shows the Create Event button', () => {
    renderView()
    expect(screen.getByRole('button', { name: /create event/i })).toBeInTheDocument()
  })

  it('shows empty state when there are no events', async () => {
    renderView()
    await waitFor(() => {
      expect(screen.getByText('No events yet.')).toBeInTheDocument()
    })
  })

  it('shows recent event title and status badge when events exist', async () => {
    mockGetOrganiserEvents.mockResolvedValue(eventsResponse)
    renderView()
    await waitFor(() => {
      expect(screen.getByText('Summer Fest')).toBeInTheDocument()
    })
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument()
  })
})
