// Module API — albums du créateur connecté (#15).
// Upload d'une photo en trois temps : signature demandée au backend → envoi direct à
// Cloudinary → réponse de Cloudinary renvoyée au backend, qui vérifie sa signature.

import { readProblemMessage } from './problemDetail'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export const FORMATS_ACCEPTES = ['image/jpeg', 'image/png', 'image/webp']
export const TAILLE_MAX_OCTETS = 10 * 1024 * 1024
export const MESSAGE_ECHEC_ENVOI = "L'envoi de la photo a échoué. Réessayez."

/** Message d'erreur si le fichier ne peut pas être envoyé, sinon null. */
export function verifierFichier(file) {
  if (!FORMATS_ACCEPTES.includes(file.type)) return `« ${file.name} » : seuls les formats JPEG, PNG et WebP sont acceptés.`
  if (file.size > TAILLE_MAX_OCTETS) return `« ${file.name} » dépasse 10 Mo.`
  return null
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function authHeaders(token) {
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function handleResponse(res, fallback) {
  if (!res.ok) throw { status: res.status, message: await readProblemMessage(res, fallback) }
  return res.status === 204 ? null : res.json()
}

// ── API réelle ────────────────────────────────────────────────────────────────

async function fetchListMyAlbums(token) {
  const res = await fetch('/api/creators/me/albums', { headers: authHeaders(token) })
  return handleResponse(res, 'Impossible de charger vos albums.')
}

async function fetchCreateAlbum(name, token) {
  const res = await fetch('/api/creators/me/albums', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders(token) },
    body: JSON.stringify({ name }),
  })
  return handleResponse(res, "Impossible de créer l'album.")
}

async function fetchRenameAlbum(albumId, name, token) {
  const res = await fetch(`/api/creators/me/albums/${albumId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders(token) },
    body: JSON.stringify({ name }),
  })
  return handleResponse(res, "Impossible de renommer l'album.")
}

async function fetchUploadPhoto(albumId, file, token) {
  // 1. Signature : dossier et formats imposés par le backend, le secret ne quitte jamais le serveur
  const signature = await handleResponse(
    await fetch(`/api/creators/me/albums/${albumId}/upload-signature`, { method: 'POST', headers: authHeaders(token) }),
    "Impossible de préparer l'envoi de la photo.",
  )

  // 2. Envoi direct du fichier à Cloudinary, avec exactement les paramètres signés
  const form = new FormData()
  form.append('file', file)
  form.append('api_key', signature.apiKey)
  form.append('timestamp', String(signature.timestamp))
  form.append('signature', signature.signature)
  form.append('folder', signature.folder)
  form.append('allowed_formats', signature.allowedFormats)
  // Le détail technique de Cloudinary (ex. « Invalid Signature … String to sign … ») reste dans la console
  let cloudinaryRes
  try {
    cloudinaryRes = await fetch(
      `https://api.cloudinary.com/v1_1/${encodeURIComponent(signature.cloudName)}/image/upload`,
      { method: 'POST', body: form },
    )
  } catch (err) {
    console.error('Envoi à Cloudinary impossible :', err)
    throw { status: 0, message: MESSAGE_ECHEC_ENVOI }
  }
  if (!cloudinaryRes.ok) {
    const body = await cloudinaryRes.json().catch(() => null)
    console.error(`Cloudinary a refusé la photo (${cloudinaryRes.status}) :`, body?.error?.message ?? body)
    throw { status: cloudinaryRes.status, message: MESSAGE_ECHEC_ENVOI }
  }
  const upload = await cloudinaryRes.json()

  // 3. Enregistrement : le backend vérifie la signature de Cloudinary avant d'accepter la photo
  const res = await fetch(`/api/creators/me/albums/${albumId}/photos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders(token) },
    body: JSON.stringify({
      publicId: upload.public_id,
      version: String(upload.version),
      signature: upload.signature,
      secureUrl: upload.secure_url,
    }),
  })
  return handleResponse(res, "La photo a été envoyée mais n'a pas pu être enregistrée.")
}

// ── Mock (VITE_USE_MOCK=true) : aucun appel au backend ni à Cloudinary ────────

const MOCK_DELAY_MS = 300
let mockAlbums = []
let mockNextId = 1

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function mockValidName(name) {
  const nom = (name ?? '').trim()
  if (!nom) throw { status: 400, message: "Le nom de l'album est obligatoire" }
  return nom
}

async function mockListMyAlbums() {
  await delay(MOCK_DELAY_MS)
  return structuredClone(mockAlbums)
}

async function mockCreateAlbum(name) {
  await delay(MOCK_DELAY_MS)
  const album = { id: mockNextId++, name: mockValidName(name), photos: [] }
  mockAlbums.push(album)
  return structuredClone(album)
}

async function mockRenameAlbum(albumId, name) {
  await delay(MOCK_DELAY_MS)
  const album = mockAlbums.find((a) => a.id === albumId)
  if (!album) throw { status: 404, message: 'Album introuvable' }
  album.name = mockValidName(name)
  return null
}

async function mockUploadPhoto(albumId, file) {
  const erreur = verifierFichier(file)
  if (erreur) throw { status: 400, message: erreur }
  await delay(MOCK_DELAY_MS)
  const album = mockAlbums.find((a) => a.id === albumId)
  if (!album) throw { status: 404, message: 'Album introuvable' }
  const photo = { id: mockNextId++, url: URL.createObjectURL(file) }
  album.photos.push(photo)
  return photo
}

// ── Exports publics ───────────────────────────────────────────────────────────

/** Albums du créateur connecté ([{ id, name, photos: [{ id, url }] }]). */
export function listMyAlbums(token) {
  return USE_MOCK ? mockListMyAlbums() : fetchListMyAlbums(token)
}

/** Crée un album ; lève { status: 400 } si le nom est vide, { status: 404 } sans profil créateur. */
export function createAlbum(name, token) {
  return USE_MOCK ? mockCreateAlbum(name) : fetchCreateAlbum(name, token)
}

export function renameAlbum(albumId, name, token) {
  return USE_MOCK ? mockRenameAlbum(albumId, name) : fetchRenameAlbum(albumId, name, token)
}

/** Envoie une photo dans l'album et renvoie { id, url } une fois enregistrée. */
export function uploadPhoto(albumId, file, token) {
  const erreur = verifierFichier(file)
  if (erreur) return Promise.reject({ status: 400, message: erreur })
  return USE_MOCK ? mockUploadPhoto(albumId, file) : fetchUploadPhoto(albumId, file, token)
}
