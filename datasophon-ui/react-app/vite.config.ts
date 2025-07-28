import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
import UnoCSS from 'unocss/vite';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    UnoCSS(),
    react(),
  ],
  resolve: {
    alias: {
      '@': '/src',
    },
  },
  server: {
    host: '192.168.200.6',
    port: 8082,
    proxy: {
      '/ddh': {
        target: 'http://192.168.200.3:8081',
        changeOrigin: true,
      },
    },
  },
}); 