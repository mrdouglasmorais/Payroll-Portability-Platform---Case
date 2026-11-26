import type { ApiError } from './types'

const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''
const DEFAULT_TIMEOUT_MS = 20_000

export type RequestOptions = RequestInit & {
  idempotencyKey?: string
  correlationId?: string
  timeoutMs?: number
}

async function parseError(response: Response): Promise<ApiError> {
  let message = `Falha na requisição (${response.status})`
  let reason: string | undefined
  try {
    const body = (await response.json()) as {
      detail?: string
      error?: string
      title?: string
      reason?: string
    }
    if (body.detail) {
      message = body.detail
    } else if (body.error) {
      message = body.error
    } else if (body.title) {
      message = body.title
    }
    if (typeof body.reason === 'string' && body.reason.trim()) {
      reason = body.reason
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
  return { status: response.status, message, kind, reason }
}

export function isApiError(error: unknown): error is ApiError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'kind' in error &&
    'message' in error
  )
}

/**
 * Shared HTTP transport for the portability API. Prefer {@link portabilityApiService}
 * for domain calls instead of calling this directly from UI features.
 */
export async function httpRequest<T>(
  path: string,
  init?: RequestOptions,
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

  const {
    idempotencyKey: _i,
    correlationId: _c,
    timeoutMs = DEFAULT_TIMEOUT_MS,
    ...rest
  } = init ?? {}
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), timeoutMs)
  if (rest.signal) {
    rest.signal.addEventListener('abort', () => controller.abort(), { once: true })
  }

  try {
    const response = await fetch(`${apiBase}${path}`, {
      ...rest,
      headers,
      signal: controller.signal,
    })
    if (!response.ok) {
      throw await parseError(response)
    }
    if (response.status === 204) {
      return undefined as T
    }
    return (await response.json()) as T
  } catch (error) {
    if (isApiError(error)) {
      throw error
    }
    const aborted =
      (error instanceof DOMException && error.name === 'AbortError') ||
      (error instanceof Error && error.name === 'AbortError')
    const unavailable: ApiError = {
      status: 0,
      kind: 'unavailable',
      message: aborted
        ? 'A API demorou demais para responder. Confira se portabilidade-api está no ar.'
        : 'Não foi possível conectar à API. Use o console em http://localhost:5173 (Compose) ou confira o proxy do Vite.',
    }
    throw unavailable
  } finally {
    window.clearTimeout(timeout)
  }
}
