import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { ToastProvider } from '../../shared/components/Toast'
import { PortabilityWizard } from './PortabilityWizard'

function renderWizard() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <PortabilityWizard />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('PortabilityWizard', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    window.history.replaceState(null, '', '/')
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
      screen.getByRole('button', { name: 'Autorizar e consultar agenda' }),
    )

    await screen.findByText('parcela-1')
    await user.click(screen.getByRole('checkbox'))
    await user.click(screen.getByRole('button', { name: 'Continuar' }))

    await user.type(screen.getByLabelText('Credor cessionário'), 'creditor-x')
    await user.type(screen.getByLabelText('Banco'), '001')
    await user.type(screen.getByLabelText('Agência'), '1234')
    await user.type(screen.getByLabelText('Conta'), '56789-0')
    await user.click(screen.getByRole('button', { name: 'Revisar' }))

    expect(screen.getAllByText(/\*{4}89-0/).length).toBeGreaterThan(0)

    const submit = screen.getByRole('button', { name: 'Enviar portabilidade' })
    await user.click(submit)

    await waitFor(() => {
      const posts = fetchMock.mock.calls.filter(
        ([url, init]) =>
          String(url).includes('/operacoes-portabilidade') &&
          (init as RequestInit | undefined)?.method === 'POST',
      )
      expect(posts.length).toBeGreaterThanOrEqual(1)
    })

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

  it('recupera etapa e aceite a partir da URL e revalida a agenda', async () => {
    window.history.replaceState(
      null,
      '',
      '/?step=agenda&ben=ben-ui&conv=conv-1&aceite=auth-url&parcelas=parcela-1',
    )
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input)
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
        return { ok: false, status: 404, json: async () => ({}) }
      }),
    )

    renderWizard()
    expect(screen.getByRole('heading', { name: 'Parcelas elegíveis' })).toBeInTheDocument()
    expect(await screen.findByText('parcela-1')).toBeInTheDocument()
    expect(screen.getAllByText(/auth-url/).length).toBeGreaterThan(0)
    expect(window.location.search).toContain('aceite=auth-url')
    expect(window.location.search).toContain('step=agenda')
    expect(window.location.search).not.toContain('idem=')
    expect(window.location.search).not.toContain('banco=')
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
    await user.click(screen.getByRole('button', { name: 'Autorizar e consultar agenda' }))
    await screen.findByText('parcela-1')
    await user.click(screen.getByRole('checkbox'))
    await user.click(screen.getByRole('button', { name: 'Continuar' }))
    await user.type(screen.getByLabelText('Credor cessionário'), 'creditor-x')
    await user.type(screen.getByLabelText('Banco'), '001')
    await user.type(screen.getByLabelText('Agência'), '1234')
    await user.type(screen.getByLabelText('Conta'), '56789-0')
    await user.click(screen.getByRole('button', { name: 'Revisar' }))
    await user.click(screen.getByRole('button', { name: 'Enviar portabilidade' }))

    await screen.findByText((_, node) => {
      return node?.getAttribute?.('data-status') === 'CONFIRMADA'
    })
    expect(screen.queryByText(/consultando API/)).not.toBeInTheDocument()
    const callsAfterTerminal = operationCalls
    await new Promise((r) => setTimeout(r, 1800))
    expect(operationCalls).toBe(callsAfterTerminal)
  })
})
