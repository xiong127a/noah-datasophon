import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// 基础historyFallback中间件，优化特殊路径处理
const historyFallback = () => ({
  name: 'history-fallback',
  configureServer(server) {
    return () => {
      server.middlewares.use((req, res, next) => {
        if (!req.url) {
          req.url = '/'
        }
        
        // 检查是否静态资源或API请求
        const isStaticResource = /\.(js|css|ico|png|jpg|jpeg|gif|svg|woff|woff2|ttf|eot|json|map|html)$/i.test(req.url)
        const isApiRequest = req.url.startsWith('/api/') || req.url.startsWith('/ddh/api/')
        
        // 特殊处理这两个问题路径 
        if (req.url === '/colony-manage/storage' || req.url === '/colony-manage/framework') {
          console.log(`[History Fallback] 检测到特殊路径: ${req.url}，使用特殊处理`)
          
          // 记录完整请求信息
          console.log(`[Request Details] 方法: ${req.method}, 请求头:`, req.headers)
          
          // 检查是否是直接访问（通过浏览器地址栏输入或刷新）
          const isDirectAccess = req.headers.accept && req.headers.accept.includes('text/html')
          
          if (isDirectAccess) {
            // 如果是直接访问，我们使用特殊页面
            if (req.url === '/colony-manage/storage') {
              console.log(`[History Fallback] 使用特殊直接访问页面: direct-storage.html`)
              req.url = '/direct-storage.html'
            } else {
              console.log(`[History Fallback] 使用特殊直接访问页面: direct-framework.html`)
              req.url = '/direct-framework.html'
            }
          } else {
            // 如果不是直接访问，使用标准SPA逻辑
            req.url = '/index.html'
          }
          
          // 继续下一个中间件
          return next()
        }
        
        // 其他特殊直接访问路径
        if (req.url === '/direct-storage' || req.url === '/direct-framework') {
          console.log(`[History Fallback] 检测到备用访问路径: ${req.url}`)
          if (req.url === '/direct-storage') {
            req.url = '/direct-storage.html'
          } else {
            req.url = '/direct-framework.html'
          }
          return next()
        }
        
        // 如果不是静态资源或API请求，重定向到index.html
        if (!isStaticResource && !isApiRequest) {
          console.log(`[History Fallback] ${req.url} -> /index.html`)
          req.url = '/index.html'
        }
        
        next()
      })
    }
  }
})

export default defineConfig({
  plugins: [
    vue(),
    historyFallback() // 使用简单的历史回退中间件
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173, // 更改为5173以匹配当前使用的端口
    strictPort: false, // 如果端口被占用，尝试下一个端口
    hmr: {
      overlay: false // 禁用HMR错误覆盖，方便调试
    },
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
        }
      }
    },
    // 添加 SPA 路由重写配置，确保所有前端路由请求都能正确指向 index.html
    fs: {
      // 允许为 public 和 src 提供服务
      allow: ['..']
    },
    middlewareMode: false
  },
  // 添加详细的日志配置
  build: {
    sourcemap: true, // 生成sourcemap便于调试
    reportCompressedSize: false, // 提高构建性能
  },
  // 输出更详细的调试信息
  logLevel: 'info'
})
