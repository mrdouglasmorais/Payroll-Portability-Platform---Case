/**
 * Compatibility barrel. Prefer importing from `portabilityApiService` / `http`.
 */
export { isApiError } from './http'
export {
  isTerminal,
  portabilityApiService,
  portabilityApiService as api,
} from './portabilityApiService'
