import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { MyBookingsView } from './my-bookings.view'

vi.mock('@/contexts/AuthContext', () => ({ useAuth: vi.fn() }))
vi.mock('@/services/order', () => ({ orderService: { getMyBookings: vi.fn(), cancelOrder: vi.fn() } }))
vi.mock('@/components/buyer-layout', () => ({ BuyerLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div> }))

import { useAuth } from '@/contexts/AuthContext'
import { orderService } from '@/services/order'
const mockUseAuth = vi.mocked(useAuth)
const mockOrderService = vi.mocked(orderService)

const TWO_DAYS_MS = Date.now() + 2 * 24 * 60 * 60 * 1000
const SIX_HOURS_MS = Date.now() + 6 * 60 * 60 * 1000

function buildOrder(overrides: { status?: string; eventDate?: number }) {
  return {
    orderId: 'order-1',
    status: overrides.status ?? 'CONFIRMED',
    totalAmount: 500,
    createdAt: new Date().toISOString(),
    items: [{
      orderItemId: 'item-1',
      tierName: 'General',
      eventTitle: 'Test Event',
      eventDate: new Date(overrides.eventDate ?? TWO_DAYS_MS).toISOString(),
      quantity: 1,
      unitPrice: 500,
      venueName: 'Venue',
    }],
  }
}

function renderView() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <MyBookingsView />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('MyBookingsView — cancel button visibility', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuth.mockReturnValue({
      user: { id: 'u1', email: 'a@b.com', fullName: 'A', role: 'BUYER', isActive: true },
      isAuthenticated: true, accessToken: 'tok', refreshToken: 'ref',
      login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
  })

  it('shows Cancel Order button for CONFIRMED order with event > 24h away', async () => {
    mockOrderService.getMyBookings.mockResolvedValue({
      content: [buildOrder({ status: 'CONFIRMED', eventDate: TWO_DAYS_MS })],
      page: 0, size: 10, totalElements: 1, totalPages: 1,
    })
    renderView()
    await waitFor(() => expect(screen.getByRole('button', { name: /cancel order/i })).toBeInTheDocument())
  })

  it('hides Cancel Order button for event < 24h away', async () => {
    mockOrderService.getMyBookings.mockResolvedValue({
      content: [buildOrder({ status: 'CONFIRMED', eventDate: SIX_HOURS_MS })],
      page: 0, size: 10, totalElements: 1, totalPages: 1,
    })
    renderView()
    await waitFor(() => screen.getByText('Test Event'))
    expect(screen.queryByRole('button', { name: /cancel order/i })).not.toBeInTheDocument()
  })

  it('hides Cancel Order button for PENDING order', async () => {
    mockOrderService.getMyBookings.mockResolvedValue({
      content: [buildOrder({ status: 'PENDING', eventDate: TWO_DAYS_MS })],
      page: 0, size: 10, totalElements: 1, totalPages: 1,
    })
    renderView()
    await waitFor(() => screen.getByText('Test Event'))
    expect(screen.queryByRole('button', { name: /cancel order/i })).not.toBeInTheDocument()
  })

  it('calls updateWalletBalance after successful cancel', async () => {
    const updateWalletBalance = vi.fn()
    mockUseAuth.mockReturnValue({
      user: { id: 'u1', email: 'a@b.com', fullName: 'A', role: 'BUYER', isActive: true },
      isAuthenticated: true, accessToken: 'tok', refreshToken: 'ref',
      login: vi.fn(), logout: vi.fn(), updateWalletBalance, isLoading: false,
    })
    mockOrderService.getMyBookings.mockResolvedValue({
      content: [buildOrder({ status: 'CONFIRMED', eventDate: TWO_DAYS_MS })],
      page: 0, size: 10, totalElements: 1, totalPages: 1,
    })
    mockOrderService.cancelOrder.mockResolvedValue({
      orderId: 'order-1', status: 'CANCELLED',
      message: 'Cancelled', remainingBalance: 9500,
    })

    vi.spyOn(window, 'confirm').mockReturnValue(true)

    renderView()
    const btn = await screen.findByRole('button', { name: /cancel order/i })
    fireEvent.click(btn)

    await waitFor(() => expect(mockOrderService.cancelOrder).toHaveBeenCalledWith('order-1'))
    await waitFor(() => expect(updateWalletBalance).toHaveBeenCalledWith(9500))
  })
})
