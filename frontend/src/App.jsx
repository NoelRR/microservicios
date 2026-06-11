import { Routes, Route, NavLink } from 'react-router-dom';
import Menu from './pages/Menu';
import Reportes from './pages/Reportes';

function Nav() {
  return (
    <nav className="nav">
      <span className="brand">🍔 Toby</span>
      <NavLink to="/" end>Menú</NavLink>
      <NavLink to="/reportes">Reportes</NavLink>
    </nav>
  );
}

export default function App() {
  return (
    <>
      <Nav />
      <main className="container">
        <Routes>
          <Route path="/" element={<Menu />} />
          <Route path="/reportes" element={<Reportes />} />
        </Routes>
      </main>
    </>
  );
}
