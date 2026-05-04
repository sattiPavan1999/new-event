import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { BuyerLayout } from './buyer-layout'

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}))

vi.mock('@/contexts/CartContext', () => ({
  useCart: vi.fn(() => ({ totalCount: 0, items: [], totalAmount: 0 })),
}))

vi.mock('@/components/logout-dialog/logout-dialog', () => ({
  LogoutDialog: () => null,
}))

import { useAuth } from '@/contexts/AuthContext'
const mockUseAuth = vi.mocked(useAuth)

function renderInRouter(ui: React.ReactNode) {
  return render(<MemoryRouter>{ui}</MemoryRouter>)
}

describe('BuyerLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders children', () => {
    mockUseAuth.mockReturnValue({
      user: null, isAuthenticated: false, accessToken: null,       login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
    renderInRouter(<BuyerLayout><span>child content</span></BuyerLayout>)
    expect(screen.getByText('child content')).toBeInTheDocument()
  })

  it('shows My Bookings link for BUYER', () => {
    mockUseAuth.mockReturnValue({
      user: { id: '1', email: 'b@x.com', fullName: 'Bob', role: 'BUYER', isActive: true },
      isAuthenticated: true, accessToken: 'tok',       login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
    renderInRouter(<BuyerLayout><span /></BuyerLayout>)
    expect(screen.getByText('My Bookings')).toBeInTheDocument()
  })

  it('hides My Bookings link for non-BUYER role', () => {
    mockUseAuth.mockReturnValue({
      user: { id: '2', email: 'a@x.com', fullName: 'Alice', role: 'ORGANISER', isActive: true },
      isAuthenticated: true, accessToken: 'tok',       login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
    renderInRouter(<BuyerLayout><span /></BuyerLayout>)
    expect(screen.queryByText('My Bookings')).not.toBeInTheDocument()
  })

  it('shows user greeting and logout button when authenticated', () => {
    mockUseAuth.mockReturnValue({
      user: { id: '1', email: 'b@x.com', fullName: 'Bob', role: 'BUYER', isActive: true },
      isAuthenticated: true, accessToken: 'tok',       login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
    renderInRouter(<BuyerLayout><span /></BuyerLayout>)
    expect(screen.getByText('Hi, Bob')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument()
  })

  it('hides user info when unauthenticated', () => {
    mockUseAuth.mockReturnValue({
      user: null, isAuthenticated: false, accessToken: null,       login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
    renderInRouter(<BuyerLayout><span /></BuyerLayout>)
    expect(screen.queryByText(/Hi,/)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /logout/i })).not.toBeInTheDocument()
  })

  it('shows wallet balance badge for BUYER with walletBalance', () => {
    mockUseAuth.mockReturnValue({
      user: { id: '1', email: 'b@x.com', fullName: 'Bob', role: 'BUYER', isActive: true, walletBalance: 10000 },
      isAuthenticated: true, accessToken: 'tok',       login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
    renderInRouter(<BuyerLayout><span /></BuyerLayout>)
    expect(screen.getByText('₹10,000')).toBeInTheDocument()
  })

  it('hides wallet badge for ORGANISER', () => {
    mockUseAuth.mockReturnValue({
      user: { id: '2', email: 'a@x.com', fullName: 'Alice', role: 'ORGANISER', isActive: true, walletBalance: 10000 },
      isAuthenticated: true, accessToken: 'tok',       login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
    renderInRouter(<BuyerLayout><span /></BuyerLayout>)
    expect(screen.queryByText('₹10,000')).not.toBeInTheDocument()
  })
})
