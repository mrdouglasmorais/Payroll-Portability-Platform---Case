import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useReducer, useRef, useState } from 'react'
import { isApiError } from '../../shared/api/http'
import { portabilityKeys } from '../../shared/api/queryKeys'
import {
  isTerminal,
  portabilityApiService,
} from '../../shared/api/portabilityApiService'
import type { AgendaItem, ReceivingAccount } from '../../shared/api/types'
import { useToast } from '../../shared/components/Toast'
import {
  labelApiMessage,
} from '../../shared/i18n/labels'
import { createIdempotencyKey } from '../../shared/lib/idempotency'
import {
  clampWizardStep,
  parseWizardSearch,
  writeWizardUrl,
  type WizardStep,
  type WizardUrlSnapshot,
} from './wizardUrlState'

export const DEMO = {
  beneficiaryId: 'ben-ui',
  agreementId: 'conv-1',
  creditor: 'Banco Demonstração S.A.',
  account: { banco: '001', agencia: '1234', conta: '99999-0' } satisfies ReceivingAccount,
}

type WorkflowState = {
  step: WizardStep
  authorizationId: string | null
  operationId: string | null
  stepError: WizardStep | null
}

type WorkflowEvent =
  | { type: 'AUTHORIZATION_SUCCEEDED'; authorizationId: string }
  | { type: 'INSTALLMENTS_SELECTED' }
  | { type: 'DESTINATION_VALIDATED' }
  | { type: 'SUBMISSION_SUCCEEDED'; operationId: string }
  | { type: 'GO_BACK' }
  | { type: 'RESET' }
  | { type: 'RESTORE'; step: WizardStep; authorizationId: string | null; operationId: string | null }
  | { type: 'SET_STEP_ERROR'; step: WizardStep | null }

function workflowReducer(state: WorkflowState, event: WorkflowEvent): WorkflowState {
  switch (event.type) {
    case 'AUTHORIZATION_SUCCEEDED':
      return {
        ...state,
        authorizationId: event.authorizationId,
        step: 'agenda',
        stepError: null,
        operationId: null,
      }
    case 'INSTALLMENTS_SELECTED':
      return { ...state, step: 'destino', stepError: null }
    case 'DESTINATION_VALIDATED':
      return { ...state, step: 'revisao', stepError: null }
    case 'SUBMISSION_SUCCEEDED':
      return {
        ...state,
        operationId: event.operationId,
        step: 'acompanhamento',
        stepError: null,
      }
    case 'GO_BACK': {
      const order: WizardStep[] = [
        'consentimento',
        'agenda',
        'destino',
        'revisao',
        'acompanhamento',
      ]
      const index = order.indexOf(state.step)
      const prev = index > 0 ? order[index - 1]! : 'consentimento'
      return { ...state, step: prev, stepError: null }
    }
    case 'RESET':
      return {
        step: 'consentimento',
        authorizationId: null,
        operationId: null,
        stepError: null,
      }
    case 'RESTORE':
      return {
        step: event.step,
        authorizationId: event.authorizationId,
        operationId: event.operationId,
        stepError: null,
      }
    case 'SET_STEP_ERROR':
      return { ...state, stepError: event.step }
    default:
      return state
  }
}

function intentFingerprint(input: {
  beneficiaryId: string
  selected: string[]
  creditor: string
  account: ReceivingAccount
}): string {
  return JSON.stringify({
    beneficiaryId: input.beneficiaryId.trim(),
    parcelas: [...input.selected].sort(),
    creditor: input.creditor.trim(),
    account: input.account,
  })
}

function describeError(error: unknown, fallback: string): string {
  if (!isApiError(error)) {
    return fallback
  }
  const base = labelApiMessage(error.message)
  if (error.reason) {
    return `${base} (${error.reason})`
  }
  return base
}

function readInitialUrl(): WizardUrlSnapshot {
  const parsed = parseWizardSearch(window.location.search, {
    ben: DEMO.beneficiaryId,
    conv: DEMO.agreementId,
  })
  return { ...parsed, step: clampWizardStep(parsed) }
}

