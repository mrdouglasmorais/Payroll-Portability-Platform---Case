import { PortabilityWizard } from './features/portability/PortabilityWizard'
import { ServiceStatusBadge } from './shared/components/ServiceStatusBadge'
import { ToastProvider } from './shared/components/Toast'

function App() {
  return (
    <ToastProvider>
      <div className="app-shell">
        <header className="app-header">
          <div className="app-header__brand">
            <img
              className="app-header__logo"
              src="/brand/logo.svg"
              alt="Cielo"
              width={96}
              height={32}
              draggable={false}
            />
            <span className="app-header__divider" aria-hidden="true" />
            <div>
              <p className="app-header__product">Portabilidade de Folha</p>
              <p className="app-header__context">Console operacional · Case Cielo</p>
            </div>
          </div>
          <ServiceStatusBadge />
        </header>
        <main className="app-main">
          <PortabilityWizard />
        </main>
      </div>
    </ToastProvider>
  )
}

export default App
