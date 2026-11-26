import { useState } from 'react'
import type { ReceivingAccount } from '../../../shared/api/types'
import {
  hasDestinationErrors,
  validateDestination,
  type DestinationErrors,
} from '../validation'

type Props = {
  creditor: string
  account: ReceivingAccount
  onCreditorChange: (value: string) => void
  onAccountChange: (account: ReceivingAccount) => void
  onApplyDemo: () => void
  onBack: () => void
  onContinue: () => void
}

export function DestinationStep({
  creditor,
  account,
  onCreditorChange,
  onAccountChange,
  onApplyDemo,
  onBack,
  onContinue,
}: Props) {
  const [errors, setErrors] = useState<DestinationErrors>({})

  return (
    <form
      className="card step-enter"
      onSubmit={(event) => {
        event.preventDefault()
        const next = validateDestination(creditor, account)
        setErrors(next)
        if (hasDestinationErrors(next)) {
          return
        }
        onContinue()
      }}
    >
      <div className="card__body">
        <div className="card__title-row">
          <span className="card__icon card__icon--accent">
            <img src="/brand/icon-store-page.svg" alt="" width={28} height={28} draggable={false} />
          </span>
          <h3 className="card__title">Conta de recebimento</h3>
        </div>
        <p className="card__desc">
          Informe o credor cessionário e a conta que receberá os valores após a confirmação.
        </p>

        <div className="actions" style={{ marginTop: 0, marginBottom: 16 }}>
          <button type="button" className="btn btn--secondary" onClick={onApplyDemo}>
            Preencher demonstração
          </button>
        </div>

        <div className="field">
          <label htmlFor="credor">Credor cessionário</label>
          <input
            id="credor"
            value={creditor}
            onChange={(e) => onCreditorChange(e.target.value)}
            autoComplete="off"
            aria-invalid={errors.creditor ? true : undefined}
          />
          {errors.creditor ? <p className="field__error">{errors.creditor}</p> : null}
        </div>

        <div className="field-grid field-grid--3">
          <div className="field">
            <label htmlFor="banco">Banco</label>
            <input
              id="banco"
              value={account.banco}
              onChange={(e) => onAccountChange({ ...account, banco: e.target.value })}
              autoComplete="off"
              inputMode="numeric"
              aria-invalid={errors.banco ? true : undefined}
            />
            {errors.banco ? <p className="field__error">{errors.banco}</p> : null}
          </div>
          <div className="field">
            <label htmlFor="agencia">Agência</label>
            <input
              id="agencia"
              value={account.agencia}
              onChange={(e) => onAccountChange({ ...account, agencia: e.target.value })}
              autoComplete="off"
              aria-invalid={errors.agencia ? true : undefined}
            />
            {errors.agencia ? <p className="field__error">{errors.agencia}</p> : null}
          </div>
          <div className="field">
            <label htmlFor="conta">Conta</label>
            <input
              id="conta"
              value={account.conta}
              onChange={(e) => onAccountChange({ ...account, conta: e.target.value })}
              autoComplete="off"
              aria-invalid={errors.conta ? true : undefined}
            />
            {errors.conta ? <p className="field__error">{errors.conta}</p> : null}
          </div>
        </div>

        <div className="actions">
          <button type="button" className="btn btn--secondary" onClick={onBack}>
            Voltar
          </button>
          <button type="submit" className="btn">
            Revisar
          </button>
        </div>
      </div>
    </form>
  )
}
