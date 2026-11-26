export function maskAccount(account: string): string {
  const trimmed = account.trim()
  if (trimmed.length <= 4) {
    return '****'
  }
  return `****${trimmed.slice(-4)}`
}
