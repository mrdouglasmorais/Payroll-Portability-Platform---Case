import { describe, expect, it } from 'vitest'
import {
  labelApiMessage,
  labelBusinessStatus,
  labelErrorCode,
  labelEventType,
  labelInstallmentStatus,
} from './labels'

describe('labels pt-BR', () => {
  it('traduz status de negócio e de parcela', () => {
    expect(labelBusinessStatus('CONFIRMADA')).toBe('Confirmada')
    expect(labelBusinessStatus('PENDENTE')).toBe('Pendente')
    expect(labelInstallmentStatus('OPEN')).toBe('Aberta')
  })

  it('traduz tipos de evento e códigos de erro', () => {
    expect(labelEventType('OPERATION_CREATED')).toBe('Operação criada')
    expect(labelErrorCode('INTEGRATION_ERROR')).toBe('Erro de integração')
  })

  it('traduz mensagens conhecidas da API e preserva sufixos', () => {
    expect(labelApiMessage('Central unavailable')).toBe('Central indisponível')
    expect(labelApiMessage('Installment already reserved: parcela-1')).toBe(
      'Parcela já reservada: parcela-1',
    )
    expect(
      labelApiMessage('Idempotency key reused with a different payload'),
    ).toBe('Chave de idempotência reutilizada com payload diferente')
  })
})
