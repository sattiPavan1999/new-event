import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import userEvent from '@testing-library/user-event'

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}))
vi.mock('@/services/auth', () => ({
  authService: { register: vi.fn() },
}))

import { BuyerRegistrationView } from './buyer-registration.view'
import { useAuth } from '@/contexts/AuthContext'

const mockUseAuth = vi.mocked(useAuth)

function renderView() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <BuyerRegistrationView />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('BuyerRegistrationView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuth.mockReturnValue({
      user: null, isAuthenticated: false, accessToken: null, refreshToken: null,
      login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
  })

  it('renders the create account heading', () => {
    renderView()
    expect(screen.getByRole('heading', { name: /create your buyer account/i })).toBeInTheDocument()
  })

  it('shows required field errors when the form is submitted empty', async () => {
    const user = userEvent.setup()
    renderView()
    await user.click(screen.getByRole('button', { name: /create account/i }))
    expect(screen.getByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Full name is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
  })

  it('shows error when password is shorter than 8 characters', async () => {
    const user = userEvent.setup()
    renderView()
    await user.type(screen.getByLabelText('Email address'), 'test@example.com')
    await user.type(screen.getByLabelText('Full Name'), 'Jane Doe')
    await user.type(screen.getByLabelText('Password'), 'abc')
    await user.click(screen.getByRole('button', { name: /create account/i }))
    expect(screen.getByText('Password must be at least 8 characters')).toBeInTheDocument()
  })

  it('shows error when password contains no digit', async () => {
    const user = userEvent.setup()
    renderView()
    await user.type(screen.getByLabelText('Email address'), 'test@example.com')
    await user.type(screen.getByLabelText('Full Name'), 'Jane Doe')
    await user.type(screen.getByLabelText('Password'), 'abcdefgh')
    await user.click(screen.getByRole('button', { name: /create account/i }))
    expect(screen.getByText('Password must contain at least one digit')).toBeInTheDocument()
  })
})
