import { describe, expect, it } from 'vitest'
import { formatDuree, formatPrix } from './format'

describe('formats (#27)', () => {
  it('affiche une durée en heures et minutes', () => {
    expect(formatDuree(45)).toBe('45 min')
    expect(formatDuree(60)).toBe('1 h')
    expect(formatDuree(90)).toBe('1 h 30')
    expect(formatDuree(485)).toBe('8 h 05')
  })

  it('affiche un prix en euros, deux décimales', () => {
    expect(formatPrix(1200)).toMatch(/^1\s?200,00\s€$/)
    expect(formatPrix('89.9')).toMatch(/^89,90\s€$/)
  })
})
