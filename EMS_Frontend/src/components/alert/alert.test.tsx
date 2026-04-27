import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Alert } from './alert'

describe('Alert', () => {
  it('renders children', () => {
    render(<Alert>Something went wrong</Alert>)
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
  })

  it('has role="alert"', () => {
    render(<Alert>Message</Alert>)
    expect(screen.getByRole('alert')).toBeInTheDocument()
  })

  it('renders error variant', () => {
    render(<Alert variant="error">Error occurred</Alert>)
    expect(screen.getByRole('alert')).toHaveTextContent('Error occurred')
  })

  it('renders success variant', () => {
    render(<Alert variant="success">Done!</Alert>)
    expect(screen.getByRole('alert')).toHaveTextContent('Done!')
  })

  it('defaults to info variant without crashing', () => {
    render(<Alert>Default info</Alert>)
    expect(screen.getByRole('alert')).toBeInTheDocument()
  })
})
