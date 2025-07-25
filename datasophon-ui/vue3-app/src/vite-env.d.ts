/// <reference types="vite/client" />

// 声明vue-sonner全局对象
interface Window {
  vueSonner?: {
    toast: any;
    Toaster: any;
  };
  Toastify?: any;
}
