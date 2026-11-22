// Module API — back-office administrateur : validation des comptes créateurs (#33)

import { apiUrl } from './baseUrl'
import { readProblemMessage } from './problemDetail'

function headers(token, json = false) {
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(json ? { 'Content-Type': 'application/json' } : {}),
  }
}

async function handleResponse(res, fallback) {
  if (!res.ok) throw { status: res.status, message: await readProblemMessage(res, fallback) }
  return res.json()
}

export async function listPendingCreators(token) {
  const res = await fetch(apiUrl('/api/admin/creators?status=PENDING'), { headers: headers(token) })
  return handleResponse(res, 'Impossible de charger les comptes en attente.')
}

export async function validateCreator(userId, token) {
  const res = await fetch(apiUrl(`/api/admin/creators/${userId}/validate`), { method: 'POST', headers: headers(token) })
  return handleResponse(res, 'Impossible de valider ce compte.')
}

export async function rejectCreator(userId, reason, token) {
  const res = await fetch(apiUrl(`/api/admin/creators/${userId}/reject`), {
    method: 'POST',
    headers: headers(token, true),
    body: JSON.stringify({ reason }),
  })
  return handleResponse(res, 'Impossible de refuser ce compte.')
}
