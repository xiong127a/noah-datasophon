import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/ddh/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        ws: true,
        rewrite: (path) => {
          const newPath = path.replace(/^\/ddh\/api/, '/api')
          console.log(`[Vite Proxy] ${path} -> ${newPath}`)
          return newPath
        },
        configure: (proxy, options) => {
          // 在这里可以添加调试信息，查看代理配置是否生效
          proxy.on('error', (err, req, res) => {
            console.error(`[Vite Proxy Error] ${req.url}:`, err.message)
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log(`[Vite Proxy Request] ${req.method} ${req.url}`)
            console.log(`[Vite Proxy Headers]`, req.headers)
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log(`[Vite Proxy Response] ${proxyRes.statusCode} ${req.url}`)
          });
        }
      }
    }
  }
})
