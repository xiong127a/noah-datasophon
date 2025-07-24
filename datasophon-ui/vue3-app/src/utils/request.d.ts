declare module '../utils/request' {
  export function axiosPost(url: string, params?: any, showLoading?: boolean): Promise<any>;
  export function axiosJsonPost(url: string, params?: any, showLoading?: boolean): Promise<any>;
  export function axiosGet(url: string, params?: any, showLoading?: boolean): Promise<any>;
  export function axiosDelete(url: string, params?: any, showLoading?: boolean): Promise<any>;
  export function axiosPut(url: string, params?: any, showLoading?: boolean): Promise<any>;
  export function setAuthorization(token: string): void;
  export function removeAuthorization(): void;
  export function checkAuthorization(): boolean;
  const axios: any;
  export default axios;
} 