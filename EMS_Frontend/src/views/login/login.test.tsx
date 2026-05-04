import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
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
import { authService } from '@/services/auth'

const mockUseAuth = vi.mocked(useAuth)
const mockLogin = vi.mocked(authService.login)

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
      user: null, isAuthenticated: false, accessToken: null,
      login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
  })

  afterEach(() => {
    vi.useRealTimers()
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

  it('shows "Wrong credentials" error when the server returns 401', async () => {
    const user = userEvent.setup()
    mockLogin.mockRejectedValueOnce({
      response: { status: 401, data: { errorCode: 'INVALID_CREDENTIALS', message: 'Invalid email or password' } },
    })

    renderView()
    await user.type(screen.getByLabelText('Email address'), 'test@example.com')
    await user.type(screen.getByLabelText('Password'), 'wrongpassword')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText('Wrong credentials')).toBeInTheDocument()
  })

  it('clears the error message when a new login attempt begins', async () => {
    const user = userEvent.setup()
    mockLogin.mockRejectedValueOnce({
      response: { status: 401, data: { errorCode: 'INVALID_CREDENTIALS', message: 'Invalid email or password' } },
    })

    renderView()
    await user.type(screen.getByLabelText('Email address'), 'test@example.com')
    await user.type(screen.getByLabelText('Password'), 'wrongpassword')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText('Wrong credentials')).toBeInTheDocument()

    mockLogin.mockResolvedValueOnce({
      accessToken: 'token',
      user: { id: '1', email: 'test@example.com', fullName: 'Test', role: 'BUYER', isActive: true, createdAt: '', walletBalance: 10000 },
    })
    await user.type(screen.getByLabelText('Password'), 'correctpassword')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(screen.queryByText('Wrong credentials')).not.toBeInTheDocument()
    })
  })

  it('auto-clears the "Wrong credentials" error after 5 seconds', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime.bind(vi) })
    mockLogin.mockRejectedValueOnce({
      response: { status: 401, data: { errorCode: 'INVALID_CREDENTIALS', message: 'Invalid email or password' } },
    })

    renderView()
    await user.type(screen.getByLabelText('Email address'), 'test@example.com')
    await user.type(screen.getByLabelText('Password'), 'wrongpassword')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText('Wrong credentials')).toBeInTheDocument()

    act(() => vi.advanceTimersByTime(5000))

    expect(screen.queryByText('Wrong credentials')).not.toBeInTheDocument()
  })
})
