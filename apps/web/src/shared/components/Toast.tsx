import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import './Toast.css'

export type ToastTone = 'info' | 'success' | 'warning' | 'error'

export type ToastInput = {
  title?: string
  message: string
  tone?: ToastTone
  /** Auto-dismiss ms. Errors default to sticky (0). */
  durationMs?: number
}

type ToastItem = ToastInput & { id: string }

type ToastContextValue = {
  push: (toast: ToastInput) => void
  dismiss: (id: string) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

function defaultDuration(tone: ToastTone): number {
  return tone === 'error' ? 0 : 4000
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])
  const timers = useRef<Map<string, number>>(new Map())

  const dismiss = useCallback((id: string) => {
    const timer = timers.current.get(id)
    if (timer != null) {
      window.clearTimeout(timer)
      timers.current.delete(id)
    }
    setItems((current) => current.filter((item) => item.id !== id))
  }, [])

  const push = useCallback(
    (toast: ToastInput) => {
      const id = crypto.randomUUID()
      const tone = toast.tone ?? 'info'
      const duration = toast.durationMs ?? defaultDuration(tone)
      setItems((current) => [...current, { ...toast, tone, id }])
      if (duration > 0) {
        const timer = window.setTimeout(() => dismiss(id), duration)
        timers.current.set(id, timer)
      }
    },
    [dismiss],
  )

  const value = useMemo(() => ({ push, dismiss }), [push, dismiss])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-viewport" aria-live="polite" aria-relevant="additions">
        {items.map((item) => (
          <div
            key={item.id}
            className={`toast toast--${item.tone ?? 'info'}`}
            role={item.tone === 'error' ? 'alert' : 'status'}
          >
            <div className="toast__body">
              {item.title ? <p className="toast__title">{item.title}</p> : null}
              <p className="toast__message">{item.message}</p>
            </div>
            <button
              type="button"
              className="toast__close"
              aria-label="Fechar notificação"
              onClick={() => dismiss(item.id)}
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext)
  if (!ctx) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return ctx
}
