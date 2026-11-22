// Module API — profil créateur
// Retirer USE_MOCK et le bloc mock quand le contrôleur /api/creators/me sera disponible (#70).

import { apiUrl } from './baseUrl'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

// ── Helpers ───────────────────────────────────────────────────────────────────

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function authHeaders(token) {
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function handleResponse(res) {
  if (res.status === 404) {
    const body = await res.json().catch(() => ({}))
    if (body.title === 'Profil introuvable') return null
    throw { status: 404, message: body.detail ?? 'Utilisateur inconnu.' }
  }
  if (res.status === 409) {
    const body = await res.json().catch(() => ({}))
    throw { status: 409, message: body.detail ?? 'Un profil créateur existe déjà pour ce compte.' }
  }
  if (res.status === 400) {
    const body = await res.json().catch(() => ({}))
    throw { status: 400, message: body.detail ?? 'Données invalides.' }
  }
  if (!res.ok) throw { status: res.status, message: `Erreur serveur (${res.status}).` }
  return res.json()
}

// ── Mock ──────────────────────────────────────────────────────────────────────

const MOCK_DELAY_MS = 300

let mockStore = null
let mockCategoryIds = []

async function mockGet() {
  await delay(MOCK_DELAY_MS)
  return mockStore ? { ...mockStore } : null
}

async function mockCreate(data) {
  await delay(MOCK_DELAY_MS)
  if (!data.displayName?.trim()) throw { status: 400, message: 'Le nom affiché est obligatoire.' }
  if (mockStore) throw { status: 409, message: 'Un profil créateur existe déjà pour ce compte.' }
  mockStore = { userId: 1, photoUrl: null, validationStatus: 'PENDING', rejectionReason: null, ...data }
  return { ...mockStore }
}

async function mockUpdate(data) {
  await delay(MOCK_DELAY_MS)
  if (!data.displayName?.trim()) throw { status: 400, message: 'Le nom affiché est obligatoire.' }
  if (!mockStore) throw { status: 404, message: 'Profil introuvable.' }
  mockStore = { ...mockStore, ...data }
  return { ...mockStore }
}

async function mockGetCategories() {
  await delay(MOCK_DELAY_MS)
  return [...mockCategoryIds]
}

async function mockUpdateCategories(categoryIds) {
  await delay(MOCK_DELAY_MS)
  mockCategoryIds = [...new Set(categoryIds)].sort((a, b) => a - b)
  return [...mockCategoryIds]
}

// ── API réelle ────────────────────────────────────────────────────────────────

async function fetchGet(token) {
  const res = await fetch(apiUrl('/api/creators/me'), { headers: authHeaders(token) })
  return handleResponse(res)
}

async function fetchCreate(data, token) {
  const res = await fetch(apiUrl('/api/creators/me'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders(token) },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

async function fetchUpdate(data, token) {
  const res = await fetch(apiUrl('/api/creators/me'), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders(token) },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

async function fetchGetCategories(token) {
  const res = await fetch(apiUrl('/api/creators/me/categories'), { headers: authHeaders(token) })
  return (await handleResponse(res)) ?? []
}

async function fetchUpdateCategories(categoryIds, token) {
  const res = await fetch(apiUrl('/api/creators/me/categories'), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders(token) },
    body: JSON.stringify({ categoryIds }),
  })
  return handleResponse(res)
}

// ── Exports publics ───────────────────────────────────────────────────────────

/** Renvoie le profil courant, ou null si aucun profil n'existe (404 "Profil introuvable"). */
export function getMyProfile(accessToken = null) {
  return USE_MOCK ? mockGet() : fetchGet(accessToken)
}

/** Crée le profil. Lève { status: 409 } si un profil existe déjà, { status: 400 } si invalide. */
export function createMyProfile(data, accessToken = null) {
  return USE_MOCK ? mockCreate(data) : fetchCreate(data, accessToken)
}

/** Met à jour le profil. Lève { status: 400 } si invalide. */
export function updateMyProfile(data, accessToken = null) {
  return USE_MOCK ? mockUpdate(data) : fetchUpdate(data, accessToken)
}

/** Identifiants des types de prestation choisis (#87) ; [] si le profil n'existe pas encore. */
export function getMyCategories(accessToken = null) {
  return USE_MOCK ? mockGetCategories() : fetchGetCategories(accessToken)
}

/** Remplace les types de prestation choisis. Lève { status: 400 } pour une catégorie inconnue. */
export function updateMyCategories(categoryIds, accessToken = null) {
  return USE_MOCK ? mockUpdateCategories(categoryIds) : fetchUpdateCategories(categoryIds, accessToken)
}
