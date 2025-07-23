import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import VueSetupExtend from 'vite-plugin-vue-setup-extend'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    VueSetupExtend(),
    // SVG图标配置
    createSvgIconsPlugin({
      // 指定需要缓存的图标文件夹
      iconDirs: [path.resolve(process.cwd(), 'src/icons/common')],
      // 指定symbolId格式
      symbolId: 'icon-[name]',
      // 自定义SVG处理
      svgoOptions: {
        plugins: [
          {
            name: 'removeAttrs',
            params: {
              attrs: ['fill']
            }
          }
        ]
      }
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 添加服务器代理配置
  server: {
    port: 5173,
    open: true,
    proxy: {
      // 将/ddh前缀的请求代理到后端服务
      '/ddh': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        // 是否重写路径
        rewrite: (path) => path
      }
    }
  }
}) 