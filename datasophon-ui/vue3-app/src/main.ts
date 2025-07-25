import './style.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { Toaster, toast } from 'vue-sonner'

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

// 设置SVG图标
setupSvgIcon(app)

// 全局挂载API
app.config.globalProperties.$api = api
// 全局挂载axios实例
app.config.globalProperties.$axios = axios

// 挂载应用
app.mount('#app')

console.log('SVG图标系统已初始化')
