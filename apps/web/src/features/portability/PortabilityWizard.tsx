import { useMutation, useQuery } from '@tanstack/react-query'
import { useId, useMemo, useState } from 'react'
import { api, isApiError, isTerminal } from '../../shared/api/client'
import type { AgendaItem, ReceivingAccount } from '../../shared/api/types'
import { createIdempotencyKey } from '../../shared/lib/idempotency'
import { maskAccount } from '../../shared/lib/mask'

type Step = 'consent' | 'agenda' | 'destination' | 'review' | 'tracking'

const STEPS: Step[] = ['consent', 'agenda', 'destination', 'review', 'tracking']

function formatMoney(value: number | string): string {
  const amount = typeof value === 'number' ? value : Number(value)
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(Number.isFinite(amount) ? amount : 0)
}

export function PortabilityWizard() {
  const formId = useId()
  const [step, setStep] = useState<Step>('consent')
  const [beneficiaryId, setBeneficiaryId] = useState('ben-001')
  const [agreementId, setAgreementId] = useState('conv-1')
  const [authorizationId, setAuthorizationId] = useState<string | null>(null)
  const [agenda, setAgenda] = useState<AgendaItem[]>([])
  const [selected, setSelected] = useState<string[]>([])
  const [creditor, setCreditor] = useState('')
  const [account, setAccount] = useState<ReceivingAccount>({
    banco: '',
    agencia: '',
    conta: '',
  })
  const [idempotencyKey, setIdempotencyKey] = useState(() => createIdempotencyKey())
  const [operationId, setOperationId] = useState<string | null>(null)
  const [banner, setBanner] = useState<string | null>(null)

  const stepIndex = STEPS.indexOf(step)

  const authorizeMutation = useMutation({
    mutationFn: () =>
      api.createAuthorization({
        beneficiarioId: beneficiaryId.trim(),
        convenioId: agreementId.trim(),
      }),
    onSuccess: async (data) => {
      setAuthorizationId(data.autorizacaoId)
      setBanner(null)
      const items = await api.fetchAgenda(beneficiaryId.trim())
      setAgenda(items)
      setSelected([])
      setStep('agenda')
    },
    onError: (error) => {
      setBanner(
        isApiError(error)
          ? error.kind === 'unavailable'
            ? `Integration issue: ${error.message}`
            : error.message
          : 'Authorization failed',
      )
    },
  })

  const submitMutation = useMutation({
    mutationFn: () =>
      api.createPortability(
        {
          beneficiarioId: beneficiaryId.trim(),
          parcelas: [...selected].sort(),
          credorCessionario: creditor.trim(),
          contaRecebimento: account,
        },
        idempotencyKey,
      ),
    onSuccess: (data) => {
      setOperationId(data.operationId)
      setBanner(null)
      setStep('tracking')
    },
    onError: (error) => {
      if (isApiError(error) && error.kind === 'conflict') {
        setBanner(`Conflict: ${error.message}. Generate a new key only if the payload changed.`)
        return
      }
      if (isApiError(error) && error.kind === 'unavailable') {
        setBanner(
          `Integration issue (not a business rejection): ${error.message}. Retry keeps the same idempotency key.`,
        )
        return
      }
      setBanner(isApiError(error) ? error.message : 'Submit failed')
    },
  })

  const statusQuery = useQuery({
    queryKey: ['operation', operationId],
    queryFn: () => api.getOperation(operationId!),
    enabled: step === 'tracking' && !!operationId,
    refetchInterval: (query) =>
      isTerminal(query.state.data?.status) ? false : 1500,
  })

  const eventsQuery = useQuery({
    queryKey: ['operation-events', operationId],
    queryFn: () => api.getEvents(operationId!),
    enabled: step === 'tracking' && !!operationId,
    refetchInterval: () =>
      isTerminal(statusQuery.data?.status) ? false : 1500,
  })

  const selectedItems = useMemo(
    () => agenda.filter((item) => selected.includes(item.parcelaId)),
    [agenda, selected],
  )

  function toggleParcela(id: string) {
    setSelected((current) =>
      current.includes(id) ? current.filter((x) => x !== id) : [...current, id],
    )
  }

  function goDestination() {
    if (selected.length === 0) {
      setBanner('Select at least one installment')
      return
    }
    setBanner(null)
    setStep('destination')
  }

  function goReview() {
    if (!creditor.trim() || !account.banco || !account.agencia || !account.conta) {
      setBanner('Destination creditor and account are required')
      return
    }
    setBanner(null)
    setStep('review')
  }

  function startOver() {
    setStep('consent')
    setAuthorizationId(null)
    setAgenda([])
    setSelected([])
    setCreditor('')
    setAccount({ banco: '', agencia: '', conta: '' })
    setIdempotencyKey(createIdempotencyKey())
    setOperationId(null)
    setBanner(null)
  }

  return (
    <section className="wizard" aria-labelledby={`${formId}-title`}>
      <div className="wizard-head">
        <h2 id={`${formId}-title`}>Portability flow</h2>
        <ol className="steps" aria-label="Workflow steps">
          {STEPS.map((name, index) => (
            <li
              key={name}
              className={
                index === stepIndex ? 'current' : index < stepIndex ? 'done' : undefined
              }
              aria-current={index === stepIndex ? 'step' : undefined}
            >
              {name}
            </li>
          ))}
        </ol>
      </div>

      {banner && (
        <p className="banner" role="alert">
          {banner}
        </p>
      )}

      {step === 'consent' && (
        <form
          className="panel"
          onSubmit={(event) => {
            event.preventDefault()
            authorizeMutation.mutate()
          }}
        >
          <h3>1. Opt-in authorization</h3>
          <p className="hint">Registers consent with the Central via the platform API.</p>
          <label>
            Beneficiary id
            <input
              name="beneficiarioId"
              value={beneficiaryId}
              onChange={(e) => setBeneficiaryId(e.target.value)}
              required
              autoComplete="off"
            />
          </label>
          <label>
            Agreement id
            <input
              name="convenioId"
              value={agreementId}
              onChange={(e) => setAgreementId(e.target.value)}
              required
              autoComplete="off"
            />
          </label>
          <button type="submit" disabled={authorizeMutation.isPending}>
            {authorizeMutation.isPending ? 'Authorizing…' : 'Authorize and load agenda'}
          </button>
        </form>
      )}

      {step === 'agenda' && (
        <div className="panel">
          <h3>2. Agenda</h3>
          <p className="hint">
            Authorization <code>{authorizationId}</code>. Select open installments.
          </p>
          {agenda.length === 0 ? (
            <p>No open installments returned.</p>
          ) : (
            <ul className="agenda-list">
              {agenda.map((item) => {
                const checked = selected.includes(item.parcelaId)
                return (
                  <li key={item.parcelaId}>
                    <label className="check-row">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggleParcela(item.parcelaId)}
                      />
                      <span>
                        <strong>{item.parcelaId}</strong>
                        <span className="meta">
                          {formatMoney(item.valor)} · {item.dataDesconto} · {item.status}
                        </span>
                      </span>
                    </label>
                  </li>
                )
              })}
            </ul>
          )}
          <div className="actions">
            <button type="button" className="ghost" onClick={() => setStep('consent')}>
              Back
            </button>
            <button type="button" onClick={goDestination}>
              Continue
            </button>
          </div>
        </div>
      )}

      {step === 'destination' && (
        <form
          className="panel"
          onSubmit={(event) => {
            event.preventDefault()
            goReview()
          }}
        >
          <h3>3. Destination</h3>
          <label>
            Assignee creditor
            <input
              value={creditor}
              onChange={(e) => setCreditor(e.target.value)}
              required
              autoComplete="off"
            />
          </label>
          <div className="grid-3">
            <label>
              Bank
              <input
                value={account.banco}
                onChange={(e) => setAccount((a) => ({ ...a, banco: e.target.value }))}
                required
                autoComplete="off"
              />
            </label>
            <label>
              Agency
              <input
                value={account.agencia}
                onChange={(e) => setAccount((a) => ({ ...a, agencia: e.target.value }))}
                required
                autoComplete="off"
              />
            </label>
            <label>
              Account
              <input
                value={account.conta}
                onChange={(e) => setAccount((a) => ({ ...a, conta: e.target.value }))}
                required
                autoComplete="off"
              />
            </label>
          </div>
          <div className="actions">
            <button type="button" className="ghost" onClick={() => setStep('agenda')}>
              Back
            </button>
            <button type="submit">Review</button>
          </div>
        </form>
      )}

      {step === 'review' && (
        <div className="panel">
          <h3>4. Review and submit</h3>
          <dl className="review">
            <div>
              <dt>Beneficiary</dt>
              <dd>{beneficiaryId}</dd>
            </div>
            <div>
              <dt>Installments</dt>
              <dd>{selectedItems.map((i) => i.parcelaId).join(', ')}</dd>
            </div>
            <div>
              <dt>Creditor</dt>
              <dd>{creditor}</dd>
            </div>
            <div>
              <dt>Account</dt>
              <dd>
                {account.banco}/{account.agencia}/{maskAccount(account.conta)}
              </dd>
            </div>
            <div>
              <dt>Idempotency key</dt>
              <dd>
                <code>{idempotencyKey}</code>
              </dd>
            </div>
          </dl>
          <p className="hint">
            Repeated submit reuses the same key to avoid duplicate operations.
          </p>
          <div className="actions">
            <button type="button" className="ghost" onClick={() => setStep('destination')}>
              Back
            </button>
            <button
              type="button"
              onClick={() => submitMutation.mutate()}
              disabled={submitMutation.isPending}
            >
              {submitMutation.isPending ? 'Submitting…' : 'Submit portability'}
            </button>
          </div>
        </div>
      )}

      {step === 'tracking' && operationId && (
        <div className="panel" aria-live="polite">
          <h3>5. Operation timeline</h3>
          <p>
            Operation <code>{operationId}</code>
          </p>
          <p className="status-line">
            Status:{' '}
            <strong data-status={statusQuery.data?.status ?? 'PENDENTE'}>
              {statusQuery.isPending && !statusQuery.data
                ? 'Loading…'
                : (statusQuery.data?.status ?? 'PENDENTE')}
            </strong>
            {!isTerminal(statusQuery.data?.status) && (
              <span className="polling"> · polling local API</span>
            )}
          </p>
          {statusQuery.data?.status === 'REJEITADA' && (
            <p className="error" role="status">
              Business rejection: {statusQuery.data.motivoRejeicao ?? 'No reason provided'}
            </p>
          )}
          {statusQuery.data?.processing?.requiresAction && (
            <p className="error" role="status">
              Technical review required ({statusQuery.data.processing.errorCode ?? 'INTEGRATION_ERROR'}
              , attempts {statusQuery.data.processing.attemptCount}). Business status remains
              pending — not a rejection.
            </p>
          )}
          {statusQuery.data?.status === 'CONFIRMADA' && (
            <p className="ok" role="status">
              Confirmed by Central. Receiving account effects applied only after verification.
            </p>
          )}
          {statusQuery.isError && (
            <p className="error" role="alert">
              Integration issue while polling:{' '}
              {isApiError(statusQuery.error) ? statusQuery.error.message : 'unknown error'}
            </p>
          )}
          <h4>Events</h4>
          <ol className="timeline">
            {(eventsQuery.data ?? []).map((event) => (
              <li key={`${event.sequence}-${event.eventType}`}>
                <span className="seq">{event.sequence}</span>
                <span>
                  <strong>{event.eventType}</strong>
                  <span className="meta">
                    {event.description} · {event.occurredAt}
                  </span>
                </span>
              </li>
            ))}
          </ol>
          <div className="actions">
            <button type="button" onClick={startOver}>
              New operation
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
