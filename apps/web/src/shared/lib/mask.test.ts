import { describe, expect, it } from 'vitest'
import { maskAccount } from './mask'

describe('maskAccount', () => {
  it('masks all but last four characters', () => {
    expect(maskAccount('56789-0')).toBe('****89-0')
  })

  it('fully masks short accounts', () => {
    expect(maskAccount('12')).toBe('****')
  })
})
