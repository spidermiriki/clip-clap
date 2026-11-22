import { afterEach, describe, expect, it, vi } from 'vitest'
import { updateMyCategories } from './creators'

function reponse(status, body) {
  return { ok: status < 400, status, json: async () => body }
}

describe('updateMyCategories (#87, revue de #122)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renvoie les identifiants enregistrés', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => reponse(200, [1, 3])))

    await expect(updateMyCategories([3, 1], 'jeton')).resolves.toEqual([1, 3])
  })

  it('lève une erreur au lieu de renvoyer null sur un 404 « Profil introuvable »', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => reponse(404, { title: 'Profil introuvable' })))

    await expect(updateMyCategories([1], 'jeton')).rejects.toMatchObject({ status: 404 })
  })
})
