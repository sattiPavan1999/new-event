import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { EventDetailView } from './event-detail.view'

vi.mock('@/contexts/AuthContext', () => ({ useAuth: vi.fn() }))
vi.mock('@/services/event', () => ({ eventService: { getPublicEvent: vi.fn() } }))
vi.mock('@/services/order', () => ({ orderService: { createOrder: vi.fn() } }))
vi.mock('@/components/buyer-layout', () => ({ BuyerLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div> }))

import { useAuth } from '@/contexts/AuthContext'
import { eventService } from '@/services/event'
import { orderService } from '@/services/order'
const mockUseAuth = vi.mocked(useAuth)
const mockEventService = vi.mocked(eventService)
const mockOrderService = vi.mocked(orderService)

const sampleEvent = {
  id: 'ev-1',
  title: 'Test Concert',
  description: 'A great show',
  category: 'CONCERT',
  status: 'PUBLISHED',
  eventDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
  bannerImageUrl: null,
  venue: { name: 'Arena', address: '1 Main St', city: 'Mumbai', country: 'India' },
  tiers: [{
    id: 'tier-1',
    name: 'General',
    description: null,
    price: 500,
    totalQty: 100,
    remainingQty: 50,
    maxPerOrder: 5,
    saleStartsAt: null,
    saleEndsAt: null,
  }],
}

function renderView(eventId = 'ev-1') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[`/events/${eventId}`]}>
        <Routes>
          <Route path="/events/:eventId" element={<EventDetailView />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('EventDetailView — wallet / insufficient balance', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuth.mockReturnValue({
      user: { id: 'u1', email: 'b@x.com', fullName: 'Bob', role: 'BUYER', isActive: true, walletBalance: 200 },
      isAuthenticated: true, accessToken: 'tok', refreshToken: 'ref',
      login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
    mockEventService.getPublicEvent.mockResolvedValue(sampleEvent as never)
  })

  it('shows insufficient balance error when order creation returns 402', async () => {
    mockOrderService.createOrder.mockRejectedValue({
      response: { status: 402, data: { message: 'Insufficient wallet balance. Required: ₹500' } },
    })

    renderView()

    const qtyInput = await screen.findByLabelText(/quantity for general/i)
    fireEvent.change(qtyInput, { target: { value: '1' } })

    const buyBtn = screen.getByRole('button', { name: /buy now/i })
    fireEvent.click(buyBtn)

    await waitFor(() =>
      expect(screen.getByText(/insufficient/i)).toBeInTheDocument(),
    )
  })
})
