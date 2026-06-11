import { useEffect, useState } from 'react';
import { api } from '../api';

export default function Reportes() {
  const [ventas, setVentas] = useState(null);
  const [topPlatos, setTopPlatos] = useState([]);
  const [error, setError] = useState('');

  async function cargar() {
    setError('');
    try {
      const [v, t] = await Promise.all([
        api.get('/reportes/ventas'),
        api.get('/reportes/platos-mas-vendidos'),
      ]);
      setVentas(v);
      setTopPlatos(t);
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => { cargar(); }, []);

  return (
    <section>
      <h2>Reportes</h2>
      {error && <p className="error">{error}</p>}

      {ventas && (
        <div className="kpis">
          <div className="card kpi">
            <span className="muted">Pedidos</span>
            <strong>{ventas.totalPedidos}</strong>
          </div>
          <div className="card kpi">
            <span className="muted">Ingreso total</span>
            <strong>${ventas.ingresoTotal}</strong>
          </div>
          <div className="card kpi">
            <span className="muted">Ticket promedio</span>
            <strong>${ventas.ticketPromedio}</strong>
          </div>
        </div>
      )}

      <div className="card">
        <h3>Platos más vendidos</h3>
        <table>
          <thead><tr><th>Plato</th><th>Unidades</th></tr></thead>
          <tbody>
            {topPlatos.map((p) => (
              <tr key={p.platoId}><td>#{p.platoId}</td><td>{p.unidades}</td></tr>
            ))}
            {topPlatos.length === 0 && <tr><td colSpan="2" className="muted">Sin datos</td></tr>}
          </tbody>
        </table>
      </div>

      <button onClick={cargar}>Actualizar</button>
    </section>
  );
}
