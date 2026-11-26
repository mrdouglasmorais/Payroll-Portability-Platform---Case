import { useMemo } from 'react'
import { AgendaStep } from './components/AgendaStep'
import { ConsentStep } from './components/ConsentStep'
import { DestinationStep } from './components/DestinationStep'
import { OperationSummary } from './components/OperationSummary'
import { PortabilityStepper } from './components/PortabilityStepper'
import { ReviewStep } from './components/ReviewStep'
import { TrackingStep } from './components/TrackingStep'
import './portability.css'
import { usePortabilityFlow } from './usePortabilityFlow'

export function PortabilityWizard() {
  const flow = usePortabilityFlow()
  const selectedItems = useMemo(
    () => flow.agenda.filter((item) => flow.selected.includes(item.parcelaId)),
    [flow.agenda, flow.selected],
  )

  return (
    <div className="app-grid">
      <section className="portability" aria-label="Fluxo de portabilidade">
        <header className="page-intro">
          <div className="page-intro__copy">
            <img
              className="page-intro__brand-mark"
              src="/brand/hero-logomarca.webp"
              alt="Inteligência de Mercado Cielo"
              width={320}
              height={144}
              draggable={false}
            />
            <p className="page-intro__eyebrow">Portabilidade de Folha</p>
            <h1>Nova operação</h1>
            <p>
              Autorize o beneficiário, selecione parcelas abertas e acompanhe a confirmação da
              Central — com clareza operacional e rastreabilidade completa.
            </p>
          </div>
          <div className="page-intro__art" aria-hidden="true">
            <img
              src="/brand/hero-person.webp"
              alt=""
              width={220}
              height={220}
              draggable={false}
            />
          </div>
        </header>

        <PortabilityStepper
          current={flow.workflow.step}
          errorStep={flow.workflow.stepError}
        />

        {flow.workflow.step === 'consentimento' ? (
          <ConsentStep
            beneficiaryId={flow.beneficiaryId}
            agreementId={flow.agreementId}
            showAdvancedIds={flow.showAdvancedIds}
            isPending={flow.authorizeMutation.isPending}
            inlineError={flow.inlineError}
            onBeneficiaryChange={flow.setBeneficiaryId}
            onAgreementChange={flow.setAgreementId}
            onToggleAdvanced={() => flow.setShowAdvancedIds((v) => !v)}
            onApplyDemo={flow.applyDemoIdentity}
            onSubmit={() => flow.authorizeMutation.mutate()}
          />
        ) : null}

        {flow.workflow.step === 'agenda' ? (
          <AgendaStep
            authorizationId={flow.workflow.authorizationId}
            beneficiaryId={flow.beneficiaryId}
            agreementId={flow.agreementId}
            agenda={flow.agenda}
            selected={flow.selected}
            loading={flow.agendaQuery.isFetching && !flow.agendaQuery.data}
            inlineError={flow.inlineError}
            onToggle={flow.toggleParcela}
            onBack={flow.goBack}
            onContinue={flow.goDestination}
          />
        ) : null}

        {flow.workflow.step === 'destino' ? (
          <DestinationStep
            creditor={flow.creditor}
            account={flow.account}
            onCreditorChange={flow.setCreditor}
            onAccountChange={flow.setAccount}
            onApplyDemo={flow.applyDemoDestination}
            onBack={flow.goBack}
            onContinue={flow.goReview}
          />
        ) : null}

        {flow.workflow.step === 'revisao' ? (
          <ReviewStep
            beneficiaryId={flow.beneficiaryId}
            selectedItems={selectedItems}
            creditor={flow.creditor}
            account={flow.account}
            idempotencyKey={flow.idempotencyKey}
            isPending={flow.submitMutation.isPending}
            inlineError={flow.inlineError}
            onBack={flow.goBack}
            onSubmit={() => flow.submitMutation.mutate()}
          />
        ) : null}

        {flow.workflow.step === 'acompanhamento' && flow.workflow.operationId ? (
          <TrackingStep
            operationId={flow.workflow.operationId}
            statusQuery={flow.statusQuery}
            eventsQuery={flow.eventsQuery}
            describeError={flow.describeError}
            onRestart={flow.startOver}
          />
        ) : null}
      </section>

      <OperationSummary
        step={flow.workflow.step}
        beneficiaryId={flow.beneficiaryId}
        agreementId={flow.agreementId}
        authorizationId={flow.workflow.authorizationId}
        selectedItems={selectedItems}
        creditor={flow.creditor}
        account={flow.account}
        operationId={flow.workflow.operationId}
        businessStatus={flow.statusQuery.data?.status}
      />
    </div>
  )
}
