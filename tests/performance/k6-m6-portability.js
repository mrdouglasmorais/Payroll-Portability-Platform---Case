import http from 'k6/http'
import { check, sleep } from 'k6'
import { Rate, Trend } from 'k6/metrics'
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js'

/**
 * M6 portability load — platform API only (Central signed by the API).
 * Contending VUs may exhaust OPEN parcels; empty agenda is treated as a valid
 * contention outcome, not a transport failure. No invented production SLOs.
 *
 *   docker run --rm -i --network host \
 *     -e API_BASE=http://localhost:8080 \
 *     grafana/k6:0.54.0 run - < tests/performance/k6-m6-portability.js
 */

const apiBase = __ENV.API_BASE || 'http://localhost:8080'
const errorRate = new Rate('errors')
const authorizeMs = new Trend('authorize_ms', true)
const createMs = new Trend('create_ms', true)
const statusMs = new Trend('status_ms', true)

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 3,
      duration: '45s',
      tags: { scenario: 'smoke' },
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    errors: ['rate<0.05'],
    authorize_ms: ['p(95)<2000'],
    create_ms: ['p(95)<2000'],
    status_ms: ['p(95)<1000'],
  },
}

function headers(extra) {
  return Object.assign(
    {
      'Content-Type': 'application/json',
      'X-Correlation-Id': `k6-${uuidv4()}`,
    },
    extra || {},
  )
}

export function setup() {
  // Treat business contention (400/409) as expected responses for http_req_failed.
  http.setResponseCallback(http.expectedStatuses(200, 201, 202, 400, 409))
}

export default function () {
  // Ensure callback is set even when setup is skipped by some runners.
  http.setResponseCallback(http.expectedStatuses(200, 201, 202, 400, 409))

  const beneficiary = 'ben-001'
  const convenio = 'conv-1'

  const authRes = http.post(
    `${apiBase}/api/v1/autorizacoes`,
    JSON.stringify({ beneficiarioId: beneficiary, convenioId: convenio }),
    { headers: headers(), tags: { name: 'authorize' } },
  )
  authorizeMs.add(authRes.timings.duration)
  const authOk = check(authRes, {
    'authorize 201': (r) => r.status === 201,
  })
  errorRate.add(!authOk)
  if (!authOk) {
    sleep(1)
    return
  }

  const agendaRes = http.get(`${apiBase}/api/v1/beneficiarios/${beneficiary}/agenda`, {
    headers: headers(),
    tags: { name: 'agenda' },
  })
  let agenda = []
  try {
    agenda = agendaRes.json()
  } catch {
    agenda = null
  }
  const agendaOk = check(agendaRes, {
    'agenda 200': (r) => r.status === 200,
    'agenda array': () => Array.isArray(agenda),
  })
  errorRate.add(!agendaOk)
  if (!agendaOk || !Array.isArray(agenda) || agenda.length === 0) {
    // Under contention all OPEN parcels may already be reserved/ported.
    sleep(0.5)
    return
  }

  // Prefer a VU-specific load parcel when present to reduce collisions.
  const preferred = agenda.find((item) => item.parcelaId === `parcela-load-${String(__VU).padStart(2, '0')}`)
  const parcela = (preferred || agenda[__ITER % agenda.length]).parcelaId
  const idem = `k6-${uuidv4()}`
  const body = JSON.stringify({
    beneficiarioId: beneficiary,
    parcelas: [parcela],
    credorCessionario: 'creditor-k6',
    contaRecebimento: { banco: '001', agencia: '1234', conta: `${__VU}-${__ITER}` },
  })

  const createRes = http.post(`${apiBase}/api/v1/operacoes-portabilidade`, body, {
    headers: headers({ 'Idempotency-Key': idem }),
    tags: { name: 'create' },
  })
  createMs.add(createRes.timings.duration)

  const createOk = check(createRes, {
    // 400: local installment already reserved/ported under contention
    'create 202/400/409 contention-safe': (r) =>
      r.status === 202 || r.status === 400 || r.status === 409,
  })
  errorRate.add(!createOk)

  if (createRes.status === 202) {
    const operationId = createRes.json().operationId
    const statusRes = http.get(`${apiBase}/api/v1/operacoes-portabilidade/${operationId}`, {
      headers: headers(),
      tags: { name: 'status' },
    })
    statusMs.add(statusRes.timings.duration)
    const statusOk = check(statusRes, {
      'status 200': (r) => r.status === 200,
      'status known': (r) => {
        try {
          const s = r.json().status
          return s === 'PENDENTE' || s === 'CONFIRMADA' || s === 'REJEITADA'
        } catch {
          return false
        }
      },
    })
    errorRate.add(!statusOk)
  }

  sleep(0.5)
}
