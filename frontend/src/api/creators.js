// Module API — profil créateur
// Retirer USE_MOCK et le bloc mock quand le contrôleur /api/creators/me sera disponible (#70).

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

// ── Helpers ───────────────────────────────────────────────────────────────────

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// TODO: injecter le JWT Zitadel ici quand l'authentification sera en place (#10).
function authHeaders() {
  const token = null
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function handleResponse(res) {
  if (res.status === 404) return null
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

async function mockGet() {
  await delay(MOCK_DELAY_MS)
  return mockStore ? { ...mockStore } : null
}

async function mockCreate(data) {
  await delay(MOCK_DELAY_MS)
  if (!data.displayName?.trim()) throw { status: 400, message: 'Le nom affiché est obligatoire.' }
  if (mockStore) throw { status: 409, message: 'Un profil créateur existe déjà pour ce compte.' }
  mockStore = { userId: 1, photoUrl: null, validationStatus: 'VALIDATED', ...data }
  return { ...mockStore }
}

async function mockUpdate(data) {
  await delay(MOCK_DELAY_MS)
  if (!data.displayName?.trim()) throw { status: 400, message: 'Le nom affiché est obligatoire.' }
  if (!mockStore) throw { status: 404, message: 'Profil introuvable.' }
  mockStore = { ...mockStore, ...data }
  return { ...mockStore }
}

// ── API réelle ────────────────────────────────────────────────────────────────

async function fetchGet() {
  const res = await fetch('/api/creators/me', { headers: authHeaders() })
  return handleResponse(res)
}

async function fetchCreate(data) {
  const res = await fetch('/api/creators/me', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

async function fetchUpdate(data) {
  const res = await fetch('/api/creators/me', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

// ── Exports publics ───────────────────────────────────────────────────────────

/** Renvoie le profil courant, ou null si aucun profil n'existe (404). */
export function getMyProfile() {
  return USE_MOCK ? mockGet() : fetchGet()
}

/** Crée le profil. Lève { status: 409 } si un profil existe déjà, { status: 400 } si invalide. */
export function createMyProfile(data) {
  return USE_MOCK ? mockCreate(data) : fetchCreate(data)
}

/** Met à jour le profil. Lève { status: 400 } si invalide. */
export function updateMyProfile(data) {
  return USE_MOCK ? mockUpdate(data) : fetchUpdate(data)
}
