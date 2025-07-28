import './style.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { Toaster, toast } from 'vue-sonner'

// 导入全局错误边界组件
import GlobalErrorBoundary from './components/GlobalErrorBoundary.vue'

// 全局挂载vue-sonner，以便在任何地方访问
window.vueSonner = { toast, Toaster }

// 导入SVG图标系统
import 'virtual:svg-icons-register'
import setupSvgIcon from './icons'

// 导入API和Axios实例
import api, { axios } from './api'

// 创建Vue应用实例
const app = createApp(App)

// 使用Pinia状态管理
app.use(createPinia())
// 使用路由
app.use(router)

// 注册全局组件
app.component('Toaster', Toaster)
app.component('GlobalErrorBoundary', GlobalErrorBoundary)

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue App Error]', err)
  console.error('[Component]', instance)
  console.error('[Error Info]', info)
  
  // 显示全局错误提示
  toast.error('发生错误，请尝试刷新页面')
}

// 设置SVG图标
setupSvgIcon(app)

// 全局挂载API
app.config.globalProperties.$api = api
// 全局挂载axios实例
app.config.globalProperties.$axios = axios

// 挂载应用
app.mount('#app')

// 开发环境下的全局访问（仅用于调试）
if (import.meta.env.DEV) {
  // @ts-ignore
  window.$app = app
  // @ts-ignore
  window.$router = router
  // @ts-ignore
  window.$toast = toast
}

// 输出路由配置信息
console.log('[App] 应用初始化完成，路由系统已加载');
console.log('[Router] 可用路由:', router.getRoutes());
