// 全局类型声明
declare module '*.vue' {
  import { ComponentOptions } from 'vue'
  const component: ComponentOptions
  export default component
}

// 路由模块声明
declare module 'vue-router/auto' {
  import type { Router, RouteLocationNormalizedLoaded } from 'vue-router'
  export { RouteLocationNormalizedLoaded as RouteLocation }
  export * from 'vue-router'
  export function definePage(options: any): void
  export function onBeforeRouteEnter(guard: () => any): void
}

// SVG图标模块声明
declare module 'virtual:svg-icons-register' {
  const component: any
  export default component
}

// 图像文件模块声明
declare module '*.svg' {
  const content: any
  export default content
}

declare module '*.png' {
  const content: any
  export default content
}

declare module '*.jpg' {
  const content: any
  export default content
} 