import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  it('renderiza a marca do console operacional e o assistente', () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          service: 'portability-api',
          milestone: 'M5',
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

    expect(screen.getByText('Portabilidade de Folha')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Console operacional' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Fluxo de portabilidade' })).toBeInTheDocument()
  })
})