export function usePortabilityFlow() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [initial] = useState(readInitialUrl)

  const [workflow, dispatch] = useReducer(workflowReducer, initial, (url) => ({
    step: url.step,
    authorizationId: url.aceite,
    operationId: url.op,
    stepError: null,
  }))

  const [beneficiaryId, setBeneficiaryId] = useState(initial.ben)
  const [agreementId, setAgreementId] = useState(initial.conv)
  const [selected, setSelected] = useState<string[]>(initial.parcelas)
  const [creditor, setCreditor] = useState('')
  const [account, setAccount] = useState<ReceivingAccount>({
    banco: '',
    agencia: '',
    conta: '',
  })
  const [idempotencyKey, setIdempotencyKey] = useState(() => createIdempotencyKey())
  const [boundIntent, setBoundIntent] = useState<string | null>(null)
  const [showAdvancedIds, setShowAdvancedIds] = useState(false)
  const [eventsCatchUpDone, setEventsCatchUpDone] = useState(false)
  const [inlineError, setInlineError] = useState<string | null>(null)
  const [historyPage, setHistoryPage] = useState(0)
  const historyPageSize = 10

  const lastStepRef = useRef<WizardStep>(initial.step)
  const skipNextUrlWriteRef = useRef(false)

  const urlSnapshot = useMemo<WizardUrlSnapshot>(
    () => ({
      step: workflow.step,
      ben: beneficiaryId.trim() || DEMO.beneficiaryId,
      conv: agreementId.trim() || DEMO.agreementId,
      aceite: workflow.authorizationId,
      parcelas: selected,
      op: workflow.operationId,
    }),
    [
      workflow.step,
      workflow.authorizationId,
      workflow.operationId,
      beneficiaryId,
      agreementId,
      selected,
    ],
  )

  useEffect(() => {
    if (skipNextUrlWriteRef.current) {
      skipNextUrlWriteRef.current = false
      lastStepRef.current = workflow.step
      return
    }
    const mode = lastStepRef.current === workflow.step ? 'replace' : 'push'
    lastStepRef.current = workflow.step
    writeWizardUrl(urlSnapshot, mode)
  }, [urlSnapshot, workflow.step])

  useEffect(() => {
    function onPopState() {
      const parsed = parseWizardSearch(window.location.search, {
        ben: DEMO.beneficiaryId,
        conv: DEMO.agreementId,
      })
      const nextStep = clampWizardStep(parsed)
      skipNextUrlWriteRef.current = true
      dispatch({
        type: 'RESTORE',
        step: nextStep,
        authorizationId: parsed.aceite,
        operationId: parsed.op,
      })
      setBeneficiaryId(parsed.ben)
      setAgreementId(parsed.conv)
      setSelected(parsed.parcelas)
      setBoundIntent(null)
      lastStepRef.current = nextStep
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  const agendaQuery = useQuery({
    queryKey: portabilityKeys.agenda(beneficiaryId.trim()),
    queryFn: () => portabilityApiService.fetchAgenda(beneficiaryId.trim()),
    enabled:
      !!workflow.authorizationId &&
      workflow.step !== 'consentimento' &&
      beneficiaryId.trim().length > 0,
    retry: 1,
  })

  useEffect(() => {
    if (!agendaQuery.data) {
      return
    }
    setSelected((current) =>
      current.filter((id) => agendaQuery.data.some((item) => item.parcelaId === id)),
    )
  }, [agendaQuery.data])

  const agendaErrorToasted = useRef(false)
  useEffect(() => {
    if (!agendaQuery.isError) {
      agendaErrorToasted.current = false
      return
    }
    setInlineError(
      `Não foi possível recuperar a agenda: ${describeError(agendaQuery.error, 'erro desconhecido')}`,
    )
    dispatch({ type: 'SET_STEP_ERROR', step: 'agenda' })
    if (!agendaErrorToasted.current) {
      agendaErrorToasted.current = true
      toast.push({
        tone: 'error',
        title: 'Agenda',
        message: describeError(agendaQuery.error, 'Falha ao carregar agenda'),
      })
    }
  }, [agendaQuery.isError, agendaQuery.error, toast])

  const agenda: AgendaItem[] = agendaQuery.data ?? []

  const currentIntent = useMemo(
    () =>
      intentFingerprint({
        beneficiaryId,
        selected,
        creditor,
        account,
      }),
    [beneficiaryId, selected, creditor, account],
  )

  useEffect(() => {
    if (boundIntent == null || boundIntent === currentIntent) {
      return
    }
    setIdempotencyKey(createIdempotencyKey())
    setBoundIntent(null)
    toast.push({
      tone: 'info',
      title: 'Nova chave de idempotência',
      message:
        'A intenção financeira mudou após uma tentativa de envio. Uma nova chave foi gerada.',
    })
  }, [boundIntent, currentIntent, toast])

  const authorizeMutation = useMutation({
    mutationFn: () =>
      portabilityApiService.authorizeAndLoadAgenda({
        beneficiarioId: beneficiaryId.trim(),
        convenioId: agreementId.trim(),
      }),
    retry: false,
    onSuccess: (data) => {
      setInlineError(null)
      setSelected([])
      queryClient.setQueryData(portabilityKeys.agenda(beneficiaryId.trim()), data.items)
      dispatch({
        type: 'AUTHORIZATION_SUCCEEDED',
        authorizationId: data.autorizacaoId,
      })
      toast.push({
        tone: 'success',
        title: 'Autorização registrada',
        message: 'Consentimento aceito. Agenda carregada.',
      })
    },
    onError: (error) => {
      dispatch({ type: 'SET_STEP_ERROR', step: 'consentimento' })
      const message =
        isApiError(error) && error.kind === 'unavailable'
          ? `Falha de integração (não é rejeição): ${describeError(error, 'indisponível')}`
          : describeError(error, 'Falha na autorização')
      setInlineError(message)
      toast.push({ tone: 'error', title: 'Autorização', message })
    },
  })

  const submitMutation = useMutation({
    mutationFn: () => {
      setBoundIntent(currentIntent)
      return portabilityApiService.createPortability(
        {
          beneficiarioId: beneficiaryId.trim(),
          parcelas: [...selected].sort(),
          credorCessionario: creditor.trim(),
          contaRecebimento: account,
        },
        idempotencyKey,
      )
    },
    retry: false,
    onSuccess: (data) => {
      setInlineError(null)
      setEventsCatchUpDone(false)
      dispatch({ type: 'SUBMISSION_SUCCEEDED', operationId: data.operationId })
      setHistoryPage(0)
      void queryClient.invalidateQueries({
        queryKey: [...portabilityKeys.all, 'operations', beneficiaryId.trim()],
      })
      toast.push({
        tone: 'success',
        title: 'Operação aceita',
        message: 'Acompanhe o status até a confirmação da Central.',
      })
    },
    onError: (error) => {
      dispatch({ type: 'SET_STEP_ERROR', step: 'revisao' })
      if (isApiError(error) && error.kind === 'conflict') {
        const message = `Conflito: ${describeError(error, 'operação conflitante')}.`
        setInlineError(message)
        toast.push({ tone: 'warning', title: 'Conflito', message })
        return
      }
      if (isApiError(error) && error.kind === 'unavailable') {
        const message = `Falha de integração (não é rejeição): ${describeError(error, 'indisponível')}. A próxima tentativa mantém a mesma chave.`
        setInlineError(message)
        toast.push({ tone: 'error', title: 'Integração', message })
        return
      }
      const message = describeError(error, 'Falha no envio')
      setInlineError(message)
      toast.push({ tone: 'error', title: 'Envio', message })
    },
  })

  useEffect(() => {
    setHistoryPage(0)
  }, [beneficiaryId])

  const historyQuery = useQuery({
    queryKey: portabilityKeys.operations(
      beneficiaryId.trim() || 'none',
      historyPage,
      historyPageSize,
    ),
    queryFn: () =>
      portabilityApiService.listOperations(
        beneficiaryId.trim(),
        historyPage,
        historyPageSize,
      ),
    enabled: beneficiaryId.trim().length > 0,
    retry: 1,
    placeholderData: (previous) => previous,
  })

  useEffect(() => {
    const totalPages = historyQuery.data?.totalPages ?? 0
    if (totalPages > 0 && historyPage >= totalPages) {
      setHistoryPage(totalPages - 1)
    }
  }, [historyQuery.data?.totalPages, historyPage])

  const statusQuery = useQuery({
    queryKey: portabilityKeys.operation(workflow.operationId ?? 'none'),
    queryFn: () => portabilityApiService.getOperation(workflow.operationId!),
    enabled: workflow.step === 'acompanhamento' && !!workflow.operationId,
    refetchInterval: (query) =>
      isTerminal(query.state.data?.status) ? false : 1500,
  })

  const statusTerminal = isTerminal(statusQuery.data?.status)

  useEffect(() => {
    if (!statusTerminal) {
      setEventsCatchUpDone(false)
      return
    }
    const ben = beneficiaryId.trim()
    if (ben) {
      void queryClient.invalidateQueries({
        queryKey: [...portabilityKeys.all, 'operations', ben],
      })
    }
  }, [statusTerminal, beneficiaryId, queryClient])

  const eventsQuery = useQuery({
    queryKey: portabilityKeys.events(workflow.operationId ?? 'none'),
    queryFn: () => portabilityApiService.getEvents(workflow.operationId!),
    enabled: workflow.step === 'acompanhamento' && !!workflow.operationId,
    refetchInterval: (query) => {
      if (!statusTerminal) {
        return 1500
      }
      const events = query.state.data ?? []
      const hasTerminalEvent = events.some(
        (event) =>
          event.eventType === 'OPERATION_CONFIRMED' ||
          event.eventType === 'OPERATION_REJECTED',
      )
      if (hasTerminalEvent || eventsCatchUpDone) {
        return false
      }
      return 1500
    },
  })

  useEffect(() => {
    if (!statusTerminal || eventsCatchUpDone) {
      return
    }
    const events = eventsQuery.data ?? []
    const hasTerminalEvent = events.some(
      (event) =>
        event.eventType === 'OPERATION_CONFIRMED' ||
        event.eventType === 'OPERATION_REJECTED',
    )
    if (hasTerminalEvent) {
      setEventsCatchUpDone(true)
      return
    }
    const timer = window.setTimeout(() => setEventsCatchUpDone(true), 2000)
    return () => window.clearTimeout(timer)
  }, [statusTerminal, eventsQuery.data, eventsCatchUpDone])

  const toggleParcela = useCallback((id: string) => {
    setSelected((current) =>
      current.includes(id) ? current.filter((x) => x !== id) : [...current, id],
    )
  }, [])

  const applyDemoIdentity = useCallback(() => {
    setBeneficiaryId(DEMO.beneficiaryId)
    setAgreementId(DEMO.agreementId)
    setShowAdvancedIds(false)
    toast.push({
      tone: 'info',
      message: 'Demonstração pronta: ben-ui / conv-1.',
    })
  }, [toast])

  const applyDemoDestination = useCallback(() => {
    setCreditor(DEMO.creditor)
    setAccount({ ...DEMO.account })
    toast.push({
      tone: 'info',
      message: 'Conta de demonstração preenchida.',
    })
  }, [toast])

  const goDestination = useCallback(() => {
    const selectable = agenda.filter((item) => item.status === 'OPEN')
    if (selectable.length === 0) {
      const message =
        'Não há parcelas elegíveis. Use a demonstração ben-ui / conv-1 ou reinicialize o ambiente.'
      setInlineError(message)
      dispatch({ type: 'SET_STEP_ERROR', step: 'agenda' })
      toast.push({ tone: 'warning', title: 'Parcelas', message })
      return
    }
    if (selected.length === 0) {
      const message = 'Selecione ao menos uma parcela aberta para continuar.'
      setInlineError(message)
      dispatch({ type: 'SET_STEP_ERROR', step: 'agenda' })
      toast.push({ tone: 'warning', message })
      return
    }
    setInlineError(null)
    dispatch({ type: 'INSTALLMENTS_SELECTED' })
  }, [agenda, selected.length, toast])

  const goReview = useCallback(() => {
    setInlineError(null)
    dispatch({ type: 'DESTINATION_VALIDATED' })
  }, [])

  const startOver = useCallback(() => {
    dispatch({ type: 'RESET' })
    setSelected([])
    setCreditor('')
    setAccount({ banco: '', agencia: '', conta: '' })
    setBeneficiaryId(DEMO.beneficiaryId)
    setAgreementId(DEMO.agreementId)
    setIdempotencyKey(createIdempotencyKey())
    setBoundIntent(null)
    setEventsCatchUpDone(false)
    setInlineError(null)
    setShowAdvancedIds(false)
    setHistoryPage(0)
    queryClient.removeQueries({ queryKey: portabilityKeys.all })
  }, [queryClient])

  const goHistoryPrevious = useCallback(() => {
    setHistoryPage((current) => Math.max(0, current - 1))
  }, [])

  const goHistoryNext = useCallback(() => {
    setHistoryPage((current) => current + 1)
  }, [])

  const openOperation = useCallback((operationId: string) => {
    setInlineError(null)
    setEventsCatchUpDone(false)
    dispatch({
      type: 'RESTORE',
      step: 'acompanhamento',
      authorizationId: workflow.authorizationId,
      operationId,
    })
  }, [workflow.authorizationId])

  const goBack = useCallback(() => {
    setInlineError(null)
    dispatch({ type: 'GO_BACK' })
  }, [])

  return {
    workflow,
    beneficiaryId,
    setBeneficiaryId,
    agreementId,
    setAgreementId,
    selected,
    creditor,
    setCreditor,
    account,
    setAccount,
    idempotencyKey,
    showAdvancedIds,
    setShowAdvancedIds,
    inlineError,
    setInlineError,
    agenda,
    agendaQuery,
    authorizeMutation,
    submitMutation,
    statusQuery,
    eventsQuery,
    historyQuery,
    historyPage,
    goHistoryPrevious,
    goHistoryNext,
    statusTerminal,
    toggleParcela,
    applyDemoIdentity,
    applyDemoDestination,
    goDestination,
    goReview,
    goBack,
    openOperation,
    startOver,
    describeError,
  }
}

export type PortabilityFlow = ReturnType<typeof usePortabilityFlow>
