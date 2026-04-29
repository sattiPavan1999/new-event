import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { OrderHistoryResponse } from '@/types/order'

vi.mock('@/services/order', () => ({
  orderService: { getMyBookings: vi.fn(), cancelOrder: vi.fn() },
}))
vi.mock('@/components/buyer-layout', () => ({
  BuyerLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))
vi.mock('@/contexts/AuthContext', () => ({ useAuth: vi.fn() }))

import { MyBookingsView } from './my-bookings.view'
import { orderService } from '@/services/order'
import { useAuth } from '@/contexts/AuthContext'

const mockGetMyBookings = vi.mocked(orderService.getMyBookings)
const mockUseAuth = vi.mocked(useAuth)

function renderView() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <MyBookingsView />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

const emptyResponse: OrderHistoryResponse = {
  content: [], page: 0, size: 10, totalElements: 0, totalPages: 0,
}

const bookingResponse: OrderHistoryResponse = {
  content: [{
    orderId: 'order-1',
    status: 'CONFIRMED',
    totalAmount: 3000,
    createdAt: '2026-01-01T00:00:00Z',
    items: [{
      orderItemId: 'item-1',
      tierName: 'General',
      eventTitle: 'Jazz Night',
      eventDate: '2026-06-01T18:00:00Z',
      venueName: 'Blue Note',
      quantity: 2,
      unitPrice: 1500,
    }],
  }],
  page: 0, size: 10, totalElements: 1, totalPages: 1,
}

describe('MyBookingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuth.mockReturnValue({
      user: { id: 'u1', email: 'b@x.com', fullName: 'Bob', role: 'BUYER', isActive: true },
      isAuthenticated: true, accessToken: 'tok',
      login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
  })

  it('shows the My Bookings heading immediately', () => {
    mockGetMyBookings.mockResolvedValue(emptyResponse)
    renderView()
    expect(screen.getByText('My Bookings')).toBeInTheDocument()
  })

  it('shows empty state when there are no bookings', async () => {
    mockGetMyBookings.mockResolvedValue(emptyResponse)
    renderView()
    await waitFor(() => {
      expect(screen.getByText("You haven't booked any tickets yet.")).toBeInTheDocument()
    })
  })

  it('shows booking cards when bookings exist', async () => {
    mockGetMyBookings.mockResolvedValue(bookingResponse)
    renderView()
    await waitFor(() => {
      expect(screen.getByText('Jazz Night')).toBeInTheDocument()
    })
    expect(screen.getByText('CONFIRMED')).toBeInTheDocument()
  })

  it('shows error alert when the request fails', async () => {
    mockGetMyBookings.mockRejectedValue(new Error('Network error'))
    renderView()
    await waitFor(() => {
      expect(screen.getByText('Failed to load bookings. Please try again.')).toBeInTheDocument()
    })
  })
})
