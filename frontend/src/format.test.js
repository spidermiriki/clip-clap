import { describe, expect, it } from 'vitest'
import { formatCreneau, formatDuree, formatPrix, parsePrix } from './format'

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

  it('lit un prix avec virgule décimale et séparateurs de milliers (revue de #119)', () => {
    expect(parsePrix('1200')).toBe(1200)
    expect(parsePrix('89,90')).toBe(89.9)
    expect(parsePrix('89.90')).toBe(89.9)
    expect(parsePrix('1.200,00')).toBe(1200)
    expect(parsePrix('1 200,50')).toBe(1200.5)
    expect(parsePrix('1 200,50')).toBe(1200.5)
    expect(parsePrix('1,200.50')).toBe(1200.5)
    expect(parsePrix('1.200.000')).toBe(1200000)
  })

  it('refuse un prix illisible plutôt que de deviner', () => {
    for (const saisie of ['', 'abc', '12,5,3', '-10', '10,555', '1,2.3,4']) {
      expect(parsePrix(saisie)).toBeNull()
    }
  })

  it("affiche un créneau à l'heure de Bruxelles, quel que soit le décalage reçu (#32)", () => {
    expect(formatCreneau('2027-04-02T14:00:00+02:00', '2027-04-02T22:00:00+02:00'))
      .toBe('vendredi 2 avril 2027, 14:00 – 22:00')
    expect(formatCreneau('2027-04-02T12:00:00Z', '2027-04-02T13:30:00Z'))
      .toBe('vendredi 2 avril 2027, 14:00 – 15:30')
  })
})
