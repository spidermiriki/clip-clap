import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getCreatorProfile } from '../api/catalog'
import ProfilPublic from './ProfilPublic'

vi.mock('../api/catalog', () => ({ getCreatorProfile: vi.fn(), getCategories: vi.fn(async () => [{ id: 1, name: 'Mariage' }]) }))

const PROFIL = {
  userId: 3,
  displayName: 'Clara Lecomte',
  photoUrl: null,
  bio: 'Photographe de mariage depuis dix ans.',
  city: 'Mons',
  country: 'Belgique',
  serviceAreas: 'Hainaut, Bruxelles',
}

function renderProfil(url) {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Routes>
        <Route path="/" element={<p>Galerie</p>} />
        <Route path="/createurs/:userId" element={<ProfilPublic />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProfilPublic (#17)', () => {
  beforeEach(() => {
    vi.mocked(getCreatorProfile).mockReset()
  })

  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(cleanup)

  it('affiche nom, ville, pays, bio et zones d\'intervention', async () => {
    vi.mocked(getCreatorProfile).mockResolvedValue(PROFIL)
    renderProfil('/createurs/3')

    expect(await screen.findByRole('heading', { level: 1, name: 'Clara Lecomte' })).toBeInTheDocument()
    expect(getCreatorProfile).toHaveBeenCalledWith('3')
    expect(screen.getByText('Mons, Belgique')).toBeInTheDocument()
    expect(screen.getByText('Photographe de mariage depuis dix ans.')).toBeInTheDocument()
    expect(screen.getByText('Hainaut, Bruxelles')).toBeInTheDocument()
  })

  it('propose un lien « Réserver » vers la page de réservation (#29)', async () => {
    vi.mocked(getCreatorProfile).mockResolvedValue(PROFIL)
    renderProfil('/createurs/3')

    expect(await screen.findByRole('link', { name: 'Réserver' })).toHaveAttribute('href', '/createurs/3/reserver')
  })

  it('affiche les albums et leurs photos (#15)', async () => {
    vi.mocked(getCreatorProfile).mockResolvedValue({
      ...PROFIL,
      albums: [{ id: 1, name: 'Mariages', photos: [{ id: 10, url: 'https://exemple.test/1.jpg' }, { id: 11, url: 'https://exemple.test/2.jpg' }] }],
    })
    renderProfil('/createurs/3')

    expect(await screen.findByRole('heading', { level: 3, name: 'Mariages' })).toBeInTheDocument()
    expect(screen.getByAltText('Mariages, photo 2')).toHaveAttribute('src', 'https://exemple.test/2.jpg')
  })

  it('affiche les prestations avec durée, prix et options (#27, scénarios 10 et 11)', async () => {
    vi.mocked(getCreatorProfile).mockResolvedValue({
      ...PROFIL,
      offerings: [{
        id: 5,
        name: 'Reportage mariage',
        durationMinutes: 480,
        basePrice: 1200,
        options: [{ id: 9, name: 'Drone', extraPrice: 150 }],
      }],
    })
    renderProfil('/createurs/3')

    expect(await screen.findByRole('heading', { level: 3, name: 'Reportage mariage' })).toBeInTheDocument()
    expect(screen.getByText(/^8 h · 1\s?200,00\s€$/)).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Options de Reportage mariage' })).toHaveTextContent(/Drone : \+ 150,00\s€/)
  })

  it("affiche la catégorie d'une prestation (#123)", async () => {
    vi.mocked(getCreatorProfile).mockResolvedValue({
      ...PROFIL,
      offerings: [{ id: 5, name: 'Reportage mariage', durationMinutes: 480, basePrice: 1200, categoryId: 1, options: [] }],
    })
    renderProfil('/createurs/3')

    expect(await screen.findByText(/^Mariage · 8 h · 1\s?200,00\s€$/)).toBeInTheDocument()
  })

  it('indique l\'absence de prestation', async () => {
    vi.mocked(getCreatorProfile).mockResolvedValue({ ...PROFIL, offerings: [] })
    renderProfil('/createurs/3')

    expect(await screen.findByText('Aucune prestation pour le moment.')).toBeInTheDocument()
  })

  it('annonce un créateur introuvable (404) sans proposer de réessayer', async () => {
    vi.mocked(getCreatorProfile).mockRejectedValue({ status: 404, message: 'Aucun créateur public' })
    renderProfil('/createurs/999')

    expect(await screen.findByRole('alert')).toHaveTextContent("Ce créateur n'existe pas ou n'est plus visible.")
    expect(screen.queryByRole('button', { name: 'Réessayer' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Retour à la galerie' })).toHaveAttribute('href', '/')
  })

  it('propose de réessayer après une panne réseau', async () => {
    vi.mocked(getCreatorProfile)
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce(PROFIL)
    renderProfil('/createurs/3')

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de charger ce profil.')
    fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Clara Lecomte' })).toBeInTheDocument()
  })

  it('affiche une vidéo avec un lecteur (#94)', async () => {
    vi.mocked(getCreatorProfile).mockResolvedValue({
      ...PROFIL,
      albums: [{ id: 2, name: 'Clips musicaux', photos: [{ id: 20, url: 'https://exemple.test/clip.mp4', mediaType: 'VIDEO' }] }],
    })
    renderProfil('/createurs/3')

    const lecteur = await screen.findByLabelText('Clips musicaux, vidéo 1')
    expect(lecteur.tagName).toBe('VIDEO')
    expect(lecteur).toHaveAttribute('controls')
  })
})
