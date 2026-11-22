import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createAlbum, deleteAlbum, deletePhoto, listMyAlbums, renameAlbum, uploadPhoto } from '../api/albums'
import MesAlbums from './MesAlbums'

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'jeton' } }) }))
vi.mock('../api/albums', async (importOriginal) => ({
  ...(await importOriginal()),
  listMyAlbums: vi.fn(),
  createAlbum: vi.fn(),
  renameAlbum: vi.fn(),
  uploadPhoto: vi.fn(),
  deletePhoto: vi.fn(),
  deleteAlbum: vi.fn(),
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
    vi.mocked(deletePhoto).mockReset().mockResolvedValue(null)
    vi.mocked(deleteAlbum).mockReset().mockResolvedValue(null)
  })

  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

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

  // ── Suppression (#94) ──────────────────────────────────────────────────────

  const ALBUM = {
    id: 7,
    name: 'Mariage Dupont',
    photos: [{ id: 1, url: 'https://exemple.test/1.jpg' }, { id: 2, url: 'https://exemple.test/2.jpg' }],
  }

  it('supprime une photo après confirmation', async () => {
    vi.mocked(listMyAlbums).mockResolvedValue([ALBUM])
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Supprimer la photo 1' }))

    expect(await screen.findByRole('button', { name: /Mariage Dupont \(1\)/ })).toBeInTheDocument()
    expect(deletePhoto).toHaveBeenCalledWith(7, 1, 'jeton')
    // La photo restante est renumérotée
    expect(screen.getByAltText('Mariage Dupont, photo 1')).toHaveAttribute('src', 'https://exemple.test/2.jpg')
  })

  it("ne supprime rien si l'utilisateur annule", async () => {
    vi.mocked(listMyAlbums).mockResolvedValue([ALBUM])
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: "Supprimer l'album" }))

    expect(deleteAlbum).not.toHaveBeenCalled()
  })

  it("supprime l'album et sélectionne le suivant", async () => {
    vi.mocked(listMyAlbums).mockResolvedValue([ALBUM, { id: 8, name: 'Portraits', photos: [] }])
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: "Supprimer l'album" }))

    expect(await screen.findByRole('heading', { name: 'Portraits' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Mariage Dupont/ })).not.toBeInTheDocument()
    expect(deleteAlbum).toHaveBeenCalledWith(7, 'jeton')
  })

  it('affiche le refus du backend quand Cloudinary est indisponible (502) et garde la photo', async () => {
    vi.mocked(listMyAlbums).mockResolvedValue([ALBUM])
    vi.mocked(deletePhoto).mockRejectedValue({ status: 502, message: "Le stockage des médias n'a pas confirmé la suppression." })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Supprimer la photo 1' }))

    expect(await screen.findByRole('alert')).toHaveTextContent("n'a pas confirmé la suppression")
    expect(screen.getByAltText('Mariage Dupont, photo 1')).toBeInTheDocument()
  })

  // ── Vidéos (#94) ───────────────────────────────────────────────────────────

  it("envoie une vidéo et l'affiche avec un lecteur", async () => {
    vi.mocked(listMyAlbums).mockResolvedValue([{ id: 7, name: 'Clips', photos: [] }])
    vi.mocked(uploadPhoto).mockResolvedValue({ id: 3, url: 'https://exemple.test/clip.mp4', mediaType: 'VIDEO' })
    const { container } = renderPage()
    await screen.findByText('Cet album ne contient pas encore de photo.')

    const clip = new File(['x'], 'clip.mp4', { type: 'video/mp4' })
    fireEvent.change(container.querySelector('input[type="file"]'), { target: { files: [clip] } })

    const lecteur = await screen.findByLabelText('Clips, vidéo 1')
    expect(lecteur.tagName).toBe('VIDEO')
    expect(lecteur).toHaveAttribute('src', 'https://exemple.test/clip.mp4')
    expect(screen.getByRole('button', { name: 'Supprimer la vidéo 1' })).toBeInTheDocument()
    expect(uploadPhoto).toHaveBeenCalledWith(7, clip, 'jeton')
  })

  it('accepte les vidéos dans le sélecteur de fichiers', async () => {
    vi.mocked(listMyAlbums).mockResolvedValue([{ id: 7, name: 'Clips', photos: [] }])
    const { container } = renderPage()
    await screen.findByText('Cet album ne contient pas encore de photo.')

    expect(container.querySelector('input[type="file"]').getAttribute('accept')).toContain('video/mp4')
  })
})
