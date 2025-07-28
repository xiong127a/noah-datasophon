import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: '192.168.200.6',
    port: 8082,
    proxy: {
      '/api': {
        target: 'http://192.168.200.3:8081',
        changeOrigin: true,
        rewrite: (path) => path,  // 不修改路径
      },
    },
  },
}); 