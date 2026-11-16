import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiUrl } from './baseUrl'

afterEach(() => {
  vi.unstubAllEnvs()
})

describe('apiUrl (#106)', () => {
  it('garde le chemin relatif quand VITE_API_BASE_URL est vide (proxy Vite en dev)', () => {
    vi.stubEnv('VITE_API_BASE_URL', '')

    expect(apiUrl('/api/catalog/categories')).toBe('/api/catalog/categories')
  })

  it("préfixe le chemin par l'URL du backend en production", () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://clip-clap.onrender.com')

    expect(apiUrl('/api/users/me')).toBe('https://clip-clap.onrender.com/api/users/me')
  })

  it('tolère un slash final dans la variable', () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://clip-clap.onrender.com/')

    expect(apiUrl('/api/users/me')).toBe('https://clip-clap.onrender.com/api/users/me')
  })
})
