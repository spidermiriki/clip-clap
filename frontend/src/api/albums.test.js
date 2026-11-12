import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { uploadPhoto, verifierFichier } from './albums'

// fetch est simulé : aucun appel réel au backend ni à Cloudinary
function reponse(status, body) {
  return { ok: status >= 200 && status < 300, status, json: async () => body }
}

const SIGNATURE = {
  cloudName: 'cloud-test',
  apiKey: 'cle-test',
  timestamp: 1731400000,
  signature: 'sig-backend',
  folder: 'clicclap/creators/1/albums/7',
  allowedFormats: 'jpg,png,webp',
}

const photoJpeg = () => new File(['x'], 'mariage.jpg', { type: 'image/jpeg' })

describe('uploadPhoto (#15)', () => {
  let fetchMock

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('enchaîne signature, envoi direct à Cloudinary puis enregistrement vérifié', async () => {
    fetchMock
      .mockResolvedValueOnce(reponse(200, SIGNATURE))
      .mockResolvedValueOnce(reponse(200, {
        public_id: 'clicclap/creators/1/albums/7/abc',
        version: 1731400123,
        signature: 'sig-cloudinary',
        secure_url: 'https://res.cloudinary.com/cloud-test/image/upload/v1731400123/clicclap/creators/1/albums/7/abc.jpg',
      }))
      .mockResolvedValueOnce(reponse(201, { id: 5, url: 'https://res.cloudinary.com/cloud-test/image/upload/v1731400123/clicclap/creators/1/albums/7/abc.jpg' }))

    const photo = await uploadPhoto(7, photoJpeg(), 'jeton')

    expect(photo.id).toBe(5)

    const [urlSignature, optionsSignature] = fetchMock.mock.calls[0]
    expect(urlSignature).toBe('/api/creators/me/albums/7/upload-signature')
    expect(optionsSignature.headers.Authorization).toBe('Bearer jeton')

    const [urlCloudinary, optionsCloudinary] = fetchMock.mock.calls[1]
    expect(urlCloudinary).toBe('https://api.cloudinary.com/v1_1/cloud-test/image/upload')
    expect(optionsCloudinary.headers).toBeUndefined()
    const form = optionsCloudinary.body
    expect(form.get('api_key')).toBe('cle-test')
    expect(form.get('timestamp')).toBe('1731400000')
    expect(form.get('signature')).toBe('sig-backend')
    expect(form.get('folder')).toBe('clicclap/creators/1/albums/7')
    expect(form.get('allowed_formats')).toBe('jpg,png,webp')

    const [urlEnregistrement, optionsEnregistrement] = fetchMock.mock.calls[2]
    expect(urlEnregistrement).toBe('/api/creators/me/albums/7/photos')
    expect(JSON.parse(optionsEnregistrement.body)).toEqual({
      publicId: 'clicclap/creators/1/albums/7/abc',
      version: '1731400123',
      signature: 'sig-cloudinary',
      secureUrl: 'https://res.cloudinary.com/cloud-test/image/upload/v1731400123/clicclap/creators/1/albums/7/abc.jpg',
    })
  })

  it('refuse un format non accepté avant tout appel réseau', async () => {
    const gif = new File(['x'], 'anim.gif', { type: 'image/gif' })

    await expect(uploadPhoto(7, gif, 'jeton')).rejects.toMatchObject({ status: 400 })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('remonte le message de Cloudinary et n\'enregistre rien si Cloudinary refuse', async () => {
    fetchMock
      .mockResolvedValueOnce(reponse(200, SIGNATURE))
      .mockResolvedValueOnce(reponse(400, { error: { message: 'Invalid Signature' } }))

    await expect(uploadPhoto(7, photoJpeg(), 'jeton'))
      .rejects.toMatchObject({ message: 'Cloudinary a refusé la photo : Invalid Signature' })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('remonte le detail du backend si l\'enregistrement est refusé', async () => {
    fetchMock
      .mockResolvedValueOnce(reponse(200, SIGNATURE))
      .mockResolvedValueOnce(reponse(200, { public_id: 'p', version: 1, signature: 's', secure_url: 'u' }))
      .mockResolvedValueOnce(reponse(400, { title: 'Upload refusé', detail: 'La signature de Cloudinary est invalide' }))

    await expect(uploadPhoto(7, photoJpeg(), 'jeton'))
      .rejects.toMatchObject({ status: 400, message: 'La signature de Cloudinary est invalide' })
  })
})

describe('verifierFichier', () => {
  it('accepte JPEG, PNG et WebP jusqu\'à 10 Mo', () => {
    expect(verifierFichier(photoJpeg())).toBeNull()
    const lourd = new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'lourd.png', { type: 'image/png' })
    expect(verifierFichier(lourd)).toContain('10 Mo')
  })
})
