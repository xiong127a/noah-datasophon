<template>
  <div class="cdh-layout">
    <admin-header
      :firstMenu="firstMenu"
      :activeFirstMenuKey="activeFirstMenuKey"
      @firstMenuSelect="onFirstMenuSelect"
      :class="['cdh-header', {'fixed-tabs': fixedTabs, 'fixed-header': fixedHeader, 'multi-page': multiPage}]"
    />
    <div class="cdh-main">
      <!-- 二级菜单已改为从顶部下拉，移除左侧菜单 -->
      <div class="cdh-content full">
        <div class="breadcrumb" style="background: #f5f6fa;">
          <a-breadcrumb>
            <a-breadcrumb-item :key="index" v-for="(item, index) in breadcrumb">
              <span>{{item}}</span>
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>
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
    onFirstMenuSelect(key) {
      this.activeFirstMenuKey = key
      this.setActivatedFirst(key)
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
        this.$router.push(targetPath)
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

<style lang="less" scoped>
.cdh-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6fa;
}
.cdh-main {
  display: flex;
  flex: 1;
  height: 100%;
}
.cdh-side-menu {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  height: 100vh;
}
.cdh-content {
  flex: 1;
  padding: 24px 32px 0 32px;
  background: #f5f6fa;
  min-height: 100vh;
  overflow-y: auto;
}
.cdh-content.full {
  padding-left: 0;
  width: 100%;
}
.breadcrumb {
  margin-bottom: 16px;
}
</style>
