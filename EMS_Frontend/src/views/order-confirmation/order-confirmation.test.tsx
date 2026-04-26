import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import type { OrderConfirmationState } from '@/types/order'

vi.mock('@/components/buyer-layout', () => ({
  BuyerLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

vi.mock('@/components/button', () => ({
  Button: ({ children, onClick }: { children: React.ReactNode; onClick?: () => void }) => (
    <button onClick={onClick}>{children}</button>
  ),
}))

import { OrderConfirmationView } from './order-confirmation.view'

function renderWithState(state: OrderConfirmationState | null) {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/order-confirmation', state }]}>
      <Routes>
        <Route path="/order-confirmation" element={<OrderConfirmationView />} />
      </Routes>
    </MemoryRouter>
  )
}

const validState: OrderConfirmationState = {
  orderId: 'order-abc-123',
  status: 'CONFIRMED',
  eventTitle: 'Rock Concert 2026',
  totalAmount: 5000,
  items: [
    { tierName: 'VIP', quantity: 2, unitPrice: 2500 },
  ],
}

describe('OrderConfirmationView', () => {
  it('shows fallback message when state is null', () => {
    renderWithState(null)
    expect(screen.getByText('Order details not found.')).toBeInTheDocument()
  })

  it('shows Payment Successful heading for valid state', () => {
    renderWithState(validState)
    expect(screen.getByText('Payment Successful')).toBeInTheDocument()
  })

  it('displays event title', () => {
    renderWithState(validState)
    expect(screen.getByText('Rock Concert 2026')).toBeInTheDocument()
  })

  it('displays order status badge', () => {
    renderWithState(validState)
    expect(screen.getByText('CONFIRMED')).toBeInTheDocument()
  })

  it('displays ticket tier and quantity', () => {
    renderWithState(validState)
    expect(screen.getByText(/VIP × 2/)).toBeInTheDocument()
  })
})
