import type { UseQueryResult } from '@tanstack/react-query'
import type { OperationEvent, OperationStatus } from '../../../shared/api/types'
import {
  formatOccurredAt,
  labelApiMessage,
  labelBusinessStatus,
  labelErrorCode,
  labelEventType,
} from '../../../shared/i18n/labels'
import { isTerminal } from '../../../shared/api/portabilityApiService'

type Props = {
  operationId: string
  statusQuery: UseQueryResult<OperationStatus, unknown>
  eventsQuery: UseQueryResult<OperationEvent[], unknown>
  describeError: (error: unknown, fallback: string) => string
  onRestart: () => void
}

function labelProcessing(status: OperationStatus['processing']): string {
  if (!status) {
    return 'Aguardando processamento'
  }
  switch (status.status) {
    case 'PENDING':
      return 'Aguardando processamento'
    case 'RUNNING':
      return 'Processando'
    case 'RETRYING':
      return 'Nova tentativa programada'
    case 'SUCCEEDED':
      return 'Processamento concluído'
    case 'REQUIRES_REVIEW':
      return 'Revisão técnica necessária'
    default:
      return 'Estado técnico desconhecido'
  }
}

function formatAccount(view: NonNullable<OperationStatus['contaRecebimento']>): string {
  return `${view.banco}/${view.agencia}/${view.conta}`
}

function accountEffectCopy(status: OperationStatus): string {
  if (status.status === 'CONFIRMADA' || status.contaEfetivada) {
    return 'Efetivada nas parcelas desta operação (conta anterior substituída).'
  }
  if (status.status === 'REJEITADA') {
    return 'Não aplicada. Conta efetiva anterior das parcelas foi preservada.'
  }
  return 'Ainda não efetivada. Só muda após confirmação verificada da Central.'
}

export function TrackingStep({
  operationId,
  statusQuery,
  eventsQuery,
  describeError,
  onRestart,
}: Props) {
  const businessStatus = statusQuery.data?.status ?? 'PENDENTE'
  const terminal = isTerminal(businessStatus)
  const conta = statusQuery.data?.contaRecebimento

  return (
    <section className="card step-enter" aria-live="polite" aria-busy={!terminal}>
      <div className="card__body">
        <div className="card__title-row">
          <span className="card__icon">
            <img src="/brand/icon-line.svg" alt="" width={28} height={28} draggable={false} />
          </span>
          <h3 className="card__title">Resultado e auditoria</h3>
        </div>
        <p className="card__desc">
          Operação <code className="mono">{operationId}</code>
        </p>

        <div className="status-panel">
          <div className="status-row">
            <span className="meta">Status de negócio</span>
            <strong data-status={businessStatus}>
              {statusQuery.isPending && !statusQuery.data
                ? 'Carregando…'
                : labelBusinessStatus(businessStatus)}
            </strong>
            {!terminal ? (
              <span className="meta">
                <span className="spinner" aria-hidden="true" /> consultando API
              </span>
            ) : null}
          </div>
          <div className="status-row">
            <span className="meta">Status técnico</span>
            <span>{labelProcessing(statusQuery.data?.processing)}</span>
          </div>
        </div>

        {conta ? (
          <div className="account-effect" data-testid="account-effect">
            <h4 className="card__title" style={{ marginTop: 0, fontSize: '1rem' }}>
              Conta de recebimento
            </h4>
            <dl className="account-effect__grid">
              <div>
                <dt>Destino informado</dt>
                <dd className="mono">{formatAccount(conta)}</dd>
              </div>
              <div>
                <dt>Efetivação nas parcelas</dt>
                <dd data-effect={businessStatus}>
                  {statusQuery.data ? accountEffectCopy(statusQuery.data) : '—'}
                </dd>
              </div>
              {statusQuery.data?.status === 'CONFIRMADA' ? (
                <div>
                  <dt>Conta efetiva após confirmação</dt>
                  <dd className="mono">{formatAccount(conta)}</dd>
                </div>
              ) : null}
            </dl>
          </div>
        ) : null}

        {statusQuery.data?.status === 'REJEITADA' ? (
          <p className="inline-alert inline-alert--error" role="status">
            Rejeição de negócio:{' '}
            {statusQuery.data.motivoRejeicao
              ? labelApiMessage(statusQuery.data.motivoRejeicao)
              : 'Motivo não informado'}
          </p>
        ) : null}

        {statusQuery.data?.processing?.requiresAction ? (
          <p className="inline-alert" role="status">
            Revisão técnica necessária (
            {labelErrorCode(statusQuery.data.processing.errorCode)}, tentativas{' '}
            {statusQuery.data.processing.attemptCount}). Isso não é uma rejeição financeira.
          </p>
        ) : null}

        {statusQuery.data?.status === 'CONFIRMADA' ? (
          <p className="inline-alert inline-alert--success" role="status">
            Confirmada pela Central. Conta efetivada nas parcelas selecionadas.
          </p>
        ) : null}

        {statusQuery.isError ? (
          <p className="inline-alert inline-alert--error" role="alert">
            Falha de integração na consulta:{' '}
            {describeError(statusQuery.error, 'erro desconhecido')}
          </p>
        ) : null}

        <h4 className="card__title" style={{ marginTop: 8 }}>
          Eventos
        </h4>

        {eventsQuery.isError ? (
          <div className="inline-alert inline-alert--error" role="alert">
            <p style={{ margin: 0 }}>
              Falha ao carregar auditoria:{' '}
              {describeError(eventsQuery.error, 'erro desconhecido')}
            </p>
            <button
              type="button"
              className="btn btn--secondary"
              style={{ marginTop: 12 }}
              onClick={() => void eventsQuery.refetch()}
            >
              Tentar novamente
            </button>
          </div>
        ) : null}

        {eventsQuery.isPending && !eventsQuery.data ? (
          <p className="meta" role="status">
            <span className="spinner" aria-hidden="true" /> Carregando auditoria…
          </p>
        ) : null}

        <ol className="timeline">
          {(eventsQuery.data ?? []).map((event) => (
            <li key={`${event.sequence}-${event.eventType}`}>
              <div className="timeline__rail">
                <span className="timeline__dot" aria-hidden="true" />
              </div>
              <div>
                <p className="timeline__time">{formatOccurredAt(event.occurredAt)}</p>
                <p className="timeline__title">{labelEventType(event.eventType)}</p>
                <p className="timeline__desc">{labelApiMessage(event.description)}</p>
              </div>
            </li>
          ))}
        </ol>

        <div className="actions">
          <button type="button" className="btn" onClick={onRestart}>
            Nova operação
          </button>
        </div>
      </div>
    </section>
  )
}
