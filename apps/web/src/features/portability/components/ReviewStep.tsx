import type { AgendaItem, ReceivingAccount } from '../../../shared/api/types'
import { maskAccount } from '../../../shared/lib/mask'
import { formatMoney, sumMoney } from '../money'

type Props = {
  beneficiaryId: string
  selectedItems: AgendaItem[]
  creditor: string
  account: ReceivingAccount
  idempotencyKey: string
  isPending: boolean
  inlineError: string | null
  onBack: () => void
  onSubmit: () => void
}

export function ReviewStep({
  beneficiaryId,
  selectedItems,
  creditor,
  account,
  idempotencyKey,
  isPending,
  inlineError,
  onBack,
  onSubmit,
}: Props) {
  const total = sumMoney(selectedItems.map((item) => item.valor))

  return (
    <section className="card step-enter" aria-busy={isPending}>
      <div className="card__body">
        <div className="card__title-row">
          <span className="card__icon card__icon--accent">
            <img src="/brand/icon-money-plus.svg" alt="" width={28} height={28} draggable={false} />
          </span>
          <h3 className="card__title">Revisão e envio</h3>
        </div>
        <p className="card__desc">Confira a intenção financeira antes de enviar.</p>

        {inlineError ? (
          <p className="inline-alert inline-alert--error" role="alert">
            {inlineError}
          </p>
        ) : null}

        <dl className="review-list">
          <div>
            <dt>Beneficiário</dt>
            <dd>
              <code className="mono">{beneficiaryId}</code>
            </dd>
          </div>
          <div>
            <dt>Parcelas</dt>
            <dd>{selectedItems.map((item) => item.parcelaId).join(', ') || '—'}</dd>
          </div>
          <div>
            <dt>Quantidade</dt>
            <dd>{selectedItems.length}</dd>
          </div>
          <div>
            <dt>Valor total</dt>
            <dd className="mono">{total == null ? '—' : formatMoney(total)}</dd>
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
        </dl>

        <details className="tech-details">
          <summary>Detalhes técnicos</summary>
          <pre>Idempotency-Key: {idempotencyKey}</pre>
        </details>

        <div className="actions">
          <button
            type="button"
            className="btn btn--secondary"
            onClick={onBack}
            disabled={isPending}
          >
            Voltar
          </button>
          <button
            type="button"
            className="btn"
            onClick={onSubmit}
            disabled={isPending}
            aria-busy={isPending}
          >
            {isPending ? <span className="spinner" aria-hidden="true" /> : null}
            {isPending ? 'Enviando…' : 'Enviar portabilidade'}
          </button>
        </div>
      </div>
    </section>
  )
}
