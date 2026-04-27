import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Input } from './input'

describe('Input', () => {
  it('renders with a label linked to the input', () => {
    render(<Input label="Email address" />)
    expect(screen.getByLabelText('Email address')).toBeInTheDocument()
  })

  it('renders without a label', () => {
    render(<Input placeholder="Enter value" />)
    expect(screen.getByRole('textbox')).toBeInTheDocument()
  })

  it('shows error message when error prop is provided', () => {
    render(<Input label="Email" error="Email is required" />)
    expect(screen.getByRole('alert')).toHaveTextContent('Email is required')
  })

  it('marks input as invalid when error is provided', () => {
    render(<Input label="Email" error="Invalid email" />)
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
  })

  it('does not show error element when error prop is absent', () => {
    render(<Input label="Email" />)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
