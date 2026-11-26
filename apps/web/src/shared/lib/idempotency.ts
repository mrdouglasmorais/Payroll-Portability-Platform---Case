export function createIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return `web-${crypto.randomUUID()}`
  }
  return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
