import type { ReactNode } from 'react'
import type { AgendaItem, ReceivingAccount } from '../../../shared/api/types'
import { labelBusinessStatus } from '../../../shared/i18n/labels'
import { maskAccount } from '../../../shared/lib/mask'
import { formatMoney, sumMoney } from '../money'
import type { WizardStep } from '../wizardUrlState'

type Props = {
  step: WizardStep
  beneficiaryId: string
  agreementId: string
  authorizationId: string | null
  selectedItems: AgendaItem[]
  creditor: string
  account: ReceivingAccount
  operationId: string | null
  businessStatus?: string
}

const STEP_LABEL: Record<WizardStep, string> = {
  consentimento: 'Consentimento',
  agenda: 'Parcelas',
  destino: 'Destino',
  revisao: 'Revisão',
  acompanhamento: 'Resultado',
}

function SummaryRow({
  label,
  children,
}: {
  label: string
  children: ReactNode
}) {
  return (
    <div className="summary-row">
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  )
}

export function OperationSummary({
  step,
  beneficiaryId,
  agreementId,
  authorizationId,
  selectedItems,
  creditor,
  account,
  operationId,
  businessStatus,
}: Props) {
  const total = sumMoney(selectedItems.map((item) => item.valor))
  const hasAccount = Boolean(account.banco && account.agencia && account.conta)

  return (
    <aside className="card summary-card">
      <div className="card__body">
        <div className="summary-card__brand">
          <img
            src="/brand/logo.svg"
            alt=""
            width={88}
            height={28}
            draggable={false}
          />
          <h2>Resumo da operação</h2>
          <p className="summary-card__hint">
            Identidade, parcelas e destino — sem expor dados sensíveis.
          </p>
        </div>

        <section className="summary-section" aria-label="Progresso">
          <h3 className="summary-section__title">Progresso</h3>
          <dl className="summary-list">
            <SummaryRow label="Etapa">{STEP_LABEL[step]}</SummaryRow>
          </dl>
        </section>

        <hr className="summary-divider" />

        <section className="summary-section" aria-label="Identidade">
          <h3 className="summary-section__title">Identidade</h3>
          <dl className="summary-list">
            <SummaryRow label="Beneficiário">
              <code className="mono">{beneficiaryId}</code>
            </SummaryRow>
            <SummaryRow label="Convênio">
              <code className="mono">{agreementId}</code>
            </SummaryRow>
            <SummaryRow label="Autorização">
              {authorizationId ? (
                <code className="mono">{authorizationId}</code>
              ) : (
                <span className="muted">Pendente</span>
              )}
            </SummaryRow>
          </dl>
        </section>

        <hr className="summary-divider" />

        <section className="summary-section" aria-label="Seleção e destino">
          <h3 className="summary-section__title">Seleção e destino</h3>
          <dl className="summary-list">
            <SummaryRow label="Parcelas">
              {selectedItems.length > 0 ? (
                <>
                  <span className="summary-pill">{selectedItems.length}</span>
                  <span className="mono">
                    {total == null ? '—' : formatMoney(total)}
                  </span>
                </>
              ) : (
                <span className="muted">Nenhuma</span>
              )}
            </SummaryRow>
            <SummaryRow label="Destino">
              {creditor && hasAccount ? (
                <div className="summary-destination">
                  <span>{creditor}</span>
                  <code className="mono">
                    {account.banco}/{account.agencia}/{maskAccount(account.conta)}
                  </code>
                </div>
              ) : (
                <span className="muted">Não informado</span>
              )}
            </SummaryRow>
          </dl>
        </section>

        {operationId ? (
          <>
            <hr className="summary-divider" />
            <section className="summary-section" aria-label="Resultado">
              <h3 className="summary-section__title">Resultado</h3>
              <dl className="summary-list">
                <SummaryRow label="Operação">
                  <code className="mono">{operationId}</code>
                </SummaryRow>
                {businessStatus ? (
                  <SummaryRow label="Status">
                    {labelBusinessStatus(businessStatus)}
                  </SummaryRow>
                ) : null}
              </dl>
            </section>
          </>
        ) : null}
      </div>
    </aside>
  )
}
