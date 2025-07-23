import './style.css'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'

// 导入SVG图标系统
import setupSvgIcon from './icons'

// 创建应用实例
const app = createApp(App)

// 注册路由和状态管理
app.use(router)
app.use(pinia)

// 设置SVG图标
setupSvgIcon(app)

// 挂载应用
app.mount('#app')
