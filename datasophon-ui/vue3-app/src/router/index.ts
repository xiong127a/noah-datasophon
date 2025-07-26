import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { authService } from '../composables/useAuth'
import { useUserStore } from '../stores/user'

// 检查认证状态的简化函数
const checkAuthorization = () => {
  const userStore = useUserStore()
  return userStore.isLoggedIn
}

// 布局组件
import MainLayout from '../layouts/MainLayout.vue'

// 简化路由配置，只保留关键路径
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: '主页',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '主页', icon: 'home' }
      },
      {
        path: 'host-manage',
        name: '主机管理',
        component: () => import('../views/host/HostManage.vue'),
        meta: { title: '主机管理', icon: 'host-manager' }
      },
      {
        path: 'alarm-manage',
        name: '告警管理',
        redirect: '/alarm-manage/notification',
        meta: { title: '告警管理', icon: 'alarm' },
        children: [
          {
            path: 'notification',
            name: '通知组管理',
            component: () => import('../views/alarm/NoticeManage.vue'),
            meta: { title: '通知组管理', icon: 'notice' }
          },
          {
            path: 'group',
            name: '告警组管理',
            component: () => import('../views/alarm/GroupManage.vue'),
            meta: { title: '告警组管理', icon: 'group' }
          },
          {
            path: 'metric',
            name: '告警指标管理',
            component: () => import('../views/alarm/MetricManage.vue'),
            meta: { title: '告警指标管理', icon: 'metric' }
          },
          {
            path: 'help',
            name: '使用帮助',
            component: () => import('../views/alarm/HelpInfo.vue'),
            meta: { title: '使用帮助', icon: 'help' }
          }
        ]
      },
      {
        path: 'system-manage',
        name: '系统管理',
        redirect: '/system-manage/tenant',
        meta: { title: '系统管理', icon: 'system' },
        children: [
          {
            path: 'tenant',
            name: '租户管理',
            component: () => import('../views/system/TenantManage.vue'),
            meta: { title: '租户管理', icon: 'tenant' }
          },
          {
            path: 'user',
            name: '用户管理',
            component: () => import('../views/system/UserManage.vue'),
            meta: { title: '用户管理', icon: 'user' }
          },
          {
            path: 'rack',
            name: '机架管理',
            component: () => import('../views/host/RackManage.vue'),
            meta: { title: '机架管理', icon: 'rack' }
          },
          {
            path: 'label',
            name: '标签管理',
            component: () => import('../views/host/LabelManage.vue'),
            meta: { title: '标签管理', icon: 'label' }
          },
          {
            path: 'log',
            name: '日志审计',
            component: () => import('../views/system/LogManage.vue'),
            meta: { title: '日志审计', icon: 'log' }
          }
        ]
      },
      {
        path: 'colony-manage',
        name: '集群管理',
        redirect: '/colony-manage/list',
        meta: { title: '集群管理', icon: 'colony', rightSide: true },
        children: [
          {
            path: 'list',
            name: '集群列表管理',
            component: () => import('../views/cluster/ClusterList.vue'),
            meta: { title: '集群管理', icon: 'cluster' }
          },
          // 只保留主路径，直接指向repository目录下的组件
          {
            path: 'storage',
            name: '存储库管理',
            component: () => import('../views/repository/ParcelList.vue'),
            meta: { title: '存储库管理', icon: 'storage' }
          },
          {
            path: 'framework',
            name: '集群框架',
            component: () => import('../views/cluster/FrameworkManage.vue'),
            meta: { title: '集群框架', icon: 'framework' }
          }
        ]
      },
      {
        path: 'user-manage',
        name: '用户管理',
        component: () => import('../views/user/UserManage.vue'),
        meta: { title: '用户管理', icon: 'user_manager', rightSide: true }
      },
      // 保留调试页面
      {
        path: 'debug-routes',
        name: '路由调试器',
        component: () => import('../views/cluster/debug-routes.vue'),
        meta: { title: '路由调试器', icon: 'debug' }
      }
    ]
  },

  // 修正登录路由路径
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'),
    meta: { title: '登录' }
  },

  // 处理URL中带有哈希部分的情况
  {
    path: '/:catchAll(.*\\/#.*)',
    redirect: to => {
      // 去除URL中的哈希部分，例如 /colony-manage/storage#/colony-manage/storage => /colony-manage/storage
      const path = to.path.split('#')[0]
      console.log(`[Router] 修正带哈希的路径: ${to.path} => ${path}`)
      return path
    }
  },

  // 404路由必须放在最后
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 添加调试输出中间件
router.beforeResolve((to, from, next) => {
  console.log(`[Router Debug] 解析路由: ${from.path} -> ${to.path}`)
  console.log(`[Router Debug] 目标组件:`, to.matched.map(record => record.components?.default?.name || 'anonymous'))
  next()
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置标题
  document.title = `${to.meta.title || ''}｜Noah大数据平台` || 'Noah大数据平台'
  
  // 调试输出
  console.log(`[Router] 路由变化: ${from.path} -> ${to.path}`)
  console.log(`[Router] 当前认证状态: ${checkAuthorization() ? '已登录' : '未登录'}`)
  
  // 获取userStore以检查登出状态
  const userStore = useUserStore()
  
  // 如果正在登出过程中，允许导航继续
  if (userStore.isLoggingOut) {
    console.log('[Router] 检测到正在登出过程中，允许导航继续')
    return next()
  }
  
  // 判断是否需要登录权限
  const publicPaths = ['/login']
  if (!publicPaths.includes(to.path)) {
    if (checkAuthorization()) {
      console.log(`[Router] 已登录，允许访问: ${to.path}`)
      next() // 已登录，允许访问
    } else {
      console.log(`[Router] 未登录，重定向到登录页`)
      next({ path: '/login', query: { redirect: to.fullPath } }) // 未登录，跳转到登录页面
    }
  } else {
    // 如果是访问登录页面且已登录，重定向到首页
    if (to.path === '/login' && checkAuthorization()) {
      console.log(`[Router] 已登录用户访问登录页，重定向到首页`)
      next({ path: '/' })
    } else {
      console.log(`[Router] 允许访问公开页面: ${to.path}`)
      next() // 未登录，允许访问登录页面
    }
  }
})

// 捕获导航错误
router.onError((error) => {
  console.error('[Router] 路由错误:', error)
})

export default router