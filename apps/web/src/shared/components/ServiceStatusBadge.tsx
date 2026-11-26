import { useQuery } from '@tanstack/react-query'
import { portabilityApiService } from '../api/portabilityApiService'
import { portabilityKeys } from '../api/queryKeys'
import './ServiceStatusBadge.css'

export type ServiceHealthState = 'healthy' | 'checking' | 'unavailable' | 'unknown'

function mapHealth(input: {
  isPending: boolean
  isError: boolean
  status?: string
}): ServiceHealthState {
  if (input.isPending) {
    return 'checking'
  }
  if (input.isError) {
    return 'unavailable'
  }
  if (input.status === 'UP') {
    return 'healthy'
  }
  if (input.status) {
    return 'unknown'
  }
  return 'unknown'
}

const LABELS: Record<ServiceHealthState, string> = {
  healthy: 'API disponível',
  checking: 'Verificando API',
  unavailable: 'API indisponível',
  unknown: 'API desconhecida',
}

export function ServiceStatusBadge() {
  const statusQuery = useQuery({
    queryKey: portabilityKeys.status(),
    queryFn: () => portabilityApiService.getStatus(),
    retry: 1,
    refetchInterval: (query) => (query.state.status === 'error' ? 5_000 : false),
  })

  const state = mapHealth({
    isPending: statusQuery.isPending,
    isError: statusQuery.isError,
    status: statusQuery.data?.status,
  })

  const detail = statusQuery.data
    ? `${statusQuery.data.service} · ${statusQuery.data.milestone}`
    : null
  const tooltip = detail ? `${LABELS[state]} — ${detail}` : LABELS[state]

  return (
    <p
      className={`service-status service-status--${state}`}
      role="status"
      aria-live="polite"
      aria-busy={state === 'checking'}
      aria-label={tooltip}
      title={tooltip}
    >
      <span className="status-dot" data-health={state} aria-hidden="true" />
      <span className="service-status__label">{LABELS[state]}</span>
      {state === 'checking' ? <span className="spinner" aria-hidden="true" /> : null}
    </p>
  )
}
