import { describe, expect, it } from 'vitest'
import {
  clampWizardStep,
  parseWizardSearch,
  toWizardSearchParams,
  type WizardUrlSnapshot,
} from './wizardUrlState'

const defaults = { ben: 'ben-ui', conv: 'conv-1' }

function base(partial: Partial<WizardUrlSnapshot> = {}): WizardUrlSnapshot {
  return {
    step: 'consentimento',
    ben: 'ben-ui',
    conv: 'conv-1',
    aceite: null,
    parcelas: [],
    op: null,
    ...partial,
  }
}

describe('wizardUrlState', () => {
  it('parseia apenas hints opacos da query', () => {
    const parsed = parseWizardSearch(
      '?step=revisao&ben=ben-ui&conv=conv-1&aceite=auth-1&parcelas=p2,p1&credor=Banco&banco=001&idem=idem-1&op=op-1',
      defaults,
    )
    expect(parsed.step).toBe('revisao')
    expect(parsed.aceite).toBe('auth-1')
    expect(parsed.parcelas).toEqual(['p2', 'p1'])
    expect(parsed.op).toBe('op-1')
    expect(parsed).not.toHaveProperty('credor')
    expect(parsed).not.toHaveProperty('idem')
  })

  it('serializa step, ids opacos e parcelas ordenadas', () => {
    const params = toWizardSearchParams(
      base({
        step: 'destino',
        aceite: 'auth-9',
        parcelas: ['b', 'a'],
      }),
    )
    expect(params.get('step')).toBe('destino')
    expect(params.get('aceite')).toBe('auth-9')
    expect(params.get('parcelas')).toBe('a,b')
    expect(params.get('banco')).toBeNull()
    expect(params.get('idem')).toBeNull()
    expect(params.get('op')).toBeNull()
  })

  it('recua a etapa quando faltam hints e nunca confia destino na URL', () => {
    expect(clampWizardStep(base({ step: 'agenda' }))).toBe('consentimento')
    expect(
      clampWizardStep(base({ step: 'destino', aceite: 'auth-1' })),
    ).toBe('agenda')
    expect(
      clampWizardStep(
        base({
          step: 'revisao',
          aceite: 'auth-1',
          parcelas: ['p1'],
        }),
      ),
    ).toBe('destino')
    expect(
      clampWizardStep(
        base({
          step: 'acompanhamento',
          aceite: 'auth-1',
          op: 'op-1',
        }),
      ),
    ).toBe('acompanhamento')
  })
})
