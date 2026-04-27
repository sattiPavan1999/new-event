import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import userEvent from '@testing-library/user-event'

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}))
vi.mock('@/services/auth', () => ({
  authService: { login: vi.fn() },
}))

import { LoginView } from './login.view'
import { useAuth } from '@/contexts/AuthContext'

const mockUseAuth = vi.mocked(useAuth)

function renderView() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <LoginView />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('LoginView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuth.mockReturnValue({
      user: null, isAuthenticated: false, accessToken: null, refreshToken: null,
      login: vi.fn(), logout: vi.fn(), isLoading: false,
    })
  })

  it('renders the sign in heading', () => {
    renderView()
    expect(screen.getByRole('heading', { name: /sign in to your account/i })).toBeInTheDocument()
  })

  it('renders email and password fields', () => {
    renderView()
    expect(screen.getByLabelText('Email address')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
  })

  it('shows email required error when submitted with empty email', async () => {
    const user = userEvent.setup()
    renderView()
    await user.click(screen.getByRole('button', { name: /sign in/i }))
    expect(screen.getByText('Email is required')).toBeInTheDocument()
  })

  it('shows invalid email error for bad email format', () => {
    const { container } = renderView()
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: 'notanemail' } })
    // fireEvent.submit bypasses browser native email validation so React's handler runs
    fireEvent.submit(container.querySelector('form')!)
    expect(screen.getByText('Please enter a valid email address')).toBeInTheDocument()
  })

  it('shows password required error when email is valid but password is empty', async () => {
    const user = userEvent.setup()
    renderView()
    await user.type(screen.getByLabelText('Email address'), 'test@example.com')
    await user.click(screen.getByRole('button', { name: /sign in/i }))
    expect(screen.getByText('Password is required')).toBeInTheDocument()
  })
})
