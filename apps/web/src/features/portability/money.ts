export function parseMoneyAmount(value: number | string): number | null {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null
  }
  const amount = Number(value)
  return Number.isFinite(amount) ? amount : null
}

export function formatMoney(value: number | string): string {
  const amount = parseMoneyAmount(value)
  if (amount == null) {
    return '—'
  }
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(amount)
}

export function sumMoney(values: Array<number | string>): number | null {
  let total = 0
  for (const value of values) {
    const amount = parseMoneyAmount(value)
    if (amount == null) {
      return null
    }
    total += amount
  }
  return total
}
