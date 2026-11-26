import type { ReceivingAccount } from '../../shared/api/types'

export type DestinationErrors = {
  creditor?: string
  banco?: string
  agencia?: string
  conta?: string
}

export function validateDestination(
  creditor: string,
  account: ReceivingAccount,
): DestinationErrors {
  const errors: DestinationErrors = {}
  if (!creditor.trim()) {
    errors.creditor = 'Informe o credor cessionário.'
  }
  if (!account.banco.trim()) {
    errors.banco = 'Informe o banco.'
  }
  if (!account.agencia.trim()) {
    errors.agencia = 'Informe a agência.'
  }
  if (!account.conta.trim()) {
    errors.conta = 'Informe a conta.'
  }
  return errors
}

export function hasDestinationErrors(errors: DestinationErrors): boolean {
  return Object.keys(errors).length > 0
}

export function validateIdentity(beneficiaryId: string, agreementId: string): {
  beneficiaryId?: string
  agreementId?: string
} {
  const errors: { beneficiaryId?: string; agreementId?: string } = {}
  if (!beneficiaryId.trim()) {
    errors.beneficiaryId = 'Informe o beneficiário.'
  }
  if (!agreementId.trim()) {
    errors.agreementId = 'Informe o convênio.'
  }
  return errors
}
