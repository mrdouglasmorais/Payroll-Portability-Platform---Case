import type {
  AgendaItem,
  ApiError,
  AuthorizationRequest,
  OperationEvent,
  OperationStatus,
  PortabilityRequest,
} from './types'

const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''

async function parseError(response: Response): Promise<ApiError> {
  let message = `Falha na requisição (${response.status})`
  try {
    const body = (await response.json()) as { error?: string }
    if (body.error) {
      message = body.error
    }
  } catch {
    // keep default
  }
  const kind =
    response.status === 409
      ? 'conflict'
      : response.status === 503 || response.status === 502
        ? 'unavailable'
        : response.status >= 400 && response.status < 500
          ? 'validation'
          : 'unknown'
  return { status: response.status, message, kind }
}

async function request<T>(
  path: string,
  init?: RequestInit & { idempotencyKey?: string; correlationId?: string },
): Promise<T> {
  const headers = new Headers(init?.headers)
  if (!headers.has('Content-Type') && init?.body) {
    headers.set('Content-Type', 'application/json')
  }
  if (init?.idempotencyKey) {
    headers.set('Idempotency-Key', init.idempotencyKey)
  }
  headers.set(
    'X-Correlation-Id',
    init?.correlationId ?? crypto.randomUUID(),
  )

  const { idempotencyKey: _i, correlationId: _c, ...rest } = init ?? {}
  const response = await fetch(`${apiBase}${path}`, { ...rest, headers })
  if (!response.ok) {
    throw await parseError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export const api = {
  createAuthorization(body: AuthorizationRequest) {
    return request<{ autorizacaoId: string }>('/api/v1/autorizacoes', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  fetchAgenda(beneficiarioId: string) {
    return request<AgendaItem[]>(
      `/api/v1/beneficiarios/${encodeURIComponent(beneficiarioId)}/agenda`,
    )
  },

  createPortability(body: PortabilityRequest, idempotencyKey: string) {
    return request<{ operationId: string }>('/api/v1/operacoes-portabilidade', {
      method: 'POST',
      body: JSON.stringify(body),
      idempotencyKey,
    })
  },

  getOperation(operationId: string) {
    return request<OperationStatus>(
      `/api/v1/operacoes-portabilidade/${encodeURIComponent(operationId)}`,
    )
  },

  getEvents(operationId: string) {
    return request<OperationEvent[]>(
      `/api/v1/operacoes-portabilidade/${encodeURIComponent(operationId)}/eventos`,
    )
  },
}

export function isTerminal(status: OperationStatus['status'] | undefined): boolean {
  return status === 'CONFIRMADA' || status === 'REJEITADA'
}

export function isApiError(error: unknown): error is ApiError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'kind' in error &&
    'message' in error
  )
}
