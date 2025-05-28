import TabsView from '@/layouts/tabs/TabsView'
import BlankView from '@/layouts/BlankView'
import PageView from '@/layouts/PageView'


// 路由配置
const options = {
  routes: [
    {
      path: '/login',
      name: '登录页',
      component: () => import('@/pages/login')
    },
    {
      path: '*',
      name: '404',
      component: () => import('@/pages/exception/404'),
    },
    {
      path: '/403',
      name: '403',
      component: () => import('@/pages/exception/403'),
    },
    {
      path: '/',
      name: '首页',
      component: TabsView,
      redirect: '/service-manage',
      children: [
        {
          path: 'service-manage',
          name: '主页',
          meta: { 
            notAlive: true,
            icon: 'home'
          },
          component: () => import('@/pages/serviceManage/ServiceLayout'),
          children: [
            {
              path: '',
              name: '服务总览',
              component: () => import('@/pages/serviceManage/ServiceOverview'),
            },
            {
              path: 'service-list/:serviceId',
              name: '服务详情',
              meta: {
                notAlive: true,
                params: {
                  serviceId: '',
                },
              },
              component: () => import('@/pages/serviceManage/index'),
            }
          ]
        },
        {
          path: 'colony-manage',
          name: '集群管理',
          meta: {
            icon: 'cluster',
            isCluster: '',
          },
          component: PageView,
          children: [
            {
              path: 'colony-list',
              meta: {
                notAlive: true,
                icon: 'cluster',
              },
              name: '集群管理',
              component: () => import('@/pages/colonyManage/list'),
            },
            {
              path: 'colony-parcel',
              name: '存储库管理',
              meta: { icon: 'wenjian' },
              component: () => import('@/pages/colonyManage/parcel'),
            },
            {
              path: 'colony-frame',
              name: '集群框架',
              meta: { icon: 'shangwutubiao-' },
              component: () => import('@/pages/colonyManage/frame'),
            },
          ],
        },
        {
          path: 'security-center',
          name: '用户管理',
          meta: {
            icon: 'user',
            isCluster: '',
          },
          component: PageView,
          children: [
            {
              path: 'user',
              name: '用户管理',
              meta: { icon: 'user' },
              component: () => import('@/pages/securityCenter/user'),
            },
          ],
        },
        {
          path: 'host-manage',
          name: '主机管理',
          meta: {
            icon: 'host',
            isCluster: 'isCluster',
          },
          component: () => import('@/pages/hostManage/index'),
          children: [],
        },
        {
          path: 'alarm-manage',
          name: '告警管理',
          meta: {
            icon: 'gaojing',
            isCluster: 'isCluster',
          },
          component: PageView,
          children: [
            {
              path: 'notice',
              meta: {
                notAlive: false,
                icon: 'gaojing',
              },
              name: '通知组管理',
              label: '通知组管理',
              component: () => import('@/pages/alarmManage/notice'),
            },
            {
              path: 'group',
              meta: {
                notAlive: false,
                icon: 'gaojing',
              },
              name: '告警组管理',
              label: '告警组管理',
              component: () => import('@/pages/alarmManage/group'),
            },
            {
              path: 'metric',
              meta: {
                notAlive: true,
                icon: 'yanjiuzhulu',
              },
              name: '告警指标管理',
              label: '告警指标管理',
              component: () => import('@/pages/alarmManage/metric'),
            },
            {
              path: 'help',
              meta: {
                notAlive: false,
                icon: 'zhuye',
              },
              name: '使用帮助',
              label: '使用帮助',
              component: () => import('@/pages/alarmManage/helpInfo/alarmManagementHelp'),
            },
            // {
            //   path: 'user',
            //   name: '租户管理',
            //   label: '租户管理',
            //   component: () => import('@/pages/systemCenter/user'),
            // },
          ],
        },
        {
          path: 'system-center',
          name: '系统管理',
          label: '系统管理',
          meta: {
            icon: 'system-icon',
            isCluster: 'isCluster',
          },
          component: PageView,
          children: [
            {
              path: 'tenant',
              name: '租户管理',
              label: '租户管理',
              meta: { icon: 'user' },
              component: () => import('@/pages/systemCenter/tenant/index'),
            },
            {
              path: 'user',
              name: '用户管理',
              label: '用户管理',
              meta: { icon: 'user' },
              component: () => import('@/pages/systemCenter/user'),
            },
            {
              path: 'frame',
              name: '机架管理',
              label: '机架管理',
              meta: { icon: 'shangwutubiao-' },
              component: () => import('@/pages/systemCenter/frame/index'),
            },
            {
              path: 'tag',
              name: '标签管理',
              label: '标签管理',
              meta: { icon: 'tag' },
              component: () => import('@/pages/systemCenter/tag/index'),
            },
            {
              path: 'log',
              name: '日志审计',
              label: '日志审计',
              meta: { icon: 'wenjian' },
              component: () => import('@/pages/systemCenter/log/index'),
            },
          ],
        },
      ],
    },
  ],
}

export default options
