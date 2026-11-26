import { IconCheck } from '../../../shared/components/Icons'
import type { WizardStep } from '../wizardUrlState'

const STEPS: { id: WizardStep; label: string }[] = [
  { id: 'consentimento', label: 'Consentimento' },
  { id: 'agenda', label: 'Parcelas' },
  { id: 'destino', label: 'Destino' },
  { id: 'revisao', label: 'Revisão' },
  { id: 'acompanhamento', label: 'Resultado' },
]

type StepState = 'completed' | 'active' | 'upcoming' | 'error'

type Props = {
  current: WizardStep
  errorStep?: WizardStep | null
}

export function PortabilityStepper({ current, errorStep }: Props) {
  const currentIndex = STEPS.findIndex((step) => step.id === current)
  const currentLabel = STEPS[currentIndex]?.label ?? ''

  return (
    <nav className="stepper-nav" aria-label="Etapas do fluxo">
      <ol className="stepper">
        {STEPS.map((step, index) => {
          let state: StepState = 'upcoming'
          if (errorStep === step.id) {
            state = 'error'
          } else if (index < currentIndex) {
            state = 'completed'
          } else if (index === currentIndex) {
            state = 'active'
          }

          return (
            <li
              key={step.id}
              className="stepper__item"
              data-state={state}
              aria-current={state === 'active' ? 'step' : undefined}
            >
              <div className="stepper__node">
                <span className="stepper__circle" aria-hidden="true">
                  {state === 'completed' ? (
                    <IconCheck className="stepper__check" />
                  ) : (
                    <span>{index + 1}</span>
                  )}
                </span>
                <span className="stepper__label">{step.label}</span>
              </div>
              {index < STEPS.length - 1 ? (
                <span className="stepper__connector" aria-hidden="true" />
              ) : null}
            </li>
          )
        })}
      </ol>
      <p className="stepper__caption" aria-live="polite">
        <span className="stepper__caption-index">
          Etapa {currentIndex + 1} de {STEPS.length}
        </span>
        <span className="stepper__caption-label">{currentLabel}</span>
      </p>
    </nav>
  )
}
