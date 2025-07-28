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
      // 集群管理模块 - 使用正确的嵌套路由结构
      {
        path: 'cluster',
        component: {
          template: `
            <div class="cluster-layout p-6">
              <div class="mb-6 bg-gray-50 rounded-lg p-4 border border-gray-200">
                <h2 class="text-xl font-bold mb-2 text-blue-700">集群管理 (父级路由组件)</h2>
                <p class="text-gray-600 mb-4">当前路径: <span class="font-mono bg-blue-50 px-2 py-0.5 rounded">{{ $route.path }}</span></p>
                <div class="flex gap-3 mb-4">
                  <button @click="$router.push('/cluster')" class="px-3 py-1.5 text-sm bg-blue-100 hover:bg-blue-200 rounded">首页</button>
                  <button @click="$router.push('/cluster/list')" class="px-3 py-1.5 text-sm bg-blue-100 hover:bg-blue-200 rounded">列表</button>
                  <button @click="$router.push('/cluster/storage')" class="px-3 py-1.5 text-sm bg-blue-100 hover:bg-blue-200 rounded">存储</button>
                  <button @click="$router.push('/cluster/framework')" class="px-3 py-1.5 text-sm bg-blue-100 hover:bg-blue-200 rounded">框架</button>
                </div>
              </div>
              
              <!-- 子路由内容 -->
              <div class="bg-white rounded-lg shadow-md">
                <!-- 嵌套路由视图 -->
                <router-view></router-view>
              </div>
            </div>
          `
        },
        children: [
          {
            path: '',
            name: 'ClusterList',
            component: () => {
              console.log('[Router Debug] 加载组件: ClusterTest.vue');
              return import('../views/SimpleTest.vue')
                .then(module => {
                  console.log('[Router Debug] 组件加载成功: SimpleTest.vue');
                  return module;
                })
                .catch(error => {
                  console.error('[Router Error] 加载组件失败: SimpleTest.vue', error);
                  throw error;
                });
            },
            meta: {
              title: '集群列表(测试)',
              icon: 'cluster'
            }
          },
          // 显式添加list路径，指向与空路径相同的组件
          {
            path: 'list',
            name: 'ClusterListExplicit',
            component: () => {
              console.log('[Router Debug] 加载组件(list): SimpleTest.vue');
              return import('../views/SimpleTest.vue')
                .then(module => {
                  console.log('[Router Debug] 组件加载成功(list): SimpleTest.vue');
                  return module;
                })
                .catch(error => {
                  console.error('[Router Error] 加载组件失败(list): SimpleTest.vue', error);
                  throw error;
                });
            },
            meta: {
              title: '集群列表(测试-list)',
              icon: 'cluster'
            }
          },
          {
            path: 'storage',
            name: 'StorageManagement',
            component: () => {
              console.log('[Router Debug] 加载组件: SimpleTest.vue (storage)');
              return import('../views/SimpleTest.vue')
                .then(module => {
                  console.log('[Router Debug] 组件加载成功: SimpleTest.vue (storage)');
                  return module;
                })
                .catch(error => {
                  console.error('[Router Error] 加载组件失败: SimpleTest.vue (storage)', error);
                  throw error;
                });
            },
            meta: {
              title: '存储管理(测试)',
              parent: 'cluster'
            }
          },
          {
            path: 'framework',
            name: 'FrameworkManagement',
            component: () => {
              console.log('[Router Debug] 加载组件: SimpleTest.vue (framework)');
              return import('../views/SimpleTest.vue')
                .then(module => {
                  console.log('[Router Debug] 组件加载成功: SimpleTest.vue (framework)');
                  return module;
                })
                .catch(error => {
                  console.error('[Router Error] 加载组件失败: SimpleTest.vue (framework)', error);
                  throw error;
                });
            },
            meta: {
              title: '框架管理(测试)',
              parent: 'cluster'
            }
          }
          // 集群管理内部404捕获 - 放在子路由内的最后位置
          ,{
            path: ':pathMatch(.*)*',
            component: {
              template: `
                <div class="p-8 bg-white rounded-lg shadow text-center">
                  <h1 class="text-4xl font-bold text-red-500 mb-4">404 - 集群路径未找到</h1>
                  <p class="text-gray-600 mb-6">您访问的集群管理路径不存在: {{ $route.path }}</p>
                  <div class="bg-yellow-50 p-4 rounded-lg mb-6 text-left">
                    <h3 class="font-medium mb-2">路由信息:</h3>
                    <p class="mb-1"><strong>路径:</strong> {{ $route.path }}</p>
                    <p class="mb-1"><strong>参数:</strong> {{ JSON.stringify($route.params) }}</p>
                    <p class="mb-1"><strong>匹配项数量:</strong> {{ $route.matched.length }}</p>
                  </div>
                  <button 
                    @click="$router.push('/cluster')" 
                    class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                  >
                    返回集群管理
                  </button>
                </div>
              `
            },
            meta: {
              title: '未找到集群页面',
            }
          }
        ],
        meta: {
          title: '集群管理',
          icon: 'cluster'
        }
      },
      // 服务管理模块
      {
        path: 'service',
        name: 'ServiceList',
        component: () => import('../views/service/ServiceList.vue'),
        meta: {
          title: '服务管理',
          icon: 'service'
        }
      },
      // 主机管理模块
      {
        path: 'host',
        name: 'HostList',
        component: () => import('../views/host/HostList.vue'),
        meta: {
          title: '主机管理',
          icon: 'host'
        }
      },
      // 告警管理模块
      {
        path: 'alarm',
        name: 'AlarmList',
        component: () => import('../views/alarm/AlarmList.vue'),
        meta: {
          title: '告警管理',
          icon: 'alarm'
        }
      },
      // 系统管理模块
      {
        path: 'system',
        name: 'SystemLog',
        component: () => import('../views/system/SystemLog.vue'),
        meta: {
          title: '系统日志',
          icon: 'system'
        }
      },
      // 用户管理模块
      {
        path: 'user',
        name: 'UserList',
        component: () => import('../views/user/UserList.vue'),
        meta: {
          title: '用户管理',
          icon: 'user'
        }
      },
      // 路由测试页面 (开发环境)
      {
        path: 'route-test',
        name: 'RouteTest',
        component: () => import('../views/RouteTest.vue'),
        meta: {
          title: '路由测试',
          icon: 'test'
        }
      },
      // 内联测试组件
      {
        path: 'test-inline',
        name: 'TestInline',
        component: {
          template: `
            <div class="p-8 bg-white rounded-lg shadow">
              <h1 class="text-2xl font-bold mb-4">内联测试组件</h1>
              <p class="text-gray-600 mb-4">这是一个直接定义在路由配置中的组件</p>
              <div class="bg-blue-50 p-4 rounded-lg">
                <p>当前路径: {{ $route.path }}</p>
                <p>路由名称: {{ $route.name }}</p>
              </div>
            </div>
          `
        },
        meta: {
          title: '内联测试',
          icon: 'test'
        }
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
    redirect: '/cluster/list'  // 修改为重定向到/cluster/list而不是/cluster
  },
  // 注意：这里不需要从/cluster到/cluster/list的重定向，因为我们已经在嵌套路由中处理了空路径
  {
    path: '/colony-manage',
    redirect: '/cluster/list'  // 修改为重定向到/cluster/list而不是/cluster
  },
  // 告警管理相关重定向
  {
    path: '/alarm-manage/notification',
    redirect: '/alarm/notification'
  },
  {
    path: '/alarm-manage/group',
    redirect: '/alarm/group'
  },
  {
    path: '/alarm-manage/metric',
    redirect: '/alarm/metric'
  },
  {
    path: '/alarm-manage/help',
    redirect: '/alarm/help'
  },
  {
    path: '/alarm-manage',
    redirect: '/alarm'
  },
  // 系统管理相关重定向
  {
    path: '/system-manage/tenant',
    redirect: '/system/tenant'
  },
  {
    path: '/system-manage/user',
    redirect: '/system/user'
  },
  {
    path: '/system-manage/rack',
    redirect: '/system/rack'
  },
  {
    path: '/system-manage/label',
    redirect: '/system/label'
  },
  {
    path: '/system-manage/log',
    redirect: '/system/log'
  },
  {
    path: '/system-manage',
    redirect: '/system'
  },
  // 服务管理相关重定向
  {
    path: '/service-manage',
    redirect: '/service'
  },
  {
    path: '/service-manage/:clusterId',
    redirect: to => `/service?clusterId=${to.params.clusterId}`
  },
  // 主机管理相关重定向
  {
    path: '/host-manage',
    redirect: '/host'
  },
  // 用户管理相关重定向
  {
    path: '/user-manage',
    redirect: '/user'
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
  
  // 调试信息
  console.log(`[Router] 导航: ${from.path} -> ${to.path}`)
  console.log(`[Router] 路由匹配:`, to.matched.map(r => ({ 
    path: r.path, 
    regex: String(r.regex),
    components: Object.keys(r.components || {})
  })))
  console.log(`[Router] 匹配路由数量: ${to.matched.length}`)
  console.log(`[Router] 路由参数:`, to.params)
  console.log(`[Router] 路由查询:`, to.query)
  
  // 嵌套路由详细信息
  if (to.path.includes('/cluster')) {
    console.log('[Router] 集群路由匹配详情:');
    to.matched.forEach((record, index) => {
      console.log(`  [${index}] 路径: ${record.path}, 组件类型:`, typeof record.components?.default);
      console.log(`      完整路径: ${record.path}`);
      console.log(`      父路径: ${record.parent?.path || 'none'}`);
      console.log(`      组件定义: `, record.components?.default?.name || '未命名组件');
    });
    
    // 捕获错误路由
    if (to.matched.length === 0) {
      console.error('[Router] 严重错误: 集群路径未匹配任何路由:', to.path);
    } else if (to.matched.length === 1) {
      console.warn('[Router] 警告: 只匹配到一个路由，可能缺少子路由匹配:', to.path);
    }
  }
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} | Noah大数据平台` : 'Noah大数据平台'
  
  // 检查路由是否匹配
  if (to.matched.length === 0) {
    console.warn(`[Router] 未找到匹配的路由: ${to.path}`, {
      path: to.path,
      fullPath: to.fullPath,
      name: to.name
    })
    
    // 检查是否是集群管理或其子路由造成的 404
    if (to.path.startsWith('/cluster/') || 
        to.path.startsWith('/colony-manage/')) {
      console.log('[Router] 尝试重定向到集群管理页面')
      next('/cluster')
      return
    }
    
    // 只有在不是根路径时才重定向到首页
    if (to.path !== '/') {
      next('/')
      return
    }
  }
  
  // 检查是否需要认证
  const requiresAuth = to.meta.requiresAuth !== false
  
  if (requiresAuth) {
    if (!userStore.isLoggedIn) {
      console.log(`[Router] 未登录，重定向到登录页`)
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
      console.log(`[Router] 已登录用户访问登录页，重定向到首页`)
      next('/')
      return
    }
  }
  
  console.log(`[Router] 导航成功: ${to.path}`)
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