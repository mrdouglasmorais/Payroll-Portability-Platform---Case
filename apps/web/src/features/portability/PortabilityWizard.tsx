import { useMutation, useQuery } from '@tanstack/react-query'
import { useId, useMemo, useState } from 'react'
import { api, isApiError, isTerminal } from '../../shared/api/client'
import type { AgendaItem, ReceivingAccount } from '../../shared/api/types'
import {
  formatDiscountDate,
  formatOccurredAt,
  labelApiMessage,
  labelBusinessStatus,
  labelErrorCode,
  labelEventType,
  labelInstallmentStatus,
} from '../../shared/i18n/labels'
import { createIdempotencyKey } from '../../shared/lib/idempotency'
import { maskAccount } from '../../shared/lib/mask'

type Step = 'consentimento' | 'agenda' | 'destino' | 'revisao' | 'acompanhamento'

const STEPS: { id: Step; label: string }[] = [
  { id: 'consentimento', label: 'Consentimento' },
  { id: 'agenda', label: 'Agenda' },
  { id: 'destino', label: 'Destino' },
  { id: 'revisao', label: 'Revisão' },
  { id: 'acompanhamento', label: 'Acompanhamento' },
]

function formatMoney(value: number | string): string {
  const amount = typeof value === 'number' ? value : Number(value)
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(Number.isFinite(amount) ? amount : 0)
}

