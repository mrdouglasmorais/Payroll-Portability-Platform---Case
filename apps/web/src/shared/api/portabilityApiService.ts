import { httpRequest, isApiError } from './http'
import type {
  AgendaItem,
  ApiStatus,
  AuthorizationRequest,
  OperationEvent,
  OperationPage,
  OperationStatus,
  PortabilityRequest,
} from './types'

/**
 * Single façade for every portability-api call used by the console.
 * Features should depend on this service — not on raw fetch.
 */
export const portabilityApiService = {
  getStatus() {
    return httpRequest<ApiStatus>('/api/v1/status', { timeoutMs: 8_000 })
  },

  createAuthorization(body: AuthorizationRequest) {
    return httpRequest<{ autorizacaoId: string }>('/api/v1/autorizacoes', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  fetchAgenda(beneficiarioId: string) {
    return httpRequest<AgendaItem[]>(
      `/api/v1/beneficiarios/${encodeURIComponent(beneficiarioId)}/agenda`,
    )
  },

  /**
   * Opt-in then load agenda. Surfaces a distinct message when authorization
   * succeeded but the agenda call failed.
   */
  async authorizeAndLoadAgenda(body: AuthorizationRequest) {
    const auth = await this.createAuthorization(body)
    try {
      const items = await this.fetchAgenda(body.beneficiarioId)
      return { autorizacaoId: auth.autorizacaoId, items }
    } catch (agendaError) {
      if (isApiError(agendaError)) {
        throw {
          ...agendaError,
          message: `Autorização ok (${auth.autorizacaoId}), mas a agenda falhou: ${agendaError.message}`,
        }
      }
      throw agendaError
    }
  },

  createPortability(body: PortabilityRequest, idempotencyKey: string) {
    return httpRequest<{ operationId: string }>('/api/v1/operacoes-portabilidade', {
      method: 'POST',
      body: JSON.stringify(body),
      idempotencyKey,
    })
  },

  async listOperations(beneficiarioId: string, page = 0, size = 10) {
    const params = new URLSearchParams({
      beneficiarioId,
      page: String(page),
      size: String(size),
    })
    const raw = await httpRequest<OperationPage | OperationPage['items']>(
      `/api/v1/operacoes-portabilidade?${params.toString()}`,
    )
    // Tolerate legacy array payloads if an old API image is still running.
    if (Array.isArray(raw)) {
      return {
        items: raw,
        page,
        size,
        totalElements: raw.length,
        totalPages: raw.length === 0 ? 0 : 1,
        hasNext: false,
        hasPrevious: false,
      } satisfies OperationPage
    }
    return raw
  },

  getOperation(operationId: string) {
    return httpRequest<OperationStatus>(
      `/api/v1/operacoes-portabilidade/${encodeURIComponent(operationId)}`,
    )
  },

  getEvents(operationId: string) {
    return httpRequest<OperationEvent[]>(
      `/api/v1/operacoes-portabilidade/${encodeURIComponent(operationId)}/eventos`,
    )
  },
}

export type PortabilityApiService = typeof portabilityApiService

export function isTerminal(status: OperationStatus['status'] | undefined): boolean {
  return status === 'CONFIRMADA' || status === 'REJEITADA'
}
