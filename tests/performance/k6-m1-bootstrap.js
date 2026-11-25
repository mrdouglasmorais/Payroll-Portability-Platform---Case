import http from 'k6/http'
import { check, sleep } from 'k6'
import { Rate, Trend } from 'k6/metrics'

/**
 * M1 bootstrap load test — health/status surfaces only.
 * Domain endpoints arrive in later milestones; do not invent SLOs.
 *
 * Run (stack must be up on the host):
 *   docker run --rm -i --network host \
 *     -e API_BASE=http://localhost:8080 \
 *     -e CENTRAL_BASE=http://localhost:8081 \
 *     -e WEB_BASE=http://localhost:5173 \
 *     grafana/k6:0.54.0 run - < tests/performance/k6-m1-bootstrap.js
 */

const apiBase = __ENV.API_BASE || 'http://localhost:8080'
const centralBase = __ENV.CENTRAL_BASE || 'http://localhost:8081'
const webBase = __ENV.WEB_BASE || 'http://localhost:5173'

const errorRate = new Rate('errors')
const apiHealthLatency = new Trend('api_health_ms', true)
const apiStatusLatency = new Trend('api_status_ms', true)
const centralHealthLatency = new Trend('central_health_ms', true)
const webLatency = new Trend('web_ms', true)

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 5,
      duration: '30s',
      startTime: '0s',
      tags: { scenario: 'smoke' },
    },
    ramp: {
      executor: 'ramping-vus',
      startTime: '35s',
      stages: [
        { duration: '20s', target: 20 },
        { duration: '40s', target: 20 },
        { duration: '15s', target: 0 },
      ],
      tags: { scenario: 'ramp' },
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    errors: ['rate<0.01'],
    api_health_ms: ['p(95)<500'],
    api_status_ms: ['p(95)<500'],
    central_health_ms: ['p(95)<500'],
    web_ms: ['p(95)<1000'],
  },
}

function probe(name, url, latencyMetric, expectJsonStatus) {
  const res = http.get(url, { tags: { name } })
  latencyMetric.add(res.timings.duration)

  const ok = check(res, {
    [`${name} status 200`]: (r) => r.status === 200,
    ...(expectJsonStatus
      ? {
          [`${name} body UP`]: (r) => {
            try {
              const body = r.json()
              return body && (body.status === 'UP' || body.service)
            } catch {
              return false
            }
          },
        }
      : {
          [`${name} has body`]: (r) => r.body && r.body.length > 0,
        }),
  })

  errorRate.add(!ok)
  return ok
}

export default function () {
  probe('api_health', `${apiBase}/actuator/health`, apiHealthLatency, true)
  probe('api_status', `${apiBase}/api/v1/status`, apiStatusLatency, true)
  probe('central_health', `${centralBase}/actuator/health`, centralHealthLatency, true)
  probe('web_home', `${webBase}/`, webLatency, false)
  sleep(0.5)
}
