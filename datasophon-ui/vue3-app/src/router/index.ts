import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '../stores/user'

// 布局组件
import MainLayout from '../layouts/MainLayout.vue'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false,
      layout: 'blank'
    }
  },
  {
    path: '/',
    component: MainLayout,
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: {
          title: '首页',
          icon: 'home'
        }
      },
      {
        path: 'home',
        redirect: '/'
      },
      // 集群管理模块
      {
        path: 'cluster',
        name: 'ClusterManagement',
        meta: {
          title: '集群管理',
          icon: 'cluster'
        },
        children: [
          {
            path: '',
            name: 'ClusterList',
            component: () => import('../views/cluster/ClusterList.vue'),
            meta: {
              title: '集群列表'
            }
          },
          {
            path: 'storage',
            name: 'StorageManagement',
            component: () => import('../views/repository/ParcelList.vue'),
            meta: {
              title: '存储管理'
            }
          },
          {
            path: 'framework',
            name: 'FrameworkManagement',
            component: () => import('../views/cluster/FrameworkManage.vue'),
            meta: {
              title: '框架管理'
            }
          }
        ]
      },
      // 服务管理模块
      {
        path: 'service',
        name: 'ServiceManagement',
        meta: {
          title: '服务管理',
          icon: 'service'
        },
        children: [
          {
            path: '',
            name: 'ServiceList',
            component: () => import('../views/service/ServiceList.vue'),
            meta: {
              title: '服务列表'
            }
          }
        ]
      },
      // 主机管理模块
      {
        path: 'host',
        name: 'HostManagement',
        meta: {
          title: '主机管理',
          icon: 'host'
        },
        children: [
          {
            path: '',
            name: 'HostList',
            component: () => import('../views/host/HostList.vue'),
            meta: {
              title: '主机列表'
            }
          }
        ]
      },
      // 告警管理模块
      {
        path: 'alarm',
        name: 'AlarmManagement',
        meta: {
          title: '告警管理',
          icon: 'alarm'
        },
        children: [
          {
            path: '',
            name: 'AlarmList',
            component: () => import('../views/alarm/AlarmList.vue'),
            meta: {
              title: '告警列表'
            }
          }
        ]
      },
      // 系统管理模块
      {
        path: 'system',
        name: 'SystemManagement',
        meta: {
          title: '系统管理',
          icon: 'system'
        },
        children: [
          {
            path: '',
            name: 'SystemSettings',
            component: () => import('../views/system/SystemSettings.vue'),
            meta: {
              title: '系统设置'
            }
          },
          {
            path: 'log',
            name: 'LogManagement',
            component: () => import('../views/system/LogManage.vue'),
            meta: {
              title: '日志管理'
            }
          }
        ]
      },
      // 用户管理模块
      {
        path: 'user',
        name: 'UserManagement',
        meta: {
          title: '用户管理',
          icon: 'user'
        },
        children: [
          {
            path: '',
            name: 'UserList',
            component: () => import('../views/user/UserList.vue'),
            meta: {
              title: '用户列表'
            }
          }
        ]
      }
    ]
  },
  // 兼容旧路由的重定向
  {
    path: '/colony-manage/storage',
    redirect: '/cluster/storage'
  },
  {
    path: '/colony-manage/framework',
    redirect: '/cluster/framework'
  },
  {
    path: '/colony-manage/list',
    redirect: '/cluster'
  },
  // 404页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: {
      title: '页面未找到',
      requiresAuth: false
    }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} | Noah大数据平台` : 'Noah大数据平台'
  
  // 检查是否需要认证
  const requiresAuth = to.meta.requiresAuth !== false
  
  if (requiresAuth) {
    if (!userStore.isLoggedIn) {
      // 未登录，重定向到登录页
      next({
        path: '/login',
        query: to.path !== '/' ? { redirect: to.fullPath } : undefined
      })
      return
    }
  } else {
    // 不需要认证的页面，如果已登录且访问登录页，重定向到首页
    if (to.path === '/login' && userStore.isLoggedIn) {
      next('/')
      return
    }
  }
  
  next()
})

// 全局后置守卫
router.afterEach((to, from) => {
  // 可以在这里添加页面访问统计等逻辑
})

// 路由错误处理
router.onError((error) => {
  console.error('路由错误:', error)
})

export default router