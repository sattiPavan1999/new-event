import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}))
vi.mock('@/components/logout-dialog/logout-dialog', () => ({
  LogoutDialog: () => null,
}))

import { AdminLayout } from './admin-layout'
import { useAuth } from '@/contexts/AuthContext'

const mockUseAuth = vi.mocked(useAuth)

function renderInRouter(ui: React.ReactNode) {
  return render(<MemoryRouter>{ui}</MemoryRouter>)
}

describe('AdminLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseAuth.mockReturnValue({
      user: { id: '1', email: 'alice@x.com', fullName: 'Alice', role: 'ORGANISER', isActive: true },
      isAuthenticated: true, accessToken: 'tok', refreshToken: 'ref',
      login: vi.fn(), logout: vi.fn(), isLoading: false,
    })
  })

  it('renders children in the main content area', () => {
    renderInRouter(<AdminLayout><span>page content</span></AdminLayout>)
    expect(screen.getByText('page content')).toBeInTheDocument()
  })

  it('shows Dashboard and Events nav links', () => {
    renderInRouter(<AdminLayout><span /></AdminLayout>)
    expect(screen.getByRole('link', { name: /dashboard/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /events/i })).toBeInTheDocument()
  })

  it('shows the organiser name in the sidebar', () => {
    renderInRouter(<AdminLayout><span /></AdminLayout>)
    expect(screen.getByText('Alice')).toBeInTheDocument()
  })

  it('shows the Logout button', () => {
    renderInRouter(<AdminLayout><span /></AdminLayout>)
    expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument()
  })

  it('shows the Public Site button', () => {
    renderInRouter(<AdminLayout><span /></AdminLayout>)
    expect(screen.getByRole('button', { name: /public site/i })).toBeInTheDocument()
  })
})
