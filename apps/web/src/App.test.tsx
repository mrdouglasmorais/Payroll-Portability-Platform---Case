import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  it('renders the operational console brand', () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          service: 'portability-api',
          milestone: 'M1',
          status: 'UP',
        }),
      }),
    )

    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={client}>
        <App />
      </QueryClientProvider>,
    )

    expect(screen.getByText('Payroll Portability')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Operational console' })).toBeInTheDocument()
  })
})
