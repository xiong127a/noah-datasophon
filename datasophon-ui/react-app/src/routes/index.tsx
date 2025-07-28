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
import TabsLayout from '@/components/layout/TabsLayout';

// 惰性加载路由组件
const LoginPage = lazy(() => import('@/features/auth/LoginPage'));
const NotFoundPage = lazy(() => import('@/features/404/NotFoundPage'));
const OverviewPage = lazy(() => import('@/features/overview/OverviewPage'));
const HomePage = lazy(() => import('@/features/home/HomePage'));

// 主机管理
const HostManagePage = lazy(() => import('@/features/host/HostManagePage'));

// 告警管理
const AlarmNoticePage = lazy(() => import('@/features/alarm/AlarmNoticePage'));
const AlarmGroupPage = lazy(() => import('@/features/alarm/AlarmGroupPage'));
const AlarmTablePage = lazy(() => import('@/features/alarm/AlarmTablePage'));
const AlarmHelpPage = lazy(() => import('@/features/alarm/AlarmHelpPage'));

// 系统管理
const SystemTenantPage = lazy(() => import('@/features/system/TenantManagePage'));
const SystemUserPage = lazy(() => import('@/features/system/UserManagePage'));
const SystemRackPage = lazy(() => import('@/features/system/RackManagePage'));
const SystemTagPage = lazy(() => import('@/features/system/TagManagePage'));
const SystemLogPage = lazy(() => import('@/features/system/LogAuditPage'));

// 集群管理
const ClusterListPage = lazy(() => import('@/features/cluster/ClusterListPage'));
const ClusterStoragePage = lazy(() => import('@/features/cluster/ClusterStoragePage'));
const ClusterFrameworkPage = lazy(() => import('@/features/cluster/ClusterFrameworkPage'));

// 用户管理
const UserManagePage = lazy(() => import('@/features/user/UserManagePage'));

// 加载指示器组件
const LoadingFallback = () => <div className="w-full h-full flex items-center justify-center">加载中...</div>;

// 身份验证检查组件
const RequireAuth = ({ children }: { children: React.ReactNode }) => {
  const token = getToken();
  if (!token) {
    return <Navigate to="/login" />;
  }
  return <>{children}</>;
};

// 创建根路由
const rootRoute = createRootRoute({
  component: () => {
    return <div className="app-container"><Outlet /></div>;
  }
});

// 创建路由层级
const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => <Navigate to="/overview" />,
});

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: () => <LoginPage />,
});

const notFoundRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '*',
  component: () => <NotFoundPage />,
});

// 应用布局路由 - 所有内部页面都是它的子路由
const layoutRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'layout',
  component: TabsLayout,
});

// 创建各个功能模块的子路由
const overviewRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/overview',
  component: () => (
    <RequireAuth>
      <OverviewPage />
    </RequireAuth>
  ),
});

const homeRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/',
  component: () => (
    <RequireAuth>
      <HomePage />
    </RequireAuth>
  ),
});

// 主机管理路由
const hostManageRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/host-manage',
  component: () => (
    <RequireAuth>
      <HostManagePage />
    </RequireAuth>
  ),
});

// 告警管理路由
const alarmNoticeRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/alarm-manage/notice',
  component: () => (
    <RequireAuth>
      <AlarmNoticePage />
    </RequireAuth>
  ),
});

const alarmGroupRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/alarm-manage/group',
  component: () => (
    <RequireAuth>
      <AlarmGroupPage />
    </RequireAuth>
  ),
});

const alarmTableRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/alarm-manage/table',
  component: () => (
    <RequireAuth>
      <AlarmTablePage />
    </RequireAuth>
  ),
});

const alarmHelpRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/alarm-manage/help',
  component: () => (
    <RequireAuth>
      <AlarmHelpPage />
    </RequireAuth>
  ),
});

// 系统管理路由
const systemTenantRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/system-manage/tenant',
  component: () => (
    <RequireAuth>
      <SystemTenantPage />
    </RequireAuth>
  ),
});

const systemUserRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/system-manage/user',
  component: () => (
    <RequireAuth>
      <SystemUserPage />
    </RequireAuth>
  ),
});

const systemRackRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/system-manage/rack',
  component: () => (
    <RequireAuth>
      <SystemRackPage />
    </RequireAuth>
  ),
});

const systemTagRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/system-manage/tag',
  component: () => (
    <RequireAuth>
      <SystemTagPage />
    </RequireAuth>
  ),
});

const systemLogRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/system-manage/log',
  component: () => (
    <RequireAuth>
      <SystemLogPage />
    </RequireAuth>
  ),
});

// 集群管理路由
const clusterListRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/cluster-manage/list',
  component: () => (
    <RequireAuth>
      <ClusterListPage />
    </RequireAuth>
  ),
});

const clusterStorageRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/cluster-manage/storage',
  component: () => (
    <RequireAuth>
      <ClusterStoragePage />
    </RequireAuth>
  ),
});

const clusterFrameworkRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/cluster-manage/framework',
  component: () => (
    <RequireAuth>
      <ClusterFrameworkPage />
    </RequireAuth>
  ),
});

// 用户管理路由
const userManageRoute = createRoute({
  getParentRoute: () => layoutRoute,
  path: '/user-manage',
  component: () => (
    <RequireAuth>
      <UserManagePage />
    </RequireAuth>
  ),
});

// 创建路由器并注册所有路由
export const routeTree = rootRoute.addChildren([
  indexRoute,
  loginRoute,
  notFoundRoute,
  layoutRoute.addChildren([
    homeRoute,
    overviewRoute,
    hostManageRoute,
    alarmNoticeRoute,
    alarmGroupRoute,
    alarmTableRoute,
    alarmHelpRoute,
    systemTenantRoute,
    systemUserRoute,
    systemRackRoute,
    systemTagRoute,
    systemLogRoute,
    clusterListRoute,
    clusterStorageRoute,
    clusterFrameworkRoute,
    userManageRoute
  ]),
]);

const router = createRouter({ routeTree });

// 导出路由类型供类型推断使用
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}

export default router; 