import { useState } from 'react'
import { IconEdit } from '../../../shared/components/Icons'
import { DEMO } from '../usePortabilityFlow'
import { validateIdentity } from '../validation'

type Props = {
  beneficiaryId: string
  agreementId: string
  showAdvancedIds: boolean
  isPending: boolean
  inlineError: string | null
  onBeneficiaryChange: (value: string) => void
  onAgreementChange: (value: string) => void
  onToggleAdvanced: () => void
  onApplyDemo: () => void
  onSubmit: () => void
}

export function ConsentStep({
  beneficiaryId,
  agreementId,
  showAdvancedIds,
  isPending,
  inlineError,
  onBeneficiaryChange,
  onAgreementChange,
  onToggleAdvanced,
  onApplyDemo,
  onSubmit,
}: Props) {
  const [errors, setErrors] = useState<{ beneficiaryId?: string; agreementId?: string }>(
    {},
  )
  const isDemo =
    beneficiaryId.trim() === DEMO.beneficiaryId &&
    agreementId.trim() === DEMO.agreementId

  return (
    <form
      className="card step-enter"
      aria-busy={isPending}
      onSubmit={(event) => {
        event.preventDefault()
        const next = validateIdentity(beneficiaryId, agreementId)
        setErrors(next)
        if (next.beneficiaryId || next.agreementId) {
          return
        }
        onSubmit()
      }}
    >
      <div className="card__body">
        <div className="card__title-row">
          <span className="card__icon card__icon--accent">
            <img src="/brand/icon-reload-money.svg" alt="" width={28} height={28} draggable={false} />
          </span>
          <h3 className="card__title">Autorização do beneficiário</h3>
        </div>
        <p className="card__desc">
          Registre o consentimento para consultar os descontos disponíveis.
        </p>

        {inlineError ? (
          <p className="inline-alert inline-alert--error" role="alert">
            {inlineError}
          </p>
        ) : null}

        <div className="demo-panel">
          <p className="demo-panel__title">Ambiente de demonstração</p>
          <dl className="demo-panel__grid">
            <div>
              <dt>Beneficiário</dt>
              <dd>{beneficiaryId || '—'}</dd>
            </div>
            <div>
              <dt>Convênio</dt>
              <dd>{agreementId || '—'}</dd>
            </div>
          </dl>
          <div className="actions" style={{ marginTop: 0 }}>
            {!isDemo ? (
              <button type="button" className="btn btn--secondary" onClick={onApplyDemo}>
                Usar demonstração
              </button>
            ) : null}
            <button
              type="button"
              className="btn btn--ghost"
              onClick={onToggleAdvanced}
              aria-expanded={showAdvancedIds}
            >
              <IconEdit />
              {showAdvancedIds ? 'Ocultar dados' : 'Alterar dados'}
            </button>
          </div>
        </div>

        {showAdvancedIds ? (
          <div className="step-enter">
            <div className="field">
              <label htmlFor="beneficiarioId">ID do beneficiário</label>
              <input
                id="beneficiarioId"
                name="beneficiarioId"
                value={beneficiaryId}
                onChange={(e) => onBeneficiaryChange(e.target.value)}
                autoComplete="off"
                aria-invalid={errors.beneficiaryId ? true : undefined}
              />
              {errors.beneficiaryId ? (
                <p className="field__error">{errors.beneficiaryId}</p>
              ) : null}
            </div>
            <div className="field">
              <label htmlFor="convenioId">ID do convênio</label>
              <input
                id="convenioId"
                name="convenioId"
                value={agreementId}
                onChange={(e) => onAgreementChange(e.target.value)}
                autoComplete="off"
                aria-invalid={errors.agreementId ? true : undefined}
              />
              {errors.agreementId ? (
                <p className="field__error">{errors.agreementId}</p>
              ) : null}
            </div>
          </div>
        ) : null}

        <div className="actions">
          <button
            type="submit"
            className="btn"
            disabled={isPending || !beneficiaryId.trim() || !agreementId.trim()}
            aria-busy={isPending}
          >
            {isPending ? <span className="spinner" aria-hidden="true" /> : null}
            {isPending ? 'Autorizando…' : 'Autorizar e consultar agenda'}
          </button>
        </div>
      </div>
    </form>
  )
}
