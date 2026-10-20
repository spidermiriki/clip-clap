import { BrowserRouter, Route, Routes } from 'react-router-dom'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/mon-profil-createur" element={<p>Chargement…</p>} />
      </Routes>
    </BrowserRouter>
  )
}
