import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { creatorsQuery, getCategories, getCreators } from '../api/catalog'
import Galerie from './Galerie'

vi.mock('../api/catalog', async (importOriginal) => ({
  ...(await importOriginal()),
  getCreators: vi.fn(),
  getCategories: vi.fn(),
}))

function UrlCourante() {
  return <p data-testid="url">{useLocation().search}</p>
}

// Simule le bouton Précédent du navigateur
function BoutonPrecedent() {
  const navigate = useNavigate()
  return <button type="button" onClick={() => navigate(-1)}>Précédent du navigateur</button>
}

function renderGalerie(url, historique = [url]) {
  return render(
    <MemoryRouter initialEntries={historique} initialIndex={historique.length - 1}>
      <Routes>
        <Route path="/" element={<><Galerie /><UrlCourante /><BoutonPrecedent /></>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('Galerie — filtres (#14)', () => {
  beforeEach(() => {
    vi.mocked(getCreators).mockReset().mockResolvedValue({ items: [], page: 0, size: 12, totalElements: 0 })
    vi.mocked(getCategories).mockReset().mockResolvedValue([{ id: 1, name: 'Mariage' }])
  })

  // Sans `globals: true` dans la config Vitest, Testing Library ne démonte pas le rendu tout seul
  afterEach(cleanup)

  it('applique les filtres présents dans l\'URL au premier chargement', async () => {
    renderGalerie('/?ville=Mons&categorie=1&media=PHOTO')

    await waitFor(() => expect(getCreators).toHaveBeenCalledWith(
      { page: 0, size: 12, city: 'Mons', country: '', categoryId: '1', mediaType: 'PHOTO' },
    ))
    expect(screen.getByLabelText('Ville')).toHaveValue('Mons')
    expect(screen.getByLabelText('Type de média')).toHaveValue('PHOTO')
  })

  it('Filtrer met les filtres dans l\'URL et repart de la première page', async () => {
    vi.mocked(getCreators).mockResolvedValue({ items: [], page: 2, size: 12, totalElements: 40 })
    renderGalerie('/')
    await screen.findByRole('option', { name: 'Mariage' })

    fireEvent.change(screen.getByLabelText('Pays'), { target: { value: ' Belgique ' } })
    fireEvent.change(screen.getByLabelText('Type de prestation'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Type de média'), { target: { value: 'PHOTO' } })
    fireEvent.click(screen.getByRole('button', { name: 'Filtrer' }))

    await waitFor(() => expect(getCreators).toHaveBeenLastCalledWith(
      { page: 0, size: 12, city: '', country: 'Belgique', categoryId: '1', mediaType: 'PHOTO' },
    ))
    expect(screen.getByTestId('url')).toHaveTextContent('?pays=Belgique&categorie=1&media=PHOTO')
  })

  it('chaque carte mène au profil public du créateur (#17)', async () => {
    vi.mocked(getCreators).mockResolvedValue({
      items: [{ userId: 3, displayName: 'Clara Lecomte', photoUrl: null, city: 'Mons', country: 'Belgique' }],
      page: 0,
      size: 12,
      totalElements: 1,
    })
    renderGalerie('/')

    expect(await screen.findByRole('link', { name: /Clara Lecomte/ })).toHaveAttribute('href', '/createurs/3')
  })

  it('Précédent du navigateur : les champs reprennent les filtres de l\'URL et la page 1 est rechargée', async () => {
    renderGalerie('/?ville=Namur&categorie=1', ['/?ville=Mons', '/?ville=Namur&categorie=1'])
    await screen.findByRole('option', { name: 'Mariage' })
    expect(screen.getByLabelText('Ville')).toHaveValue('Namur')
    expect(screen.getByLabelText('Type de prestation')).toHaveValue('1')

    fireEvent.click(screen.getByRole('button', { name: 'Précédent du navigateur' }))

    await waitFor(() => expect(screen.getByLabelText('Ville')).toHaveValue('Mons'))
    expect(screen.getByLabelText('Type de prestation')).toHaveValue('')
    expect(screen.getByTestId('url')).toHaveTextContent('?ville=Mons')
    await waitFor(() => expect(getCreators).toHaveBeenLastCalledWith(
      { page: 0, size: 12, city: 'Mons', country: '', categoryId: '' },
    ))
  })

  it('annonce l\'absence de résultat quand des filtres sont actifs', async () => {
    renderGalerie('/?pays=Islande')

    expect(await screen.findByText('Aucun créateur ne correspond à ces filtres.')).toBeInTheDocument()
  })
})

describe('creatorsQuery', () => {
  it('n\'envoie que les filtres renseignés', () => {
    expect(creatorsQuery({ page: 0, size: 12, city: ' Mons ', country: '', categoryId: '' }))
      .toBe('page=0&size=12&city=Mons')
    expect(creatorsQuery({ page: 1, size: 12, city: '', country: 'Belgique', categoryId: '3' }))
      .toBe('page=1&size=12&country=Belgique&categoryId=3')
    expect(creatorsQuery({ page: 0, size: 12, city: '', country: '', categoryId: '', mediaType: 'PHOTO' }))
      .toBe('page=0&size=12&mediaType=PHOTO')
  })
})
