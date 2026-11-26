import { useEffect, useRef, useState } from 'react'
import type { UseQueryResult } from '@tanstack/react-query'
import type { OperationPage } from '../../../shared/api/types'
import {
  formatOccurredAt,
  labelBusinessStatus,
} from '../../../shared/i18n/labels'

type Props = {
  beneficiaryId: string
  currentOperationId: string | null
  historyQuery: UseQueryResult<OperationPage, unknown>
  describeError: (error: unknown, fallback: string) => string
  onOpenOperation: (operationId: string) => void
  onPreviousPage: () => void
  onNextPage: () => void
}

function formatAccount(item: OperationPage['items'][number]): string {
  const a = item.contaRecebimento
  return `${a.banco}/${a.agencia}/${a.conta}`
}

function PaginationBar({
  currentPage,
  totalPages,
  hasPrevious,
  hasNext,
  isFetching,
  onPrevious,
  onNext,
}: {
  currentPage: number
  totalPages: number
  hasPrevious: boolean
  hasNext: boolean
  isFetching: boolean
  onPrevious: () => void
  onNext: () => void
}) {
  return (
    <nav className="history-pagination" aria-label="Paginação do histórico">
      <button
        type="button"
        className="btn btn--secondary"
        onClick={onPrevious}
        disabled={!hasPrevious || isFetching}
      >
        Anterior
      </button>
      <p className="history-pagination__status" aria-live="polite">
        <span className="history-pagination__badge">
          Página {currentPage + 1} de {totalPages}
        </span>
      </p>
      <button
        type="button"
        className="btn btn--secondary"
        onClick={onNext}
        disabled={!hasNext || isFetching}
      >
        Próxima
      </button>
    </nav>
  )
}

export function HistoryPanel({
  beneficiaryId,
  currentOperationId,
  historyQuery,
  describeError,
  onOpenOperation,
  onPreviousPage,
  onNextPage,
}: Props) {
  const panelRef = useRef<HTMLElement>(null)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const [flash, setFlash] = useState(false)
  const [cuePage, setCuePage] = useState<number | null>(null)
  const [listKey, setListKey] = useState(0)
  const page = historyQuery.data
  const items = page?.items ?? []
  const trimmed = beneficiaryId.trim()

  const totalElements = page?.totalElements ?? 0
  const totalPages = page?.totalPages ?? 0
  const currentPage = page?.page ?? 0
  const hasPrevious = page?.hasPrevious ?? false
  const hasNext = page?.hasNext ?? false

  useEffect(() => {
    if (!flash) {
      return
    }
    const timer = window.setTimeout(() => {
      setFlash(false)
      setCuePage(null)
    }, 1100)
    return () => window.clearTimeout(timer)
  }, [flash])

  if (!trimmed) {
    return null
  }

  function bringHistoryIntoView(targetPage: number) {
    panelRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    headingRef.current?.focus({ preventScroll: true })
    setCuePage(targetPage)
    setFlash(true)
    setListKey((value) => value + 1)
  }

  function handlePrevious() {
    const target = Math.max(0, currentPage - 1)
    onPreviousPage()
    bringHistoryIntoView(target)
  }

  function handleNext() {
    const target = currentPage + 1
    onNextPage()
    bringHistoryIntoView(target)
  }

  return (
    <section
      ref={panelRef}
      className={flash ? 'card history-panel history-panel--flash' : 'card history-panel'}
      aria-label="Histórico de operações"
    >
      <div className="card__body">
        <div className="history-panel__header">
          <div className="history-panel__title-row">
            <h3 ref={headingRef} className="history-panel__title" tabIndex={-1}>
              Histórico do beneficiário
            </h3>
            {totalPages > 1 ? (
              <span className="history-page-chip">
                Página {currentPage + 1}/{totalPages}
              </span>
            ) : null}
          </div>
          <p className="meta">
            Operações de <code className="mono">{trimmed}</code>
            {totalElements > 0 ? ` — ${totalElements} no total` : null}
          </p>
          {flash && cuePage != null ? (
            <p className="history-panel__cue" role="status">
              Exibindo página {cuePage + 1}
              {totalPages > 0 ? ` de ${totalPages}` : ''}
            </p>
          ) : null}
        </div>

        {totalPages > 1 ? (
          <PaginationBar
            currentPage={currentPage}
            totalPages={totalPages}
            hasPrevious={hasPrevious}
            hasNext={hasNext}
            isFetching={historyQuery.isFetching}
            onPrevious={handlePrevious}
            onNext={handleNext}
          />
        ) : null}

        {historyQuery.isPending && !historyQuery.data ? (
          <p className="meta" role="status">
            <span className="spinner" aria-hidden="true" /> Carregando histórico…
          </p>
        ) : null}

        {historyQuery.isError ? (
          <div className="inline-alert inline-alert--error" role="alert">
            <p style={{ margin: 0 }}>
              Falha ao listar operações:{' '}
              {describeError(historyQuery.error, 'erro desconhecido')}
            </p>
            <button
              type="button"
              className="btn btn--secondary"
              style={{ marginTop: 12 }}
              onClick={() => void historyQuery.refetch()}
            >
              Tentar novamente
            </button>
          </div>
        ) : null}

        {!historyQuery.isPending && !historyQuery.isError && totalElements === 0 ? (
          <p className="meta">Nenhuma operação registrada para este beneficiário.</p>
        ) : null}

        {items.length > 0 ? (
          <ul key={listKey} className="history-list history-list--enter">
            {items.map((item) => {
              const active = item.operationId === currentOperationId
              return (
                <li key={item.operationId}>
                  <button
                    type="button"
                    className={
                      active
                        ? 'history-list__item history-list__item--active'
                        : 'history-list__item'
                    }
                    onClick={() => onOpenOperation(item.operationId)}
                    aria-current={active ? 'true' : undefined}
                  >
                    <span className="history-list__status" data-status={item.status}>
                      {labelBusinessStatus(item.status)}
                    </span>
                    <span className="history-list__meta">
                      <span className="mono">{item.operationId.slice(0, 8)}…</span>
                      <span>{formatOccurredAt(item.createdAt)}</span>
                    </span>
                    <span className="history-list__account mono">{formatAccount(item)}</span>
                    <span className="history-list__effect">
                      {item.contaEfetivada
                        ? 'Conta efetivada'
                        : item.status === 'REJEITADA'
                          ? 'Conta não aplicada'
                          : 'Aguardando efetivação'}
                    </span>
                  </button>
                </li>
              )
            })}
          </ul>
        ) : null}

        {totalPages > 1 ? (
          <div className="history-pagination--footer">
            <PaginationBar
              currentPage={currentPage}
              totalPages={totalPages}
              hasPrevious={hasPrevious}
              hasNext={hasNext}
              isFetching={historyQuery.isFetching}
              onPrevious={handlePrevious}
              onNext={handleNext}
            />
          </div>
        ) : null}
      </div>
    </section>
  )
}
