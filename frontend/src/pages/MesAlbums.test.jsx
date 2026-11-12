import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createAlbum, listMyAlbums, renameAlbum, uploadPhoto } from '../api/albums'
import MesAlbums from './MesAlbums'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'jeton' } }) }))
vi.mock('../api/albums', async (importOriginal) => ({
  ...(await importOriginal()),
  listMyAlbums: vi.fn(),
  createAlbum: vi.fn(),
  renameAlbum: vi.fn(),
  uploadPhoto: vi.fn(),
}))

function renderPage() {
  return render(<MemoryRouter><MesAlbums /></MemoryRouter>)
}

describe('MesAlbums (#15)', () => {
  beforeEach(() => {
    vi.mocked(listMyAlbums).mockReset().mockResolvedValue([])
    vi.mocked(createAlbum).mockReset()
    vi.mocked(renameAlbum).mockReset().mockResolvedValue(null)
    vi.mocked(uploadPhoto).mockReset()
  })

  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(cleanup)

  it('crée un album puis y envoie deux photos (scénario 5)', async () => {
    vi.mocked(createAlbum).mockResolvedValue({ id: 7, name: 'Mariage Dupont', photos: [] })
    vi.mocked(uploadPhoto)
      .mockResolvedValueOnce({ id: 1, url: 'https://exemple.test/1.jpg' })
      .mockResolvedValueOnce({ id: 2, url: 'https://exemple.test/2.jpg' })
    const { container } = renderPage()
    await screen.findByText('Aucun album pour le moment.')

    fireEvent.change(screen.getByLabelText('Nom du nouvel album'), { target: { value: 'Mariage Dupont' } })
    fireEvent.click(screen.getByRole('button', { name: '+ Nouvel album' }))
    expect(await screen.findByRole('heading', { level: 2, name: 'Mariage Dupont' })).toBeInTheDocument()
    expect(createAlbum).toHaveBeenCalledWith('Mariage Dupont', 'jeton')

    const fichiers = [
      new File(['a'], 'a.jpg', { type: 'image/jpeg' }),
      new File(['b'], 'b.png', { type: 'image/png' }),
    ]
    fireEvent.change(container.querySelector('input[type="file"]'), { target: { files: fichiers } })

    expect(await screen.findByAltText('Mariage Dupont, photo 2')).toBeInTheDocument()
    expect(uploadPhoto).toHaveBeenNthCalledWith(1, 7, fichiers[0], 'jeton')
    expect(uploadPhoto).toHaveBeenNthCalledWith(2, 7, fichiers[1], 'jeton')
  })

  it('renomme l\'album sélectionné', async () => {
    vi.mocked(listMyAlbums).mockResolvedValue([{ id: 3, name: 'Ancien nom', photos: [] }])
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Renommer' }))
    fireEvent.change(screen.getByLabelText('Nouveau nom'), { target: { value: 'Nouveau nom ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }))

    expect(await screen.findByRole('heading', { level: 2, name: 'Nouveau nom' })).toBeInTheDocument()
    expect(renameAlbum).toHaveBeenCalledWith(3, 'Nouveau nom ', 'jeton')
  })

  it('affiche le refus de l\'upload sans perdre l\'album', async () => {
    vi.mocked(listMyAlbums).mockResolvedValue([{ id: 3, name: 'Portraits', photos: [] }])
    vi.mocked(uploadPhoto).mockRejectedValue({ status: 400, message: 'La signature de Cloudinary est invalide' })
    const { container } = renderPage()
    await screen.findByRole('heading', { level: 2, name: 'Portraits' })

    fireEvent.change(container.querySelector('input[type="file"]'), {
      target: { files: [new File(['a'], 'a.jpg', { type: 'image/jpeg' })] },
    })

    expect(await screen.findByRole('alert')).toHaveTextContent('La signature de Cloudinary est invalide')
    expect(screen.getByText('Cet album ne contient pas encore de photo.')).toBeInTheDocument()
  })

  it('invite à créer son profil créateur si le backend répond 404', async () => {
    vi.mocked(listMyAlbums).mockRejectedValue({ status: 404, message: 'Profil introuvable' })
    renderPage()

    expect(await screen.findByRole('link', { name: 'Créer mon profil' })).toHaveAttribute('href', '/mon-profil')
  })
})
