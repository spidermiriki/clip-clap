// Module API — galerie publique des créateurs
// Retirer USE_MOCK et le bloc mock quand le backend sera accessible sans proxy (#13).

import { readProblemMessage } from './problemDetail'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

// ── Helpers ───────────────────────────────────────────────────────────────────

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function handleResponse(res) {
  if (!res.ok) {
    const fallback = res.status === 400 ? 'Paramètres invalides.' : `Erreur serveur (${res.status}).`
    throw { status: res.status, message: await readProblemMessage(res, fallback) }
  }
  return res.json()
}

// ── Mock ──────────────────────────────────────────────────────────────────────

const MOCK_DELAY_MS = 300

const MOCK_CREATORS = [
  { userId: 1,  displayName: 'Alice Martin',      photoUrl: null,                               city: 'Bruxelles',  country: 'Belgique' },
  { userId: 2,  displayName: 'Bob Dupont',         photoUrl: null,                               city: 'Paris',      country: 'France'   },
  { userId: 3,  displayName: 'Clara Lecomte',      photoUrl: null,                               city: 'Mons',       country: 'Belgique' },
  { userId: 4,  displayName: 'David Renard',       photoUrl: null,                               city: 'Lyon',       country: 'France'   },
  { userId: 5,  displayName: 'Emma Fontaine',      photoUrl: null,                               city: 'Liège',      country: 'Belgique' },
  { userId: 6,  displayName: 'Fabrice Morin',      photoUrl: null,                               city: 'Toulouse',   country: 'France'   },
  { userId: 7,  displayName: 'Gisèle Noël',        photoUrl: null,                               city: 'Namur',      country: 'Belgique' },
  { userId: 8,  displayName: 'Hugo Petit',         photoUrl: null,                               city: 'Bordeaux',   country: 'France'   },
  { userId: 9,  displayName: 'Inès Lambert',       photoUrl: null,                               city: 'Charleroi',  country: 'Belgique' },
  { userId: 10, displayName: 'Julien Bernard',     photoUrl: null,                               city: 'Nantes',     country: 'France'   },
  { userId: 11, displayName: 'Karine Simon',       photoUrl: null,                               city: 'Gand',       country: 'Belgique' },
  { userId: 12, displayName: 'Lucas Moreau',       photoUrl: null,                               city: 'Lille',      country: 'France'   },
  { userId: 13, displayName: 'Marie Lefebvre',     photoUrl: null,                               city: 'Louvain',    country: 'Belgique' },
  { userId: 14, displayName: 'Nicolas Thomas',     photoUrl: null,                               city: 'Marseille',  country: 'France'   },
  { userId: 15, displayName: 'Olivia Richard',     photoUrl: null,                               city: 'Bruges',     country: 'Belgique' },
  { userId: 16, displayName: 'Pierre Garcia',      photoUrl: null,                               city: 'Nice',       country: 'France'   },
  { userId: 17, displayName: 'Quentin Robert',     photoUrl: null,                               city: 'Anvers',     country: 'Belgique' },
  { userId: 18, displayName: 'Rachel Martinez',    photoUrl: null,                               city: 'Rennes',     country: 'France'   },
  { userId: 19, displayName: 'Samuel Blanc',       photoUrl: null,                               city: 'Ixelles',    country: 'Belgique' },
  { userId: 20, displayName: 'Théa Leclerc',       photoUrl: null,                               city: 'Strasbourg', country: 'France'   },
  { userId: 21, displayName: 'Ulysse Bonnet',      photoUrl: null,                               city: 'Tournai',    country: 'Belgique' },
  { userId: 22, displayName: 'Véronique Durand',   photoUrl: null,                               city: 'Grenoble',   country: 'France'   },
  { userId: 23, displayName: 'William Girard',     photoUrl: null,                               city: 'Hasselt',    country: 'Belgique' },
  { userId: 24, displayName: 'Xuân Nguyen',        photoUrl: null,                               city: 'Montpellier',country: 'France'   },
  { userId: 25, displayName: 'Yasmine Dubois',     photoUrl: null,                               city: 'Arlon',      country: 'Belgique' },
]

// Pannes simulées à partir de la page 2, via l'URL :
//   ?mockErreur=reseau            → échec à la première tentative de chaque page, puis succès
//   ?mockErreur=reseau-permanent  → échec à chaque tentative
//   ?mockErreur=400               → 400 ProblemDetail, lu comme une vraie réponse du backend
function mockErreur() {
  return new URLSearchParams(window.location.search).get('mockErreur')
}

const mockPagesDejaEchouees = new Set()

function mockPanneReseau(page) {
  if (page < 1) return false
  const mode = mockErreur()
  if (mode === 'reseau-permanent') return true
  if (mode === 'reseau' && !mockPagesDejaEchouees.has(page)) {
    mockPagesDejaEchouees.add(page)
    return true
  }
  return false
}

