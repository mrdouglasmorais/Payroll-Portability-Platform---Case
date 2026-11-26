export type WizardStep =
  | 'consentimento'
  | 'agenda'
  | 'destino'
  | 'revisao'
  | 'acompanhamento'

/**
 * Opaque session hints only. Never put bank details, secrets or idempotency keys here.
 */
export type WizardUrlSnapshot = {
  step: WizardStep
  ben: string
  conv: string
  aceite: string | null
  parcelas: string[]
  op: string | null
}

const STEPS: WizardStep[] = [
  'consentimento',
  'agenda',
  'destino',
  'revisao',
  'acompanhamento',
]

export function isWizardStep(value: string | null | undefined): value is WizardStep {
  return value != null && (STEPS as string[]).includes(value)
}

export function parseWizardSearch(
  search: string,
  defaults: { ben: string; conv: string },
): WizardUrlSnapshot {
  const params = new URLSearchParams(
    search.startsWith('?') ? search.slice(1) : search,
  )
  const stepRaw = params.get('step')
  const parcelasRaw = params.get('parcelas')
  const parcelas =
    parcelasRaw == null || parcelasRaw.trim() === ''
      ? []
      : parcelasRaw
          .split(',')
          .map((item) => item.trim())
          .filter(Boolean)

  return {
    step: isWizardStep(stepRaw) ? stepRaw : 'consentimento',
    ben: params.get('ben')?.trim() || defaults.ben,
    conv: params.get('conv')?.trim() || defaults.conv,
    aceite: emptyToNull(params.get('aceite')),
    parcelas,
    op: emptyToNull(params.get('op')),
  }
}

/**
 * Drops the operator to the furthest step that opaque URL hints can support.
 * Account / destination never come from the URL — destination requires form re-entry.
 */
export function clampWizardStep(snapshot: WizardUrlSnapshot): WizardStep {
  const { step, aceite, parcelas, op } = snapshot
  if (step === 'acompanhamento') {
    return op ? 'acompanhamento' : aceite ? 'revisao' : 'consentimento'
  }
  if (step === 'revisao' || step === 'destino') {
    if (!aceite) {
      return 'consentimento'
    }
    if (parcelas.length === 0) {
      return 'agenda'
    }
    // Destination details are not URL-trusted; land on destino to re-enter them.
    return 'destino'
  }
  if (step === 'agenda') {
    return aceite ? 'agenda' : 'consentimento'
  }
  return 'consentimento'
}

export function toWizardSearchParams(snapshot: WizardUrlSnapshot): URLSearchParams {
  const params = new URLSearchParams()
  params.set('step', snapshot.step)
  if (snapshot.ben) {
    params.set('ben', snapshot.ben)
  }
  if (snapshot.conv) {
    params.set('conv', snapshot.conv)
  }
  if (snapshot.aceite) {
    params.set('aceite', snapshot.aceite)
  }
  if (snapshot.parcelas.length > 0) {
    params.set('parcelas', [...snapshot.parcelas].sort().join(','))
  }
  if (snapshot.op) {
    params.set('op', snapshot.op)
  }
  return params
}

export function writeWizardUrl(
  snapshot: WizardUrlSnapshot,
  mode: 'replace' | 'push' = 'replace',
): string {
  const params = toWizardSearchParams(snapshot)
  const query = params.toString()
  const next = `${window.location.pathname}${query ? `?${query}` : ''}${window.location.hash}`
  if (mode === 'push') {
    window.history.pushState(null, '', next)
  } else {
    window.history.replaceState(null, '', next)
  }
  return next
}

function emptyToNull(value: string | null): string | null {
  if (value == null || value.trim() === '') {
    return null
  }
  return value.trim()
}
