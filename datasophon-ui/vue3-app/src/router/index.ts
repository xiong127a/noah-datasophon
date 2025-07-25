import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 检查认证状态的简化函数
const checkAuthorization = () => !!localStorage.getItem('token')

// 布局组件
import MainLayout from '../layouts/MainLayout.vue'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    children: [
      {
        path: '/home',
        name: '主页',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '主页', icon: 'home' }
      },
      {
        path: '/host-manage',
        name: '主机管理',
        component: () => import('../views/host/HostManage.vue'),
        meta: { title: '主机管理', icon: 'host-manager' }
      },
      {
        path: '/alarm-manage',
        name: '告警管理',
        redirect: '/alarm-manage/notification',
        meta: { title: '告警管理', icon: 'alarm' },
        children: [
          {
            path: '/alarm-manage/notification',
            name: '通知组管理',
            component: () => import('../views/alarm/NoticeManage.vue'),
            meta: { title: '通知组管理', icon: 'notice' }
          },
          {
            path: '/alarm-manage/group',
            name: '告警组管理',
            component: () => import('../views/alarm/GroupManage.vue'),
            meta: { title: '告警组管理', icon: 'group' }
          },
          {
            path: '/alarm-manage/metric',
            name: '告警指标管理',
            component: () => import('../views/alarm/MetricManage.vue'),
            meta: { title: '告警指标管理', icon: 'metric' }
          },
          {
            path: '/alarm-manage/help',
            name: '使用帮助',
            component: () => import('../views/alarm/HelpInfo.vue'),
            meta: { title: '使用帮助', icon: 'help' }
          }
        ]
      },
      {
        path: '/system-manage',
        name: '系统管理',
        redirect: '/system-manage/tenant',
        meta: { title: '系统管理', icon: 'system' },
        children: [
          {
            path: '/system-manage/tenant',
            name: '租户管理',
            component: () => import('../views/system/TenantManage.vue'),
            meta: { title: '租户管理', icon: 'tenant' }
          },
          {
            path: '/system-manage/user',
            name: '用户管理',
            component: () => import('../views/system/UserManage.vue'),
            meta: { title: '用户管理', icon: 'user' }
          },
          {
            path: '/system-manage/rack',
            name: '机架管理',
            component: () => import('../views/host/RackManage.vue'),
            meta: { title: '机架管理', icon: 'rack' }
          },
          {
            path: '/system-manage/label',
            name: '标签管理',
            component: () => import('../views/host/LabelManage.vue'),
            meta: { title: '标签管理', icon: 'label' }
          },
          {
            path: '/system-manage/log',
            name: '日志审计',
            component: () => import('../views/system/LogManage.vue'),
            meta: { title: '日志审计', icon: 'log' }
          }
        ]
      },
      {
        path: '/colony-manage',
        name: '集群管理',
        redirect: '/colony-manage/list',
        meta: { title: '集群管理', icon: 'colony', rightSide: true },
        children: [
          {
            path: 'list',
            name: '集群列表管理',
            component: () => import('../views/cluster/ClusterList.vue'),
            meta: { title: '集群管理', icon: 'cluster' },
            // 添加别名，使得/cluster/list和/colony-manage/list指向同一个组件
            alias: '/cluster/list'
          },
          {
            path: 'storage',
            name: '存储库管理',
            component: () => import('../views/cluster/StorageManage.vue'),
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
        path: '/user-manage',
        name: '用户管理',
        component: () => import('../views/user/UserManage.vue'),
        meta: { title: '用户管理', icon: 'user_manager', rightSide: true }
      }
    ]
  },
  // 添加一个额外的cluster路径作为别名
  {
    path: '/cluster',
    redirect: '/colony-manage/list',
    children: [
      {
        path: '/cluster/list',
        component: () => import('../views/cluster/ClusterList.vue'),
      },
      {
        path: '/cluster/storage',
        redirect: '/colony-manage/storage'
      },
      {
        path: '/cluster/framework',
        redirect: '/colony-manage/framework'
      }
    ]
  },
  // 修正登录路由路径
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'), // 使用正确的登录组件
    meta: { title: '登录' }
  },
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

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置标题
  document.title = `${to.meta.title} | Noah大数据平台` || 'Noah大数据平台'
  
  // 判断是否需要登录权限
  const publicPaths = ['/login'] // 移除 '/login-debug'
  if (!publicPaths.includes(to.path)) {
    if (checkAuthorization()) {
      next() // 已登录，允许访问
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } }) // 未登录，跳转到登录页面
    }
  } else {
    // 如果是访问登录页面且已登录，重定向到首页
    if (to.path === '/login' && checkAuthorization()) {
      next({ path: '/' })
    } else {
      next() // 未登录，允许访问登录页面
    }
  }
})

export default router