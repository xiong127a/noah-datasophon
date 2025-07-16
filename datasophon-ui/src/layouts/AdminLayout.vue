<template>
  <div class="cdh-layout">
    <admin-header
      :firstMenu="firstMenu"
      :activeFirstMenuKey="activeFirstMenuKey"
      @firstMenuSelect="onFirstMenuSelect"
      @routeChanged="onRouteChanged"
      :class="['cdh-header', {'fixed-tabs': fixedTabs, 'fixed-header': fixedHeader, 'multi-page': multiPage}]"
    />
    <div class="cdh-main">
      <!-- 二级菜单已改为从顶部下拉，移除左侧菜单 -->
      <div class="cdh-content full">
        <!-- 删除面包屑导航，更好利用空间 -->
        <div style="position: relative">
          <slot></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AdminHeader from './header/AdminHeader'
import { mapState, mapGetters, mapMutations } from 'vuex'
import { getI18nKey } from '@/utils/routerUtil'

export default {
  name: 'AdminLayout',
  components: { AdminHeader },
  data () {
    return {
      collapsed: false,
      activeFirstMenuKey: ''
    }
  },
  computed: {
    ...mapState('setting', ['fixedTabs', 'fixedHeader', 'multiPage']),
    ...mapGetters('setting', ['firstMenu', 'subMenu']),
    breadcrumb() {
      let page = this.page
      let breadcrumb = page && page.breadcrumb
      if (breadcrumb) {
        let i18nBreadcrumb = []
        breadcrumb.forEach(item => {
          i18nBreadcrumb.push(this.$t(item))
        })
        return i18nBreadcrumb
      } else {
        return this.getRouteBreadcrumb()
      }
    },
  },
  methods: {
    ...mapMutations('setting', ['setActivatedFirst']),
    onRouteChanged(path) {
      console.log('路由已变更到:', path);
      // 手动刷新视图
      this.$nextTick(() => {
        this.$forceUpdate();
      });
    },
    onFirstMenuSelect(key) {
      this.activeFirstMenuKey = key
      this.setActivatedFirst(key)
      
      // 对于service-manage（主页）路径做特殊处理
      if (key === '/service-manage') {
        // 直接跳转到主页而不是子菜单
        if (this.$route.path !== '/service-manage') {
          this.$router.push('/service-manage').catch(err => {
            if (err.name !== 'NavigationDuplicated') {
              throw err;
            }
          });
        }
        return;
      }
      
      // 跳转到第一个可见二级菜单页面，若无则跳转到一级菜单自身
      const firstMenu = this.firstMenu.find(item => item.fullPath === key)
      if (firstMenu) {
        let targetPath = firstMenu.fullPath
        if (firstMenu.children && firstMenu.children.length > 0) {
          const findFirstLeaf = (children) => {
            for (const child of children) {
              if (child.meta && child.meta.invisible) continue
              if (!child.children || child.children.length === 0) {
                return child
              } else {
                const leaf = findFirstLeaf(child.children)
                if (leaf) return leaf
              }
            }
            return null
          }
          const firstLeaf = findFirstLeaf(firstMenu.children)
          if (firstLeaf && firstLeaf.fullPath) {
            targetPath = firstLeaf.fullPath
          }
        }
        // 避免重复导航
        if (this.$route.path !== targetPath) {
          this.$router.push(targetPath).catch(err => {
            // 忽略导航重复错误
            if (err.name !== 'NavigationDuplicated') {
              throw err;
            }
          })
        }
      }
    },
    getRouteBreadcrumb() {
      let routes = this.$route.matched
      const path = this.$route.path
      let breadcrumb = []
      routes.filter(item => path.includes(item.path))
        .forEach(route => {
          const path = route.path.length === 0 ? '/home' : route.path
          breadcrumb.push(this.$t(getI18nKey(path)))
        })
      let pageTitle = this.page && this.page.title
      if (this.customTitle || pageTitle) {
        breadcrumb[breadcrumb.length - 1] = this.customTitle || pageTitle
      }
      breadcrumb.shift()
      // 去匹配动态的路由名称
      if (this.$route && this.$route.params && this.$route.params.serviceId) {
        let name = ''
        const serviceId = this.$route.params.serviceId || ''
        const menuData = JSON.parse(localStorage.getItem('menuData')) || []
        const arr = menuData.filter(item => item.path === 'service-manage')
        if (arr.length > 0) {
          arr[0].children.map(item => {
            if (item.meta.params.serviceId == serviceId) name = item.name
          })
          breadcrumb.push(name)
        }
      }
      return breadcrumb
    }
  },
  created() {
    if (this.firstMenu && this.firstMenu.length > 0) {
      this.activeFirstMenuKey = this.firstMenu[0].fullPath
    }
  }
}
</script>

<style lang="less">
.cdh-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100vw;
  max-width: 100%;
  overflow-x: hidden;

  .cdh-header {
    background: #fff;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
    position: relative;
    z-index: 10;
    
    /* 隐藏总览标签 */
    .tabs-view-content {
      a[href="/service-manage"],
      a[href="/service-manage/"],
      div.tab:has(> div.title:contains("总览")),
      div:has(> span:contains("总览")) {
        display: none !important;
      }
    }
  }

  .cdh-main {
    flex: 1;
    display: flex;
    width: 100%;
    max-width: 100%;

    .cdh-content {
      position: relative;
      flex: 1;
      width: 100%;
      
      &.full {
        margin: 0;
        padding: 0;
        overflow: visible !important; /* 确保内容区域不产生自己的滚动条 */
      }
    }
  }
}

/* 确保页面宽度固定并禁用内部滚动 */
#app, .router-view {
  width: 100%;
  height: 100%;
  overflow: visible !important;
}

/* 移除所有内部滚动，只在最外层显示滚动条 */
.cdh-content, .service-detail, .ant-tabs-content, .ant-tabs-tabpane {
  overflow: visible !important;
}

/* 保留表格滚动行为 */
.ant-table-body {
  overflow: auto !important;
}
</style>
