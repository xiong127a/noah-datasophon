import config from '@/config'
import {ADMIN} from '@/config/default'
import {formatFullPath} from '@/utils/i18n'
import {filterMenu} from '@/utils/authority-utils'
// getLocalSetting import removed
import deepClone from 'lodash.clonedeep'


const localSetting = {} // Local setting removed
const customTitlesStr = sessionStorage.getItem(process.env.VUE_APP_TBAS_TITLES_KEY)
const customTitles = (customTitlesStr && JSON.parse(customTitlesStr)) || []
let timer = null
export default {
  namespaced: true,
  state: {
    isMobile: false,
    animates: ADMIN.animates,
    palettes: ADMIN.palettes,
    pageMinHeight: 0,
    menuData: [],
    dashboardUrl: '', // 总览的地址
    // asyncRoutes: true,
    isCluster: '',
    serviceId: '',
    activatedFirst: undefined,
    clusterId: '',
    runningCluster: [],
    clusterSettingVisible: false,
    alarmManageVisible:true,
    customTitles,
    activatedFirstMenuKey: '',
    ...config,
    ...localSetting
  },
  getters: {
    runningCluster (state, getters, rootState) {
      if (!state.runningCluster) {
        try {
          // const runningCluster = localStorage.getItem('runningCluster')
          // state.runningCluster = runningCluster
        } catch (e) {
          console.error(e)
        }
      }
      return state.runningCluster
    },
    menuData(state, getters, rootState) {
      // if (state.filterMenu) {
      //   const {permissions, roles} = rootState.account
      //   return filterMenu(deepClone(state.menuData), permissions, roles)
      // }
      if (!state.menuData) {
        try {
          const menuData = localStorage.getItem('menuData')
          state.menuData = JSON.parse(menuData)
        } catch (e) {
          console.error(e)
        }
      }
      return state.menuData
    },
    clusterId(state, getters, rootState) {
      if (!state.clusterId) {
        try {
          const clusterId = localStorage.getItem('clusterId')
          state.clusterId = clusterId
        } catch (e) {
          console.error(e)
        }
      }
      return state.clusterId
    },
    isCluster(state, getters, rootState) {
      if (!state.isCluster) {
        try {
          const isCluster = localStorage.getItem('isCluster')
          state.isCluster = isCluster
          // 这个地方可以做定时去刷新的操作
        } catch (e) {
          console.error(e)
        }
      }
      return state.isCluster
    },
    firstMenu: state => {
      // 过滤掉不可见菜单、隐藏菜单和总览菜单
      return state.menuData.filter(item => 
        !item.meta.invisible && 
        !item.meta.hidden && 
        item.path !== 'overview'
      );
    },
    subMenu: state => {
      const activeKey = state.activatedFirstMenuKey || (state.menuData[0] && state.menuData[0].fullPath)
      const active = state.menuData.find(item => item.fullPath === activeKey)
      return active && active.children ? active.children : []
    }
  },
  actions: {
    getDashboardUrl ({dispatch, commit}) {
      const clusterId = localStorage.getItem('clusterId')
      this._vm.$axiosPost(global.API.getDashboardUrl, {clusterId}).then(res => {
        commit('setDashboardUrl', res.data)
        clearTimeout(timer)
        dispatch('getServiceList')
      })
    },
    getRunningClusterList ({dispatch, commit}) {
      this._vm.$axiosPost(global.API.runningClusterList, {}).then(async res => {
        let arr = [];
        
        // 获取每个集群的基本信息
        for (const item of res.data) {
          const clusterInfo = {
            label: item.clusterName,
            value: item.id
          };
          
          // 获取每个集群的详细信息
          try {
            const detailRes = await this._vm.$axiosGet(`/ddh/api/cluster/info/${item.id}`);
            if (detailRes.code === 200 && detailRes.data) {
              // 将详细信息合并到基本信息中
              clusterInfo.depType = detailRes.data.depType;
              clusterInfo.deployType = detailRes.data.deployType;
              clusterInfo.desc = detailRes.data.desc;
              clusterInfo.clusterFrame = detailRes.data.clusterFrame;
              clusterInfo.clusterState = detailRes.data.clusterState;
            }
          } catch (error) {
            console.error(`获取集群 ${item.id} 详细信息失败:`, error);
          }
          
          arr.push(clusterInfo);
        }
        
        console.log("获取的集群完整信息:", arr);
        commit('setRunningCluster', arr);
        dispatch('getDashboardUrl');
      });
    },
    getServiceList ({dispatch, commit}) {
      let menuData = JSON.parse(localStorage.getItem('menuData')) || []
      const isCluster = localStorage.getItem('isCluster')
      const clusterId = localStorage.getItem('clusterId')
      // 当进入到这个页面的时候去做一个定时刷新获取列表的功能
      if (clusterId && isCluster === 'isCluster') { 
        this._vm.$axiosPost(global.API.getServiceListByCluster, {
          clusterId: clusterId,
        }).then((res) => {
          // 查找服务管理菜单
          const serviceManageMenu = menuData.find(item => item.path === "service-manage")
          if (serviceManageMenu) {
            // 兼容处理原有结构
            serviceManageMenu.children = [];
            res.data.map((serviceItem) => {
              serviceManageMenu.children.push({
                name: serviceItem.serviceName,
                label:serviceItem.label,
                meta: {
                  notAlive: true,
                  isCluster: 'isCluster',
                  params: {
                    serviceId: serviceItem.id
                  },
                  obj: serviceItem,
                  authority: {
                    permission: "*",
                  },
                  permission: [{
                    permission: "*",
                  },
                  {
                    permission: "*",
                  },
                  ],
                },
                fullPath: `/service-manage/service-list/${serviceItem.id}`,
                path: `service-list/${serviceItem.id}`,
                component: () => import("@/pages/serviceManage/index"),
              });
            });
          } else {
            // 服务管理菜单不存在，直接保存服务列表
            localStorage.setItem('serviceList', JSON.stringify(res.data));
          }
          
          commit('setMenuData', menuData)
          timer = setTimeout(() => {
            dispatch('getServiceList')
          }, global.intervalTime)
        })
      }
    }
  },
  mutations: {
    showAlarmManageVisible(state,alarmManageVisible){
      state.alarmManageVisible = alarmManageVisible
    },
    setServiceId(state, serviceId) {
      state.serviceId = serviceId
    },
    showClusterSetting(state, clusterSettingVisible) {
      state.clusterSettingVisible = clusterSettingVisible
    },
    setRunningCluster (state, RunningCluster) {
      state.runningCluster = RunningCluster
      // localStorage.setItem('runningCluster', RunningCluster)
    },
    setDashboardUrl (state, dashboardUrl) {
      state.dashboardUrl = dashboardUrl
    },
    setIsCluster (state, isCluster) {
      state.isCluster = isCluster
      localStorage.setItem('isCluster', isCluster)
    },
    setClusterId (state, clusterId) {
      state.clusterId = clusterId
      localStorage.setItem('clusterId', clusterId)
    },
    setDevice (state, isMobile) {
      state.isMobile = isMobile
    },
    // setTheme mutation removed
    setLayout (state, layout) {
      state.layout = layout
    },
    setMultiPage (state, multiPage) {
      state.multiPage = multiPage
    },
    setAnimate (state, animate) {
      state.animate = animate
    },
    setWeekMode(state, weekMode) {
      state.weekMode = weekMode
    },
    setFixedHeader(state, fixedHeader) {
      state.fixedHeader = fixedHeader
    },
    setFixedSideBar(state, fixedSideBar) {
      state.fixedSideBar = fixedSideBar
    },
    setLang(state, lang) {
      state.lang = lang
    },
    setHideSetting(state, hideSetting) {
      state.hideSetting = hideSetting
    },
    correctPageMinHeight(state, minHeight) {
      state.pageMinHeight += minHeight
    },
    setMenuData(state, menuData) {
      state.menuData = menuData
      // 在这个地方去做操作，给menuData数据赋值
      localStorage.setItem('menuData', JSON.stringify(menuData))
    },
    setAsyncRoutes(state, asyncRoutes) {
      state.asyncRoutes = asyncRoutes
    },
    setPageWidth(state, pageWidth) {
      state.pageWidth = pageWidth
    },
    setActivatedFirst(state, key) {
      state.activatedFirstMenuKey = key
    },
    setFixedTabs(state, fixedTabs) {
      state.fixedTabs = fixedTabs
    },
    setCustomTitle(state, {path, title}) {
      if (title) {
        const obj = state.customTitles.find(item => item.path === path)
        if (obj) {
          obj.title = title
        } else {
          state.customTitles.push({path, title})
        }
        sessionStorage.setItem(process.env.VUE_APP_TBAS_TITLES_KEY, JSON.stringify(state.customTitles))
      }
    }
  }
}
