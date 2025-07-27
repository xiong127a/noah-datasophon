/// <reference types="vite/client" />
/// <reference types="unplugin-vue-router/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// 全局vue-sonner
interface Window {
  vueSonner: {
    toast: any;
    Toaster: any;
  };
  $app: any;
  $router: any;
  $toast: any;
} 