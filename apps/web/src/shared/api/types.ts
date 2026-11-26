export type AuthorizationRequest = {
  beneficiarioId: string
  convenioId: string
}

export type AgendaItem = {
  parcelaId: string
  convenioId: string
  valor: number | string
  dataDesconto: string
  status: string
}

export type ReceivingAccount = {
  banco: string
  agencia: string
  conta: string
}

export type PortabilityRequest = {
  beneficiarioId: string
  parcelas: string[]
  credorCessionario: string
  contaRecebimento: ReceivingAccount
}

export type OperationStatus = {
  status: 'PENDENTE' | 'CONFIRMADA' | 'REJEITADA'
  motivoRejeicao: string | null
}

export type OperationEvent = {
  eventType: string
  sequence: number
  occurredAt: string
  description: string
}

export type ApiError = {
  status: number
  message: string
  kind: 'validation' | 'conflict' | 'unavailable' | 'unknown'
}
