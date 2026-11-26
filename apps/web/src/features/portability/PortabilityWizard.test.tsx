import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { PortabilityWizard } from './PortabilityWizard'

function renderWizard() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <PortabilityWizard />
    </QueryClientProvider>,
  )
}

describe('PortabilityWizard', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('percorre consentimento até agenda e reutiliza a chave de idempotência no reenvio', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      if (url.includes('/autorizacoes') && init?.method === 'POST') {
        return {
          ok: true,
          status: 201,
          json: async () => ({ autorizacaoId: 'auth-test' }),
        }
      }
      if (url.includes('/agenda')) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            {
              parcelaId: 'parcela-1',
              convenioId: 'conv-1',
              valor: 100,
              dataDesconto: '2026-12-10',
              status: 'OPEN',
            },
          ],
        }
      }
      if (url.includes('/operacoes-portabilidade') && init?.method === 'POST') {
        return {
          ok: false,
          status: 503,
          json: async () => ({ error: 'Central unavailable' }),
        }
      }
      return { ok: false, status: 404, json: async () => ({ error: 'missing' }) }
    })
    vi.stubGlobal('fetch', fetchMock)

    renderWizard()

    await user.click(
      screen.getByRole('button', { name: 'Autorizar e carregar agenda' }),
    )

    await screen.findByText('parcela-1')
    await user.click(screen.getByRole('checkbox'))
    await user.click(screen.getByRole('button', { name: 'Continuar' }))

    await user.type(screen.getByLabelText('Credor cessionário'), 'creditor-x')
    await user.type(screen.getByLabelText('Banco'), '001')
    await user.type(screen.getByLabelText('Agência'), '1234')
    await user.type(screen.getByLabelText('Conta'), '56789-0')
    await user.click(screen.getByRole('button', { name: 'Revisar' }))

    expect(screen.getByText(/\*{4}89-0/)).toBeInTheDocument()

    const submit = screen.getByRole('button', { name: 'Enviar portabilidade' })
    await user.click(submit)
    await screen.findByRole('alert')

    await user.click(screen.getByRole('button', { name: 'Enviar portabilidade' }))

    await waitFor(() => {
      const posts = fetchMock.mock.calls.filter(
        ([url, init]) =>
          String(url).includes('/operacoes-portabilidade') &&
          (init as RequestInit | undefined)?.method === 'POST',
      )
      expect(posts.length).toBe(2)
      const key1 = new Headers(posts[0][1]?.headers).get('Idempotency-Key')
      const key2 = new Headers(posts[1][1]?.headers).get('Idempotency-Key')
      expect(key1).toBeTruthy()
      expect(key1).toBe(key2)
    })
  })

  it('para de anunciar consulta contínua quando o status é terminal', async () => {
    const user = userEvent.setup()
    let operationCalls = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        if (url.includes('/autorizacoes')) {
          return {
            ok: true,
            status: 201,
            json: async () => ({ autorizacaoId: 'auth-test' }),
          }
        }
        if (url.includes('/agenda')) {
          return {
            ok: true,
            status: 200,
            json: async () => [
              {
                parcelaId: 'parcela-1',
                convenioId: 'conv-1',
                valor: 100,
                dataDesconto: '2026-12-10',
                status: 'OPEN',
              },
            ],
          }
        }
        if (url.includes('/operacoes-portabilidade') && init?.method === 'POST') {
          return {
            ok: true,
            status: 202,
            json: async () => ({ operationId: 'op-1' }),
          }
        }
        if (url.endsWith('/operacoes-portabilidade/op-1')) {
          operationCalls += 1
          return {
            ok: true,
            status: 200,
            json: async () => ({
              status: 'CONFIRMADA',
              motivoRejeicao: null,
            }),
          }
        }
        if (url.includes('/eventos')) {
          return {
            ok: true,
            status: 200,
            json: async () => [
              {
                eventType: 'OPERATION_CREATED',
                sequence: 1,
                occurredAt: '2026-11-25T00:00:00Z',
                description: 'created',
              },
            ],
          }
        }
        return { ok: false, status: 404, json: async () => ({}) }
      }),
    )

    renderWizard()
    await user.click(screen.getByRole('button', { name: 'Autorizar e carregar agenda' }))
    await screen.findByText('parcela-1')
    await user.click(screen.getByRole('checkbox'))
    await user.click(screen.getByRole('button', { name: 'Continuar' }))
    await user.type(screen.getByLabelText('Credor cessionário'), 'creditor-x')
    await user.type(screen.getByLabelText('Banco'), '001')
    await user.type(screen.getByLabelText('Agência'), '1234')
    await user.type(screen.getByLabelText('Conta'), '56789-0')
    await user.click(screen.getByRole('button', { name: 'Revisar' }))
    await user.click(screen.getByRole('button', { name: 'Enviar portabilidade' }))

    await screen.findByText('Confirmada')
    expect(screen.queryByText(/consultando API local/)).not.toBeInTheDocument()
    const callsAfterTerminal = operationCalls
    await new Promise((r) => setTimeout(r, 1800))
    expect(operationCalls).toBe(callsAfterTerminal)
  })
})
