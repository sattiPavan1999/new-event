import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { EventDetailView } from './event-detail.view'

vi.mock('@/contexts/AuthContext', () => ({ useAuth: vi.fn() }))
vi.mock('@/contexts/CartContext', () => ({ useCart: vi.fn() }))
vi.mock('@/services/event', () => ({ eventService: { getPublicEvent: vi.fn() } }))
vi.mock('@/components/buyer-layout', () => ({ BuyerLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div> }))

import { useAuth } from '@/contexts/AuthContext'
import { useCart } from '@/contexts/CartContext'
import { eventService } from '@/services/event'

const mockUseAuth = vi.mocked(useAuth)
const mockUseCart = vi.mocked(useCart)
const mockEventService = vi.mocked(eventService)

const sampleEvent = {
  id: 1,
  title: 'Test Concert',
  description: 'A great show',
  category: 'CONCERT',
  status: 'PUBLISHED',
  eventDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
  bannerImageUrl: null,
  venue: { name: 'Arena', address: '1 Main St', city: 'Mumbai', country: 'India' },
  tiers: [{
    id: 1,
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

function renderView(eventId = '1') {
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

describe('EventDetailView — add to cart', () => {
  const mockAddItems = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuth.mockReturnValue({
      user: { id: 1, email: 'b@x.com', fullName: 'Bob', role: 'BUYER', isActive: true, walletBalance: 200 },
      isAuthenticated: true, accessToken: 'tok',
      login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
    mockUseCart.mockReturnValue({
      items: [], addItems: mockAddItems, removeEvent: vi.fn(), clearCart: vi.fn(),
      totalCount: 0, totalAmount: 0,
    })
    mockEventService.getPublicEvent.mockResolvedValue(sampleEvent as never)
  })

  it('renders Add to Cart button when tickets are available', async () => {
    renderView()
    await screen.findByLabelText(/quantity for general/i)
    expect(screen.getByRole('button', { name: /add to cart/i })).toBeInTheDocument()
  })

  it('button is disabled until a quantity is selected', async () => {
    renderView()
    const btn = await screen.findByRole('button', { name: /add to cart/i })
    expect(btn).toBeDisabled()

    const qtyInput = screen.getByLabelText(/quantity for general/i)
    fireEvent.change(qtyInput, { target: { value: '1' } })
    expect(btn).not.toBeDisabled()
  })

  it('calls cart.addItems with selected tiers when Add to Cart is clicked', async () => {
    renderView()

    const qtyInput = await screen.findByLabelText(/quantity for general/i)
    fireEvent.change(qtyInput, { target: { value: '2' } })

    fireEvent.click(screen.getByRole('button', { name: /add to cart/i }))

    await waitFor(() =>
      expect(mockAddItems).toHaveBeenCalledWith(
        1,
        'Test Concert',
        [{ tierId: 1, tierName: 'General', quantity: 2, unitPrice: 500, maxQty: 5 }],
      )
    )
  })
})
