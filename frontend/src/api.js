// Cliente HTTP minimo contra el gateway (proxiado en /api).
// Adjunta el JWT guardado y centraliza el manejo de errores.

const BASE = '/api';

function token() {
  return localStorage.getItem('token');
}

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  const t = token();
  if (t) headers.Authorization = `Bearer ${t}`;

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    let detalle = '';
    try {
      const data = await res.json();
      detalle = data.message || data.error || JSON.stringify(data);
    } catch {
      detalle = await res.text();
    }
    throw new Error(`HTTP ${res.status}: ${detalle}`);
  }

  // 204 sin cuerpo
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  patch: (path, body) => request('PATCH', path, body),
  del: (path) => request('DELETE', path),
};
