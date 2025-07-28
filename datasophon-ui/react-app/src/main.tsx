import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from '@tanstack/react-router';
import { router } from './routes';
import './styles/index.css';

// 创建React Query客户端
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5分钟
      refetchOnWindowFocus: false,
    },
  },
});

// 注册路由已经在routes/index.tsx中完成了，这里不需要重复注册

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>,
); 