export function PortabilityWizard() {
  const formId = useId()
  const [step, setStep] = useState<Step>('consentimento')
  // ben-ui mantém parcelas OPEN dedicadas ao console (isoladas da contenção k6/E2E em ben-001).
  const [beneficiaryId, setBeneficiaryId] = useState('ben-ui')
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

  const stepIndex = STEPS.findIndex((item) => item.id === step)

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
            ? `Falha de integração: ${labelApiMessage(error.message)}`
            : labelApiMessage(error.message)
          : 'Falha na autorização',
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
      setStep('acompanhamento')
    },
    onError: (error) => {
      if (isApiError(error) && error.kind === 'conflict') {
        setBanner(
          `Conflito: ${labelApiMessage(error.message)}. Gere uma nova chave somente se o payload tiver mudado.`,
        )
        return
      }
      if (isApiError(error) && error.kind === 'unavailable') {
        setBanner(
          `Falha de integração (não é rejeição de negócio): ${labelApiMessage(error.message)}. A nova tentativa mantém a mesma chave de idempotência.`,
        )
        return
      }
      setBanner(
        isApiError(error) ? labelApiMessage(error.message) : 'Falha no envio',
      )
    },
  })

  const statusQuery = useQuery({
    queryKey: ['operation', operationId],
    queryFn: () => api.getOperation(operationId!),
    enabled: step === 'acompanhamento' && !!operationId,
    refetchInterval: (query) =>
      isTerminal(query.state.data?.status) ? false : 1500,
  })

  const eventsQuery = useQuery({
    queryKey: ['operation-events', operationId],
    queryFn: () => api.getEvents(operationId!),
    enabled: step === 'acompanhamento' && !!operationId,
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
    if (agenda.length === 0) {
      setBanner(
        'Não há parcelas abertas para este beneficiário. Tente ben-ui / conv-1 ou reabra as parcelas de demonstração na Central.',
      )
      return
    }
    if (selected.length === 0) {
      setBanner('Selecione ao menos uma parcela')
      return
    }
    setBanner(null)
    setStep('destino')
  }

  function goReview() {
    if (!creditor.trim() || !account.banco || !account.agencia || !account.conta) {
      setBanner('Informe o credor cessionário e a conta de recebimento')
      return
    }
    setBanner(null)
    setStep('revisao')
  }

  function startOver() {
    setStep('consentimento')
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
        <h2 id={`${formId}-title`}>Fluxo de portabilidade</h2>
        <ol className="steps" aria-label="Etapas do fluxo">
          {STEPS.map((item, index) => (
            <li
              key={item.id}
              className={
                index === stepIndex ? 'current' : index < stepIndex ? 'done' : undefined
              }
              aria-current={index === stepIndex ? 'step' : undefined}
            >
              {item.label}
            </li>
          ))}
        </ol>
      </div>

      {banner && (
        <p className="banner" role="alert">
          {banner}
        </p>
      )}

      {step === 'consentimento' && (
        <form
          className="panel"
          onSubmit={(event) => {
            event.preventDefault()
            authorizeMutation.mutate()
          }}
        >
          <h3>1. Autorização (opt-in)</h3>
          <p className="hint">
            Registra o consentimento na Central por meio da API da plataforma.
          </p>
          <label>
            ID do beneficiário
            <input
              name="beneficiarioId"
              value={beneficiaryId}
              onChange={(e) => setBeneficiaryId(e.target.value)}
              required
              autoComplete="off"
            />
          </label>
          <label>
            ID do convênio
            <input
              name="convenioId"
              value={agreementId}
              onChange={(e) => setAgreementId(e.target.value)}
              required
              autoComplete="off"
            />
          </label>
          <button type="submit" disabled={authorizeMutation.isPending}>
            {authorizeMutation.isPending
              ? 'Autorizando…'
              : 'Autorizar e carregar agenda'}
          </button>
        </form>
      )}

      {step === 'agenda' && (
        <div className="panel">
          <h3>2. Agenda</h3>
          <p className="hint">
            Autorização <code>{authorizationId}</code>. Selecione parcelas abertas.
          </p>
          {agenda.length === 0 ? (
            <p role="status">
              A Central não retornou parcelas abertas para <code>{beneficiaryId}</code>.
              Beneficiário de demonstração com parcelas abertas reservadas:{' '}
              <code>ben-ui</code> / <code>conv-1</code>.
            </p>
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
                          {formatMoney(item.valor)} · {formatDiscountDate(item.dataDesconto)} ·{' '}
                          {labelInstallmentStatus(item.status)}
                        </span>
                      </span>
                    </label>
                  </li>
                )
              })}
            </ul>
          )}
          <div className="actions">
            <button type="button" className="ghost" onClick={() => setStep('consentimento')}>
              Voltar
            </button>
            <button type="button" onClick={goDestination}>
              Continuar
            </button>
          </div>
        </div>
      )}

      {step === 'destino' && (
        <form
          className="panel"
          onSubmit={(event) => {
            event.preventDefault()
            goReview()
          }}
        >
          <h3>3. Destino</h3>
          <label>
            Credor cessionário
            <input
              value={creditor}
              onChange={(e) => setCreditor(e.target.value)}
              required
              autoComplete="off"
            />
          </label>
          <div className="grid-3">
            <label>
              Banco
              <input
                value={account.banco}
                onChange={(e) => setAccount((a) => ({ ...a, banco: e.target.value }))}
                required
                autoComplete="off"
              />
            </label>
            <label>
              Agência
              <input
                value={account.agencia}
                onChange={(e) => setAccount((a) => ({ ...a, agencia: e.target.value }))}
                required
                autoComplete="off"
              />
            </label>
            <label>
              Conta
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
              Voltar
            </button>
            <button type="submit">Revisar</button>
          </div>
        </form>
      )}

      {step === 'revisao' && (
        <div className="panel">
          <h3>4. Revisão e envio</h3>
          <dl className="review">
            <div>
              <dt>Beneficiário</dt>
              <dd>{beneficiaryId}</dd>
            </div>
            <div>
              <dt>Parcelas</dt>
              <dd>{selectedItems.map((i) => i.parcelaId).join(', ')}</dd>
            </div>
            <div>
              <dt>Credor</dt>
              <dd>{creditor}</dd>
            </div>
            <div>
              <dt>Conta</dt>
              <dd>
                {account.banco}/{account.agencia}/{maskAccount(account.conta)}
              </dd>
            </div>
            <div>
              <dt>Chave de idempotência</dt>
              <dd>
                <code>{idempotencyKey}</code>
              </dd>
            </div>
          </dl>
          <p className="hint">
            Reenvios reutilizam a mesma chave para evitar operações duplicadas.
          </p>
          <div className="actions">
            <button type="button" className="ghost" onClick={() => setStep('destino')}>
              Voltar
            </button>
            <button
              type="button"
              onClick={() => submitMutation.mutate()}
              disabled={submitMutation.isPending}
            >
              {submitMutation.isPending ? 'Enviando…' : 'Enviar portabilidade'}
            </button>
          </div>
        </div>
      )}

      {step === 'acompanhamento' && operationId && (
        <div className="panel" aria-live="polite">
          <h3>5. Linha do tempo da operação</h3>
          <p>
            Operação <code>{operationId}</code>
          </p>
          <p className="status-line">
            Status:{' '}
            <strong data-status={statusQuery.data?.status ?? 'PENDENTE'}>
              {statusQuery.isPending && !statusQuery.data
                ? 'Carregando…'
                : labelBusinessStatus(statusQuery.data?.status)}
            </strong>
            {!isTerminal(statusQuery.data?.status) && (
              <span className="polling"> · consultando API local</span>
            )}
          </p>
          {statusQuery.data?.status === 'REJEITADA' && (
            <p className="error" role="status">
              Rejeição de negócio:{' '}
              {statusQuery.data.motivoRejeicao
                ? labelApiMessage(statusQuery.data.motivoRejeicao)
                : 'Motivo não informado'}
            </p>
          )}
          {statusQuery.data?.processing?.requiresAction && (
            <p className="error" role="status">
              Revisão técnica necessária (
              {labelErrorCode(statusQuery.data.processing.errorCode)}, tentativas{' '}
              {statusQuery.data.processing.attemptCount}). O status de negócio permanece
              pendente — isso não é uma rejeição.
            </p>
          )}
          {statusQuery.data?.status === 'CONFIRMADA' && (
            <p className="ok" role="status">
              Confirmada pela Central. A conta de recebimento só é efetivada após a
              verificação.
            </p>
          )}
          {statusQuery.isError && (
            <p className="error" role="alert">
              Falha de integração na consulta:{' '}
              {isApiError(statusQuery.error)
                ? labelApiMessage(statusQuery.error.message)
                : 'erro desconhecido'}
            </p>
          )}
          <h4>Eventos</h4>
          <ol className="timeline">
            {(eventsQuery.data ?? []).map((event) => (
              <li key={`${event.sequence}-${event.eventType}`}>
                <span className="seq">{event.sequence}</span>
                <span>
                  <strong>{labelEventType(event.eventType)}</strong>
                  <span className="meta">
                    {labelApiMessage(event.description)} ·{' '}
                    {formatOccurredAt(event.occurredAt)}
                  </span>
                </span>
              </li>
            ))}
          </ol>
          <div className="actions">
            <button type="button" onClick={startOver}>
              Nova operação
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