// Mêmes types de prestation que data.sql ; chaque créateur fictif en reçoit un, en rotation
const MOCK_CATEGORIES = [
  { id: 1, name: 'Mariage' },
  { id: 2, name: 'Événementiel' },
  { id: 3, name: 'Sport' },
  { id: 4, name: 'Produit' },
  { id: 5, name: 'Clip' },
  { id: 6, name: 'Portrait' },
]

function mockCategoryIdOf(creator) {
  return ((creator.userId - 1) % MOCK_CATEGORIES.length) + 1
}

// Un créateur fictif sur quatre n'a pas encore de photo ; aucune vidéo avant la v0.2 (#94)
function mockMediaTypesOf(creator) {
  return creator.userId % 4 === 0 ? [] : ['PHOTO']
}

function normaliser(texte) {
  return (texte ?? '').trim().toLowerCase()
}

// Même règle que le backend : filtres combinés, ville et pays sans casse ni espaces autour
function mockCorrespond(creator, { city, country, categoryId, mediaType }) {
  if (normaliser(city) && normaliser(creator.city) !== normaliser(city)) return false
  if (normaliser(country) && normaliser(creator.country) !== normaliser(country)) return false
  if (categoryId && mockCategoryIdOf(creator) !== Number(categoryId)) return false
  if (mediaType && !mockMediaTypesOf(creator).includes(mediaType.toUpperCase())) return false
  return true
}

async function mockGetCreators({ page, size, ...filtres }) {
  await delay(MOCK_DELAY_MS)
  if (mockPanneReseau(page)) throw new TypeError('Failed to fetch')
  if (page >= 1 && mockErreur() === '400') {
    const problem = {
      type: 'about:blank',
      title: 'Bad Request',
      status: 400,
      detail: 'size doit être compris entre 1 et 50',
      instance: '/api/catalog/creators',
    }
    return handleResponse(new Response(JSON.stringify(problem), {
      status: 400,
      headers: { 'Content-Type': 'application/problem+json' },
    }))
  }
  if (page < 0) throw { status: 400, message: 'page doit être >= 0' }
  if (size < 1 || size > 50) throw { status: 400, message: 'size doit être compris entre 1 et 50' }
  const retenus = MOCK_CREATORS.filter((creator) => mockCorrespond(creator, filtres))
  const start = page * size
  const items = retenus.slice(start, start + size)
  return { items, page, size, totalElements: retenus.length }
}

async function mockGetCreatorProfile(userId) {
  await delay(MOCK_DELAY_MS)
  const creator = MOCK_CREATORS.find((c) => c.userId === Number(userId))
  // Même réponse que le backend pour un créateur inconnu, non validé ou suspendu
  if (!creator) throw { status: 404, message: `Aucun créateur public avec l'identifiant ${userId}` }
  return {
    ...creator,
    bio: `${creator.displayName} photographie mariages, événements et portraits.`,
    serviceAreas: `${creator.city} et environs`,
  }
}

async function mockGetCategories() {
  await delay(MOCK_DELAY_MS)
  return MOCK_CATEGORIES
}

// ── API réelle ────────────────────────────────────────────────────────────────

/** Paramètres de GET /api/catalog/creators ; les filtres vides ne sont pas envoyés. */
export function creatorsQuery({ page, size, city, country, categoryId, mediaType }) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (city?.trim()) params.set('city', city.trim())
  if (country?.trim()) params.set('country', country.trim())
  if (categoryId) params.set('categoryId', String(categoryId))
  if (mediaType) params.set('mediaType', mediaType)
  return params.toString()
}

async function fetchGetCreators(criteres) {
  const res = await fetch(`/api/catalog/creators?${creatorsQuery(criteres)}`)
  return handleResponse(res)
}

async function fetchGetCreatorProfile(userId) {
  const res = await fetch(`/api/catalog/creators/${encodeURIComponent(userId)}`)
  return handleResponse(res)
}

async function fetchGetCategories() {
  const res = await fetch('/api/catalog/categories')
  return handleResponse(res)
}

// ── Exports publics ───────────────────────────────────────────────────────────

/**
 * Renvoie une page de créateurs validés ({ items, page, size, totalElements }),
 * filtrée par ville, pays, type de prestation (categoryId) et type de média (PHOTO ou VIDEO) s'ils sont renseignés.
 */
export function getCreators(criteres) {
  return USE_MOCK ? mockGetCreators(criteres) : fetchGetCreators(criteres)
}

/**
 * Renvoie le profil public d'un créateur (userId, displayName, photoUrl, bio, city, country, serviceAreas).
 * Lève { status: 404 } s'il est inconnu, non validé ou suspendu.
 */
export function getCreatorProfile(userId) {
  return USE_MOCK ? mockGetCreatorProfile(userId) : fetchGetCreatorProfile(userId)
}

/** Renvoie les types de prestation ([{ id, name }]) proposés comme filtre. */
export function getCategories() {
  return USE_MOCK ? mockGetCategories() : fetchGetCategories()
}
