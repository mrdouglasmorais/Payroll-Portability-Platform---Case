import type { AgendaItem } from '../../../shared/api/types'
import {
  formatDiscountDate,
  labelInstallmentStatus,
} from '../../../shared/i18n/labels'
import { formatMoney, sumMoney } from '../money'

type Props = {
  authorizationId: string | null
  beneficiaryId: string
  agreementId: string
  agenda: AgendaItem[]
  selected: string[]
  loading: boolean
  inlineError: string | null
  onToggle: (parcelaId: string) => void
  onBack: () => void
  onContinue: () => void
}

function statusChipClass(status: string): string {
  return status === 'OPEN' ? 'status-chip status-chip--open' : 'status-chip status-chip--blocked'
}

export function AgendaStep({
  authorizationId,
  beneficiaryId,
  agreementId,
  agenda,
  selected,
  loading,
  inlineError,
  onToggle,
  onBack,
  onContinue,
}: Props) {
  const selectedItems = agenda.filter((item) => selected.includes(item.parcelaId))
  const total = sumMoney(selectedItems.map((item) => item.valor))
  const agreements = [
    ...new Set(selectedItems.map((item) => item.convenioId).filter(Boolean)),
  ]

  return (
    <section className="card step-enter" aria-busy={loading}>
      <div className="card__body">
        <div className="card__title-row">
          <span className="card__icon card__icon--accent">
            <img src="/brand/icon-money-id.svg" alt="" width={28} height={28} draggable={false} />
          </span>
          <h3 className="card__title">Parcelas elegíveis</h3>
        </div>
        <p className="card__desc">
          Autorização <code className="mono">{authorizationId}</code>. Selecione apenas
          parcelas abertas.
        </p>

        {inlineError ? (
          <p className="inline-alert inline-alert--error" role="alert">
            {inlineError}
          </p>
        ) : null}

        {loading ? (
          <p className="meta" role="status">
            <span className="spinner" aria-hidden="true" /> Carregando agenda…
          </p>
        ) : null}

        {!loading && agenda.length === 0 ? (
          <p className="empty-state" role="status">
            Nenhuma parcela retornada para <code className="mono">{beneficiaryId}</code>.
          </p>
        ) : null}

        {agenda.length > 0 ? (
          <>
            <div className="agenda-table-wrap">
              <table className="agenda-table">
                <thead>
                  <tr>
                    <th className="col-check" scope="col">
                      <span className="visually-hidden">Seleção</span>
                    </th>
                    <th scope="col">Identificador</th>
                    <th scope="col">Convênio</th>
                    <th scope="col">Data do desconto</th>
                    <th className="col-money" scope="col">
                      Valor
                    </th>
                    <th scope="col">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {agenda.map((item) => {
                    const selectable = item.status === 'OPEN'
                    const checked = selected.includes(item.parcelaId)
                    return (
                      <tr key={item.parcelaId} data-disabled={!selectable}>
                        <td className="col-check">
                          <input
                            type="checkbox"
                            checked={checked}
                            disabled={!selectable}
                            aria-label={`Selecionar ${item.parcelaId}`}
                            onChange={() => {
                              if (selectable) {
                                onToggle(item.parcelaId)
                              }
                            }}
                          />
                        </td>
                        <td className="col-id">{item.parcelaId}</td>
                        <td>{item.convenioId}</td>
                        <td>{formatDiscountDate(item.dataDesconto)}</td>
                        <td className="col-money">{formatMoney(item.valor)}</td>
                        <td>
                          <span className={statusChipClass(item.status)}>
                            {labelInstallmentStatus(item.status)}
                          </span>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            <div className="agenda-summary" aria-live="polite">
              <div>
                Selecionadas: <strong>{selectedItems.length}</strong>
              </div>
              <div>
                Valor total:{' '}
                <strong className="mono">
                  {total == null ? '—' : formatMoney(total)}
                </strong>
              </div>
              <div>
                Beneficiário: <code className="mono">{beneficiaryId}</code>
              </div>
              <div>
                Convênio(s):{' '}
                <code className="mono">
                  {agreements.length > 0 ? agreements.join(', ') : agreementId}
                </code>
              </div>
            </div>
          </>
        ) : null}

        <div className="actions">
          <button type="button" className="btn btn--secondary" onClick={onBack}>
            Voltar
          </button>
          <button type="button" className="btn" onClick={onContinue}>
            Continuar
          </button>
        </div>
      </div>
    </section>
  )
}
