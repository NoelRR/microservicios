import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// El frontend llama a /api/** y Vite lo reenvia al gateway en :8080
// quitando el prefijo /api. Asi se evita CORS en dev sin tocar el backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});
