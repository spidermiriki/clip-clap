// Module API — prestations à prix fixe du créateur connecté (#27)

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
  return res.status === 204 ? null : res.json()
}

export async function listMyOfferings(token) {
  const res = await fetch(apiUrl('/api/offerings/me'), { headers: headers(token) })
  return handleResponse(res, 'Impossible de charger vos prestations.')
}

/** offering : { name, durationMinutes, basePrice, options: [{ name, extraPrice }] } */
export async function createOffering(offering, token) {
  const res = await fetch(apiUrl('/api/offerings/me'), {
    method: 'POST',
    headers: headers(token, true),
    body: JSON.stringify(offering),
  })
  return handleResponse(res, "Impossible d'enregistrer la prestation.")
}

export async function updateOffering(offeringId, offering, token) {
  const res = await fetch(apiUrl(`/api/offerings/me/${offeringId}`), {
    method: 'PUT',
    headers: headers(token, true),
    body: JSON.stringify(offering),
  })
  return handleResponse(res, "Impossible d'enregistrer la prestation.")
}

export async function deleteOffering(offeringId, token) {
  const res = await fetch(apiUrl(`/api/offerings/me/${offeringId}`), { method: 'DELETE', headers: headers(token) })
  return handleResponse(res, 'Impossible de supprimer la prestation.')
}
