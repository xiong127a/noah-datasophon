import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'HomePage',
        component: () => import('@/views/Home.vue'),
        meta: {
          title: '首页',
          icon: 'home'
        }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue')
  },
  // 添加集群管理相关路由
  {
    path: '/cluster',
    name: 'ClusterManage',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: 'list',
        name: 'ClusterList',
        component: () => import('@/views/cluster/ClusterList.vue'),
        meta: {
          title: '集群管理',
          icon: 'colony'
        }
      }
    ]
  },
  // 添加与旧版兼容的集群管理路由
  {
    path: '/colony-manage',
    name: 'ColonyManage',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: 'list',
        name: 'ColonyList',
        component: () => import('@/views/cluster/ClusterList.vue'),
        meta: {
          title: '集群管理',
          icon: 'colony'
        }
      }
    ]
  },
  {
    path: '/service-manage',
    name: 'ServiceManage',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'ServiceOverview',
        component: () => import('@/views/service/ServiceOverview.vue'),
        meta: {
          title: '服务管理',
          icon: 'service'
        }
      }
    ]
  },
  {
    path: '/alarm',
    name: 'AlarmManage',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: 'group',
        name: 'AlarmGroup',
        component: () => import('@/views/alarm/AlarmGroup.vue'),
        meta: {
          title: '告警组管理',
          icon: 'alarm-group'
        }
      },
      {
        path: 'metric',
        name: 'AlarmMetric',
        component: () => import('@/views/alarm/AlarmMetric.vue'),
        meta: {
          title: '告警指标管理',
          icon: 'alarm-metric'
        }
      },
      {
        path: 'notice',
        name: 'AlarmNotice',
        component: () => import('@/views/alarm/AlarmNotice.vue'),
        meta: {
          title: '告警通知管理',
          icon: 'alarm-notice'
        }
      },
      {
        path: 'help',
        name: 'AlarmHelp',
        component: () => import('@/views/alarm/AlarmHelp.vue'),
        meta: {
          title: '使用帮助',
          icon: 'help'
        }
      }
    ]
  },
  {
    path: '/system',
    name: 'SystemManage',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/system/UserManage.vue'),
        meta: {
          title: '用户管理',
          icon: 'user'
        }
      },
      {
        path: 'tenant',
        name: 'TenantManage',
        component: () => import('@/views/system/TenantManage.vue'),
        meta: {
          title: '租户管理',
          icon: 'tenant'
        }
      },
      {
        path: 'log',
        name: 'LogManage',
        component: () => import('@/views/system/LogManage.vue'),
        meta: {
          title: '日志管理',
          icon: 'log'
        }
      },
      {
        path: 'tag',
        name: 'TagManage',
        component: () => import('@/views/system/TagManage.vue'),
        meta: {
          title: '标签管理',
          icon: 'tag'
        }
      },
      {
        path: 'frame',
        name: 'FrameManage',
        component: () => import('@/views/system/FrameManage.vue'),
        meta: {
          title: '框架管理',
          icon: 'frame'
        }
      }
    ]
  },
  {
    path: '/host',
    name: 'HostManage',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'HostList',
        component: () => import('@/views/host/HostList.vue'),
        meta: {
          title: '主机管理',
          icon: 'host'
        }
      }
    ]
  },
  // 404页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 获取token
  const token = localStorage.getItem('token')
  
  // 判断是否需要登录
  if (to.name !== 'Login' && !token) {
    next({ name: 'Login' })
  } else {
    next()
  }
})

export default router 