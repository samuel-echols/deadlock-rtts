import { Routes, Route, NavLink } from 'react-router-dom'
import HeroTrendPage from './HeroTrendPage'
import MoversPage from './MoversPage'
import PatchDiffPage from './PatchDiffPage'

export default function App() {
  return (
    <>
      <nav>
        <span className="brand">Deadlock Meta History</span>
        <NavLink to="/" end>Hero Trends</NavLink>
        <NavLink to="/movers">Movers</NavLink>
        <NavLink to="/patch-diff">Patch Diff</NavLink>
      </nav>

      <Routes>
        <Route path="/" element={<HeroTrendPage />} />
        <Route path="/movers" element={<MoversPage />} />
        <Route path="/patch-diff" element={<PatchDiffPage />} />
      </Routes>

      <footer>
        Data provided by <a href="https://deadlock-api.com" target="_blank" rel="noreferrer">deadlock-api.com</a>
        {' '}— open-source community API (MIT). Not affiliated with Valve.
      </footer>
    </>
  )
}
