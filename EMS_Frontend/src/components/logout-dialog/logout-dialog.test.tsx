import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}))
vi.mock('@/services/auth', () => ({
  authService: { logout: vi.fn() },
}))
vi.mock('@/components/button', () => ({
  Button: ({ children, onClick, disabled }: { children: React.ReactNode; onClick?: () => void; disabled?: boolean }) => (
    <button onClick={onClick} disabled={disabled}>{children}</button>
  ),
}))

import { LogoutDialog } from './logout-dialog'
import { useAuth } from '@/contexts/AuthContext'

const mockUseAuth = vi.mocked(useAuth)

function renderDialog(props: { isOpen: boolean; onClose: () => void }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <LogoutDialog {...props} />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('LogoutDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuth.mockReturnValue({
      user: null, isAuthenticated: false, accessToken: null, refreshToken: 'ref-tok',
      login: vi.fn(), logout: vi.fn(), updateWalletBalance: vi.fn(), isLoading: false,
    })
  })

  it('renders nothing when isOpen is false', () => {
    renderDialog({ isOpen: false, onClose: vi.fn() })
    expect(screen.queryByText('Confirm Logout')).not.toBeInTheDocument()
  })

  it('renders dialog content when isOpen is true', () => {
    renderDialog({ isOpen: true, onClose: vi.fn() })
    expect(screen.getByText('Confirm Logout')).toBeInTheDocument()
    expect(screen.getByText(/Are you sure you want to log out/)).toBeInTheDocument()
  })

  it('calls onClose when Cancel is clicked', () => {
    const onClose = vi.fn()
    renderDialog({ isOpen: true, onClose })
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('shows a Logout button', () => {
    renderDialog({ isOpen: true, onClose: vi.fn() })
    expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument()
  })
})
