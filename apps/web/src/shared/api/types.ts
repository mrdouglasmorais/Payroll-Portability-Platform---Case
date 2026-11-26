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

export type ProcessingView = {
  status:
    | 'PENDING'
    | 'RUNNING'
    | 'RETRYING'
    | 'SUCCEEDED'
    | 'REQUIRES_REVIEW'
    | 'UNKNOWN'
  attemptCount: number
  errorCode: string | null
  requiresAction: boolean
}

export type ReceivingAccountView = {
  banco: string
  agencia: string
  conta: string
}

export type OperationStatus = {
  operationId?: string
  status: 'PENDENTE' | 'CONFIRMADA' | 'REJEITADA'
  motivoRejeicao: string | null
  correlationId?: string
  centralProtocol?: string | null
  contaRecebimento?: ReceivingAccountView
  /** True only after confirmed — destination applied to installments. */
  contaEfetivada?: boolean
  processing?: ProcessingView
}

export type OperationListItem = {
  operationId: string
  status: 'PENDENTE' | 'CONFIRMADA' | 'REJEITADA'
  createdAt: string
  correlationId: string
  centralProtocol: string | null
  contaRecebimento: ReceivingAccountView
  contaEfetivada: boolean
}

export type OperationPage = {
  items: OperationListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export type OperationEvent = {
  eventType: string
  sequence: number
  occurredAt: string
  description: string
}

export type ApiStatus = {
  service: string
  milestone: string
  status: string
}

export type ApiError = {
  status: number
  message: string
  kind: 'validation' | 'conflict' | 'unavailable' | 'unknown'
  reason?: string
}
