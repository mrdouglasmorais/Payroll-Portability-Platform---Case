import { afterEach, describe, expect, it, vi } from 'vitest'
import { portabilityApiService } from './portabilityApiService'

describe('portabilityApiService', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('getStatus hits /api/v1/status', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        service: 'portability-api',
        milestone: 'M10',
        status: 'UP',
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    const status = await portabilityApiService.getStatus()

    expect(status.status).toBe('UP')
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain('/api/v1/status')
  })

  it('authorizeAndLoadAgenda returns auth and agenda together', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      if (url.includes('/autorizacoes') && init?.method === 'POST') {
        return {
          ok: true,
          status: 201,
          json: async () => ({ autorizacaoId: 'auth-1' }),
        }
      }
      if (url.includes('/agenda')) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            {
              parcelaId: 'p1',
              convenioId: 'conv-1',
              valor: 10,
              dataDesconto: '2026-12-01',
              status: 'OPEN',
            },
          ],
        }
      }
      return { ok: false, status: 404, json: async () => ({}) }
    })
    vi.stubGlobal('fetch', fetchMock)

    const result = await portabilityApiService.authorizeAndLoadAgenda({
      beneficiarioId: 'ben-ui',
      convenioId: 'conv-1',
    })

    expect(result.autorizacaoId).toBe('auth-1')
    expect(result.items).toHaveLength(1)
  })
})
