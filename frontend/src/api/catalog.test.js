import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest'

// USE_MOCK est lu à l'import : on active le mock puis on importe un module neuf
let catalog

beforeAll(async () => {
  vi.stubEnv('VITE_USE_MOCK', 'true')
  vi.resetModules()
  catalog = await import('./catalog')
})

afterAll(() => {
  vi.unstubAllEnvs()
})

describe('mock de catalog.js — filtres (#14)', () => {
  it('filtre par pays sans tenir compte de la casse', async () => {
    const page = await catalog.getCreators({ page: 0, size: 50, country: 'BELGIQUE' })

    expect(page.totalElements).toBe(13)
    expect(page.items.every((c) => c.country === 'Belgique')).toBe(true)
  })

  it('filtre par ville en ignorant les espaces autour', async () => {
    const page = await catalog.getCreators({ page: 0, size: 50, city: ' mons ' })

    expect(page.items.map((c) => c.displayName)).toEqual(['Clara Lecomte'])
  })

  it('combine type de prestation et pays, et pagine le résultat filtré', async () => {
    // Type 1 = créateurs 1, 7, 13, 19, 25, tous en Belgique ; le 2 (France) est d'un autre type
    const premiere = await catalog.getCreators({ page: 0, size: 2, country: 'Belgique', categoryId: '1' })
    const derniere = await catalog.getCreators({ page: 2, size: 2, country: 'Belgique', categoryId: '1' })
    const aucun = await catalog.getCreators({ page: 0, size: 2, country: 'France', categoryId: '1' })

    expect(premiere.totalElements).toBe(5)
    expect(premiere.items).toHaveLength(2)
    expect(derniere.items).toHaveLength(1)
    expect(aucun.totalElements).toBe(0)
  })

  it('propose les mêmes types de prestation que data.sql', async () => {
    const categories = await catalog.getCategories()

    expect(categories.map((c) => c.name)).toEqual(['Mariage', 'Événementiel', 'Sport', 'Produit', 'Clip', 'Portrait'])
  })
})
