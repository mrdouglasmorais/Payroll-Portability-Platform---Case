import { useQuery } from '@tanstack/react-query'
import { PortabilityWizard } from './features/portability/PortabilityWizard'
import './App.css'

type ApiStatus = {
  service: string
  milestone: string
  status: string
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''

async function fetchStatus(): Promise<ApiStatus> {
  const response = await fetch(`${apiBase}/api/v1/status`)
  if (!response.ok) {
    throw new Error(`API status failed: ${response.status}`)
  }
  return response.json()
}

function App() {
  const statusQuery = useQuery({
    queryKey: ['api-status'],
    queryFn: fetchStatus,
    retry: 1,
  })

  return (
    <main className="shell">
      <header className="brand">
        <p className="eyebrow">Payroll Portability</p>
        <h1>Operational console</h1>
        <p className="lede">
          Operator flow: authorize, select installments, submit once, and follow the local
          timeline until the Central outcome is verified.
        </p>
      </header>

      <section className="status-panel" aria-live="polite">
        <h2>Platform API</h2>
        {statusQuery.isPending && <p>Checking API health…</p>}
        {statusQuery.isError && (
          <p className="error">
            Unable to reach the API. Start the stack with{' '}
            <code>docker compose up --build</code>.
          </p>
        )}
        {statusQuery.data && (
          <dl>
            <div>
              <dt>Service</dt>
              <dd>{statusQuery.data.service}</dd>
            </div>
            <div>
              <dt>Milestone</dt>
              <dd>{statusQuery.data.milestone}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd className="ok">{statusQuery.data.status}</dd>
            </div>
          </dl>
        )}
      </section>

      <PortabilityWizard />
    </main>
  )
}

export default App
