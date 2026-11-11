// Module API — galerie publique des créateurs
// Retirer USE_MOCK et le bloc mock quand le backend sera accessible sans proxy (#13).

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

// ── Helpers ───────────────────────────────────────────────────────────────────

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function handleResponse(res) {
  if (res.status === 400) {
    const body = await res.text().catch(() => '')
    throw { status: 400, message: body || 'Paramètres invalides.' }
  }
  if (!res.ok) throw { status: res.status, message: `Erreur serveur (${res.status}).` }
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

async function mockGetCreators({ page, size }) {
  await delay(MOCK_DELAY_MS)
  if (mockPanneReseau(page)) throw new TypeError('Failed to fetch')
  if (page < 0) throw { status: 400, message: 'page doit être >= 0' }
  if (size < 1 || size > 50) throw { status: 400, message: 'size doit être compris entre 1 et 50' }
  const start = page * size
  const items = MOCK_CREATORS.slice(start, start + size)
  return { items, page, size, totalElements: MOCK_CREATORS.length }
}

// ── API réelle ────────────────────────────────────────────────────────────────

async function fetchGetCreators({ page, size }) {
  const res = await fetch(`/api/catalog/creators?page=${page}&size=${size}`)
  return handleResponse(res)
}

// ── Exports publics ───────────────────────────────────────────────────────────

/** Renvoie une page de créateurs validés ({ items, page, size, totalElements }). */
export function getCreators({ page, size }) {
  return USE_MOCK ? mockGetCreators({ page, size }) : fetchGetCreators({ page, size })
}
