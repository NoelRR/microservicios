import { useEffect, useState } from 'react';
import { api } from '../api';

// Sin auth: cliente fijo para la demo (ya no hay user-service).
const CLIENTE_EMAIL = 'cliente@toby.com';

export default function Menu() {
  const [platos, setPlatos] = useState([]);
  const [carrito, setCarrito] = useState({}); // platoId -> cantidad
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  async function cargar() {
    setError('');
    try {
      setPlatos(await api.get('/menu/items'));
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => { cargar(); }, []);

  function agregar(id) {
    setCarrito((c) => ({ ...c, [id]: (c[id] || 0) + 1 }));
  }
  function quitar(id) {
    setCarrito((c) => {
      const n = { ...c };
      if (n[id] > 1) n[id] -= 1; else delete n[id];
      return n;
    });
  }

  const items = Object.entries(carrito).map(([platoId, cantidad]) => ({
    platoId: Number(platoId),
    cantidad,
  }));
  const total = items.reduce((s, it) => {
    const p = platos.find((x) => x.id === it.platoId);
    return s + (p ? p.precio * it.cantidad : 0);
  }, 0);

  async function confirmar() {
    setError(''); setOk('');
    try {
      const pedido = await api.post('/pedidos', {
        clienteEmail: CLIENTE_EMAIL,
        items,
      });
      setOk(`Pedido #${pedido.id} creado. Total $${pedido.total}`);
      setCarrito({});
      cargar(); // refresca disponibilidad (puede cambiar por stock)
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div className="grid-2">
      <section>
        <h2>Menú</h2>
        {error && <p className="error">{error}</p>}
        <div className="platos">
          {platos.map((p) => (
            <div className="card plato" key={p.id}>
              <div>
                <strong>{p.nombre}</strong>
                <div className="muted">{p.categoria}</div>
              </div>
              <div className="precio">${p.precio}</div>
              <button onClick={() => agregar(p.id)}>Agregar</button>
            </div>
          ))}
          {platos.length === 0 && !error && <p className="muted">No hay platos disponibles.</p>}
        </div>
      </section>

      <aside>
        <h2>Carrito</h2>
        <div className="card">
          {items.length === 0 && <p className="muted">Vacío</p>}
          {items.map((it) => {
            const p = platos.find((x) => x.id === it.platoId);
            return (
              <div className="linea" key={it.platoId}>
                <span>{p ? p.nombre : it.platoId}</span>
                <span className="qty">
                  <button onClick={() => quitar(it.platoId)}>−</button>
                  {it.cantidad}
                  <button onClick={() => agregar(it.platoId)}>+</button>
                </span>
                <span>${(p ? p.precio * it.cantidad : 0).toFixed(2)}</span>
              </div>
            );
          })}
          {items.length > 0 && (
            <>
              <div className="linea total">
                <strong>Total</strong>
                <strong>${total.toFixed(2)}</strong>
              </div>
              <button className="primary" onClick={confirmar}>Confirmar pedido</button>
            </>
          )}
          {ok && <p className="ok">{ok}</p>}
        </div>
      </aside>
    </div>
  );
}
