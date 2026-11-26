import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  it('renderiza o shell do console e o fluxo de portabilidade', () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          service: 'portability-api',
          milestone: 'M10',
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

    expect(screen.getAllByAltText('Cielo').length).toBeGreaterThanOrEqual(1)
    expect(screen.getAllByText('Portabilidade de Folha').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText(/Case Cielo/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Nova operação' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Autorização do beneficiário' })).toBeInTheDocument()
    expect(screen.getByAltText('Inteligência de Mercado Cielo')).toBeInTheDocument()
  })
})
