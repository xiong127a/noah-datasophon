// 临时类型声明文件，用于解决TypeScript错误
// 当unplugin-vue-router生成正式的类型声明后会被替换

// 声明vue-router/auto模块，提供基本路由类型
declare module 'vue-router/auto' {
  import type { Router, RouteRecordRaw } from 'vue-router'
  import type { App } from 'vue'

  export function createRouter(options: {
    history: any;
    [key: string]: any;
  }): Router

  export function createWebHistory(base?: string): any

  export function useRouter(): Router

  export function definePage(options: any): void

  export function onBeforeRouteEnter(guard: () => any): void

  export function useRoute(): any
}

// 声明虚拟模块，用于SVG图标
declare module 'virtual:svg-icons-register' {
  // 空模块，表明它存在
  const content: any
  export default content
}

// 扩展Vue模块，支持Vue组件文件
declare module '*.vue' {
  import { ComponentOptions } from 'vue'
  const componentOptions: ComponentOptions
  export default componentOptions
} 