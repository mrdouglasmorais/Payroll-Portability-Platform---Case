/** Rótulos pt-BR para códigos de contrato/API exibidos no console. */

const BUSINESS_STATUS: Record<string, string> = {
  PENDENTE: 'Pendente',
  CONFIRMADA: 'Confirmada',
  REJEITADA: 'Rejeitada',
}

const INSTALLMENT_STATUS: Record<string, string> = {
  OPEN: 'Aberta',
  RESERVED: 'Reservada',
  PORTED: 'Portada',
  CLOSED: 'Encerrada',
}

const EVENT_TYPE: Record<string, string> = {
  OPERATION_CREATED: 'Operação criada',
  PROTOCOL_RECEIVED: 'Protocolo recebido',
  OPERATION_CONFIRMED: 'Operação confirmada',
  OPERATION_REJECTED: 'Operação rejeitada',
  INTEGRATION_FAILURE: 'Falha de integração',
}

const ERROR_CODE: Record<string, string> = {
  INTEGRATION_ERROR: 'Erro de integração',
  PERMANENT_CENTRAL_ERROR: 'Erro permanente da Central',
  POLL_DEADLINE_EXCEEDED: 'Prazo de consulta esgotado',
  RATE_LIMITED: 'Limite de taxa',
  CENTRAL_UNAVAILABLE: 'Central indisponível',
  HMAC_FAILURE: 'Falha de verificação HMAC',
}

const KNOWN_API_MESSAGES: Record<string, string> = {
  'Request failed': 'Falha na requisição',
  // Mensagens legadas (EN) e equivalentes — a API agora emite pt-BR.
  'Central unavailable': 'Central indisponível',
  'Central unavailable: HTTP': 'Central indisponível: HTTP',
  'Idempotency key reused with a different payload':
    'Chave de idempotência reutilizada com payload diferente',
  'Rejected by Central': 'Rejeitada pela Central',
  'Portability operation created': 'Operação de portabilidade criada',
  'Central protocol attached': 'Protocolo da Central anexado',
  'Operation confirmed by Central': 'Operação confirmada pela Central',
  'Beneficiary not found': 'Beneficiário não encontrado',
  'No accepted consent': 'Não há consentimento aceito',
  'Unknown beneficiary': 'Beneficiário desconhecido',
  'Not found': 'Não encontrado',
  'parcelas required': 'Informe ao menos uma parcela',
  'Idempotency-Key is required': 'Cabeçalho Idempotency-Key é obrigatório',
  'Installment already reserved': 'Parcela já reservada',
  'One or more installments were not found': 'Uma ou mais parcelas não foram encontradas',
  'Beneficiary has no accepted consent': 'Beneficiário sem consentimento aceito',
  'Central rate limited': 'Central com limite de taxa',
  'Central conflict': 'Conflito na Central',
  'Central transport failure': 'Falha de transporte com a Central',
  'Central HTTP error:': 'Erro HTTP da Central:',
  'Permanent central error:': 'Erro permanente da Central:',
  missing: 'Recurso não encontrado',
}

export function labelBusinessStatus(status: string | undefined | null): string {
  if (!status) return BUSINESS_STATUS.PENDENTE
  return BUSINESS_STATUS[status] ?? status
}

export function labelInstallmentStatus(status: string): string {
  return INSTALLMENT_STATUS[status] ?? status
}

export function labelEventType(eventType: string): string {
  return EVENT_TYPE[eventType] ?? eventType
}

export function labelErrorCode(code: string | null | undefined): string {
  if (!code) return ERROR_CODE.INTEGRATION_ERROR
  return ERROR_CODE[code] ?? code
}

export function labelApiMessage(message: string): string {
  if (KNOWN_API_MESSAGES[message]) {
    return KNOWN_API_MESSAGES[message]
  }
  for (const [en, pt] of Object.entries(KNOWN_API_MESSAGES)) {
    if (message.startsWith(en)) {
      return message.replace(en, pt)
    }
  }
  return message
}

export function formatOccurredAt(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'medium',
    timeZone: 'UTC',
  }).format(date)
}

export function formatDiscountDate(isoDate: string): string {
  const date = new Date(`${isoDate}T00:00:00`)
  if (Number.isNaN(date.getTime())) return isoDate
  return new Intl.DateTimeFormat('pt-BR').format(date)
}
