import { describe, expect, it } from 'vitest'
import { isTerminal } from './client'

describe('isTerminal', () => {
  it('stops polling for confirmed and rejected', () => {
    expect(isTerminal('CONFIRMADA')).toBe(true)
    expect(isTerminal('REJEITADA')).toBe(true)
    expect(isTerminal('PENDENTE')).toBe(false)
    expect(isTerminal(undefined)).toBe(false)
  })
})
