// 导入 Tanstack Router
import { 
  createRootRoute, 
  createRoute,
  createRouter,
  Navigate,
  Outlet
} from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { getToken } from '@/utils/auth';

// 布局组件
const MainLayout = lazy(() => import('@/components/layout/MainLayout'));
const TabsLayout = lazy(() => import('@/components/layout/TabsLayout'));

// 页面组件 - 按需导入
const LoginPage = lazy(() => import('@/features/auth/LoginPage'));
const NotFoundPage = lazy(() => import('@/features/system/NotFoundPage'));
const ServiceOverview = lazy(() => import('@/features/service/ServiceOverview'));
const ClusterOverview = lazy(() => import('@/features/cluster/ClusterOverview'));

// 加载指示器组件
const LoadingFallback = () => <div className="w-full h-full flex items-center justify-center">加载中...</div>;

// 认证组件
const RequireAuth = ({ children }: { children: React.ReactNode }) => {
  const token = getToken();
  if (!token) return <Navigate to="/login" />;
  return <>{children}</>;
};

// 根路由
const rootRoute = createRootRoute({
  component: () => <Outlet />
});

// 登录路由
const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: () => (
    <Suspense fallback={<LoadingFallback />}>
      <LoginPage />
    </Suspense>
  )
});

// 404路由
const notFoundRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '*',
  component: () => (
    <Suspense fallback={<LoadingFallback />}>
      <NotFoundPage />
    </Suspense>
  )
});

// 授权布局路由
const appLayoutRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'app',
  component: () => (
    <Suspense fallback={<LoadingFallback />}>
      <RequireAuth>
        <TabsLayout />
      </RequireAuth>
    </Suspense>
  )
});

// 首页路由
const indexRoute = createRoute({
  getParentRoute: () => appLayoutRoute,
  path: '/',
  component: () => (
    <Suspense fallback={<LoadingFallback />}>
      <ServiceOverview />
    </Suspense>
  )
});

// 概览路由
const overviewRoute = createRoute({
  getParentRoute: () => appLayoutRoute,
  path: '/overview',
  component: () => (
    <Suspense fallback={<LoadingFallback />}>
      <ClusterOverview />
    </Suspense>
  )
});

// 构建路由树
const routeTree = rootRoute.addChildren([
  loginRoute,
  notFoundRoute,
  appLayoutRoute.addChildren([
    indexRoute,
    overviewRoute
  ])
]);

// 创建并导出路由器
export const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
});

// 为TypeScript声明类型
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
} 