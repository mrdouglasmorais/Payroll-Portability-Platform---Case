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
    throw new Error(`Falha ao consultar status da API: ${response.status}`)
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
        <p className="eyebrow">Portabilidade de Folha</p>
        <h1>Console operacional</h1>
        <p className="lede">
          Fluxo do operador: autorizar, selecionar parcelas, enviar uma vez e acompanhar a
          linha do tempo local até a Central confirmar o resultado.
        </p>
      </header>

      <section className="status-panel" aria-live="polite">
        <h2>API da plataforma</h2>
        {statusQuery.isPending && <p>Verificando saúde da API…</p>}
        {statusQuery.isError && (
          <p className="error">
            Não foi possível alcançar a API. Suba o ambiente com{' '}
            <code>docker compose up --build</code>.
          </p>
        )}
        {statusQuery.data && (
          <dl>
            <div>
              <dt>Serviço</dt>
              <dd>{statusQuery.data.service}</dd>
            </div>
            <div>
              <dt>Marco</dt>
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
