<template>
  <a-layout-header :class="[headerTheme, 'admin-header', 'cdh-header']" style="background: #fff; border-bottom: 1px solid #e0e0e0; height: 56px;">
    <div class="cdh-header-inner">
      <!-- 左侧Logo和常用菜单 -->
      <div class="left-section">
        <div class="logo" @click="goToHome">
          <img src="@/assets/img/logo.png" alt style="height:32px; margin-right:8px;"/>
          <h1 style="color:#1976d2; font-size:20px; font-weight:600; margin:0;">{{systemName}}</h1>
        </div>
        <div class="cdh-header-menu">
          <ul class="cdh-top-menu">
            <li v-for="item in regularMenus" :key="item.fullPath" :class="{'active': item.fullPath === activeFirstMenuKey}" @click="onLeftMenuClick(item)" @mouseenter="handleMenuEnter(item)" @mouseleave="handleMenuLeave(item)">
              <span v-if="item.meta && item.meta.icon">
                <svg-icon :icon-class="item.meta.icon" style="margin-right:6px;"/>
              </span>
              <span v-if="item.path === 'service-manage'">主页</span>
              <span v-else>{{item.name}}</span>
              <span class="dropdown-icon" v-if="hasChildren(item) && item.path !== 'service-manage'">
                <a-icon type="caret-down" />
              </span>
              
              <ul v-show="hoveredMenu === item.fullPath && item.path !== 'service-manage'" class="cdh-sub-dropdown" v-if="hasChildren(item)">
                <li v-for="subItem in getMenuChildren(item)" :key="subItem.fullPath" :class="{'active': $route.path.includes(subItem.fullPath)}">
                  <a :href="'#' + subItem.fullPath" @click.stop="onSubMenuSelect(subItem, $event)">
                    <svg-icon :icon-class="subItem.meta && subItem.meta.icon ? subItem.meta.icon : 'menu-default'" style="margin-right:6px;"/>
                    {{subItem.name || subItem.label}}
                  </a>
                </li>
              </ul>
            </li>
          </ul>
        </div>
      </div>

      <!-- 右侧管理菜单和功能按钮 -->
      <div class="right-section">
        <!-- 右侧管理菜单 -->
        <ul class="admin-menu-list">
          <li v-for="item in adminMenus" :key="item.fullPath" :class="{'active': item.fullPath === activeFirstMenuKey}" @mouseenter="handleMenuEnter(item)" @mouseleave="handleMenuLeave(item)">
            <div class="menu-content" @click="onLeftMenuClick(item)">
              <template v-if="item.path === 'colony-manage'">
                <svg-icon icon-class="colony" style="margin-right:6px;"/>
              </template>
              <template v-else-if="item.path === 'security-center'">
                <svg-icon icon-class="user_manager" style="margin-right:6px;"/>
              </template>
              <span>{{item.name}}</span>
              <span class="dropdown-icon">
                <a-icon type="caret-down" />
              </span>
            </div>
            <ul v-show="hoveredMenu === item.fullPath" class="admin-sub-dropdown" v-if="hasChildren(item)">
              <li v-for="subItem in getMenuChildren(item)" :key="subItem.fullPath" :class="{'active': $route.path.includes(subItem.fullPath)}">
                <a :href="'#' + subItem.fullPath" @click.stop="onSubMenuSelect(subItem, $event)">
                  <svg-icon :icon-class="subItem.meta && subItem.meta.icon ? subItem.meta.icon : 'menu-default'" style="margin-right:6px;"/>
                  {{subItem.name || subItem.label}}
                </a>
              </li>
            </ul>
          </li>
        </ul>
        <!-- 右侧功能按钮 -->
        <div class="action-buttons">
          <!-- 集群切换下拉菜单 -->
          <a-dropdown class="cluster-selector">
            <span class="action-item">
              {{ currentCluster.name || 'bdp' }}
              <span class="dropdown-icon">
                <a-icon type="caret-down" />
              </span>
            </span>
            <a-menu slot="overlay">
              <a-menu-item v-for="item in runningCluster" :key="item.value" @click="changeCluster({key: item.value})">
                {{ item.label }}
              </a-menu-item>
            </a-menu>
          </a-dropdown>
          <alarm-manage v-if="isCluster === 'isCluster'" />
          <cluster-setting v-if="isCluster === 'isCluster'" />
          <header-avatar class="header-item" />
        </div>
      </div>
    </div>
  </a-layout-header>
</template>

<script>
import HeaderAvatar from "./HeaderAvatar";
// import IMenu from "@/components/menu/menu";
import ClusterSetting from './clusterSetting';
import AlarmManage from './alarmManage.vue'
import { mapState, mapMutations, mapGetters } from "vuex";
export default {
  name: "AdminHeader",
  components: { HeaderAvatar, ClusterSetting, AlarmManage },
  props: {
    firstMenu: Array,
    activeFirstMenuKey: String
  },
  data() {
    return {
      langList: [
        { key: "CN", name: "简体中文", alias: "简体" },
        // {key: 'HK', name: '繁體中文', alias: '繁體'},
        { key: "US", name: "English", alias: "English" },
      ],
      searchActive: false,
      hoveredMenu: '', // 当前鼠标悬浮的菜单
      menuTimeouts: {}, // 存储菜单延迟关闭的定时器
    };
  },
  computed: {
    ...mapGetters('setting', ['isCluster', 'runningCluster', 'clusterId']),
    ...mapState("setting", [
      "theme",
      "isMobile",
      "layout",
      "systemName",
      "lang",
      "pageWidth",
    ]),
    headerTheme() {
      if (
        this.layout == "side" &&
        this.theme.mode == "dark" &&
        !this.isMobile
      ) {
        return "light";
      }
      return this.theme.mode;
    },
    langAlias() {
      let lang = this.langList.find((item) => item.key == this.lang);
      return lang.alias;
    },
    menuWidth() {
      const { layout, searchActive } = this;
      const headWidth = layout === "head" ? "100% - 188px" : "100%";
      const extraWidth = searchActive ? "600px" : "400px";
      return `calc(${headWidth} - ${extraWidth})`;
    },
    currentCluster () {
      let arr = this.runningCluster.filter(item => item.value === Number(this.clusterId)) || []
      return {
        name: arr.length > 0 ? arr[0].label : '',
        clusterId: this.clusterId
      }
    },
    regularMenus() {
      if (!this.firstMenu) return [];
      // 排除集群管理、用户管理
      const excludePaths = ['colony-manage', 'security-center'];
      return this.firstMenu.filter(item => !excludePaths.includes(item.path));
    },
    adminMenus() {
      if (!this.firstMenu) return [];
      // 仅包含集群管理和用户管理，确保服务管理不会包含在内
      const adminMenuPaths = ['colony-manage', 'security-center'];
      return this.firstMenu.filter(item => adminMenuPaths.includes(item.path));
    },
  },
  methods: {
    toggleCollapse() {
      this.$emit("toggleCollapse");
    },
    onSelect(obj) {
      this.$emit("menuSelect", obj);
    },
    // 处理菜单悬浮
    handleMenuEnter(item) {
      // 清除任何现有的关闭定时器
      if (this.menuTimeouts[item.fullPath]) {
        clearTimeout(this.menuTimeouts[item.fullPath]);
        delete this.menuTimeouts[item.fullPath];
      }
      
      // 设置当前悬浮菜单
      this.hoveredMenu = item.fullPath;
    },
    // 处理菜单离开
    handleMenuLeave(item) {
      // 设置延迟关闭，以便用户有时间移动到子菜单
      this.menuTimeouts[item.fullPath] = setTimeout(() => {
        if (this.hoveredMenu === item.fullPath) {
          this.hoveredMenu = '';
        }
      }, 300);
    },
    // 处理菜单点击
    onLeftMenuClick(item) {
      // 如果是主页菜单，使用与logo点击相同的处理方法
      if (item.path === 'service-manage') {
        this.goToHome(); // 直接调用goToHome方法确保行为一致
        this.$emit('firstMenuSelect', '/service-manage'); // 确保使用完整路径作为参数
        return;
      }
      
      // 如果没有子菜单，直接发出选择事件
      if (!this.hasChildren(item)) {
        this.$emit('firstMenuSelect', item.fullPath);
      } else {
        // 如果有子菜单，只处理下拉菜单的展开/折叠
        if (this.hoveredMenu !== item.fullPath) {
          // 如果下拉菜单未展开，则展开
          this.hoveredMenu = item.fullPath;
        } else {
          // 如果下拉菜单已展开，则折叠
          this.hoveredMenu = '';
        }
      }
    },
    
    // 检查菜单项是否有子菜单
    hasChildren(item) {
      // 对于主页菜单，始终返回false
      if (item.path === 'service-manage') {
        return false;
      }
      
      // 从完整的firstMenu中查找对应项
      const menuItem = this.firstMenu.find(m => m.fullPath === item.fullPath);
      return menuItem && menuItem.children && menuItem.children.length > 0;
    },
    
    // 获取菜单的子菜单
    getMenuChildren(item) {
      // 从完整的firstMenu中查找对应项的children
      const menuItem = this.firstMenu.find(m => m.fullPath === item.fullPath);
      return menuItem && menuItem.children ? menuItem.children : [];
    },
    
    // 处理子菜单点击
    onSubMenuSelect(subItem, event) {
      // 打印完整的子菜单项对象，以便调试
      console.log('子菜单项:', subItem.name, subItem.fullPath);
      
      // 关闭所有下拉菜单
      this.hoveredMenu = '';
      
      // 设置当前激活的一级菜单
      const parentMenu = this.firstMenu.find(item => 
        item.children && item.children.some(child => child.fullPath === subItem.fullPath)
      );
      if (parentMenu) {
        this.$emit('firstMenuSelect', parentMenu.fullPath);
      }
      
      // 通知父组件路由已经改变
      this.$emit('routeChanged', subItem.fullPath);
    },
    // 切换运行中的集群
    changeCluster (val) {
      if (this.clusterId === val.key) return false
      this.setClusterId(val.key)
      // 刷新服务列表
      this.$store.dispatch('setting/getRunningClusterList')
      // 使用goToHome确保行为一致
      this.goToHome();
    },
    ...mapMutations("setting", ["setLang", "setClusterId"]),
    goToHome() {
      // 跳转到主页
      if (this.$route.path !== '/service-manage') {
        this.$router.push("/service-manage").catch(err => {
          // 忽略重复导航错误
          if (err.name !== 'NavigationDuplicated') {
            throw err;
          }
        });
      }
    },
  },
  created() {
    if (this.firstMenu && this.firstMenu.length > 0) {
      this.activeFirstMenuKey = this.firstMenu[0].fullPath
    }
    
    // 添加调试信息
    console.log('菜单数据:', this.firstMenu);
    setTimeout(() => {
      console.log('菜单数据(延迟检查):', this.firstMenu);
      console.log('regularMenus:', this.regularMenus);
    }, 1000);
  }
};
</script>

<style lang="less" scoped>
@import "index";
.system-name {
  font-size: 14px;
  color: #fff;
  letter-spacing: 0.39px;
  font-weight: 500;
  margin-right: 5px;
}
.cdh-header-inner {
  display: flex;
  align-items: center;
  width: 100%;
  height: 56px;
  padding: 0 16px;
}
.left-section {
  display: flex;
  align-items: center;
}
.right-section {
  display: flex;
  align-items: center;
  margin-left: auto;
}
.logo {
  display: flex;
  align-items: center;
  margin-right: 32px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.logo:hover {
  opacity: 0.9;
}

.cdh-header-menu {
  display: flex;
  align-items: center;
}
.cdh-top-menu {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  height: 56px;
  align-items: center;
}
.cdh-top-menu li {
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 56px;
  font-size: 15px;
  color: #222b45;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  position: relative;
}
.cdh-top-menu li.active, .cdh-top-menu li:hover {
  color: #1976d2;
  background: #f0f6ff;
}
.admin-menu-list {
  display: flex;
  list-style: none;
  margin: 0 24px 0 0;
  padding: 0;
  height: 56px;
  align-items: center;

  li {
    display: flex;
    align-items: center;
    padding: 0;
    height: 56px;
    font-size: 15px;
    color: #222b45;
    cursor: pointer;
    transition: background 0.2s, color 0.2s;
    position: relative;
    
    &:hover, &.active {
      color: #1976d2;
      background: #f0f6ff;
    }
    
    .menu-content {
      display: flex;
      align-items: center;
      height: 100%;
      padding: 0 16px;
    }
    
    .svg-icon {
      font-size: 16px;
    }
  }
}

.action-buttons {
  display: flex;
  align-items: center;
  padding-left: 16px;
  border-left: 1px solid #e0e0e0;
}

.cluster-selector {
  margin-right: 16px;
  cursor: pointer;

  .action-item {
    font-size: 14px;
    color: #222b45;
    display: flex;
    align-items: center;

    .anticon {
      font-size: 12px;
      margin-left: 4px;
    }

    &:hover {
      color: #1976d2;
    }
  }
}

.cluster-setting, .alarm-manage {
  margin: 0 8px;
  display: flex;
  align-items: center;
}

.admin-sub-dropdown {
  position: absolute;
  top: 56px;
  left: 0;
  min-width: 160px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  border-radius: 8px;
  z-index: 100;
  padding: 8px 0;
  list-style: none;
  
  li {
    padding: 0 20px;
    height: 44px;
    display: flex;
    align-items: center;
    font-size: 15px;
    color: #222b45;
    cursor: pointer;
    transition: background 0.2s, color 0.2s;
    
    &:hover, &.active {
      background: #f0f6ff;
      color: #1976d2;
    }
    
    .svg-icon {
      font-size: 18px;
      margin-right: 8px;
    }
  }
}

.cdh-sub-dropdown {
  position: absolute;
  top: 56px;
  left: 0;
  min-width: 160px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  border-radius: 8px;
  z-index: 100;
  padding: 8px 0;
  list-style: none;
  li {
    padding: 0 20px;
    height: 44px;
    display: flex;
    align-items: center;
    font-size: 15px;
    color: #222b45;
    cursor: pointer;
    transition: background 0.2s, color 0.2s;
    &:hover, &.active {
      background: #f0f6ff;
      color: #1976d2;
    }
    .svg-icon {
      font-size: 18px;
      margin-right: 8px;
    }
  }
}

.dropdown-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 6px;
  width: 16px;
  height: 16px;
  opacity: 0.6;
  transition: all 0.3s;
  
  .anticon {
    font-size: 10px;
    transform: scale(0.9);
    transition: transform 0.3s;
  }
}

li:hover .dropdown-icon,
li.active .dropdown-icon {
  opacity: 1;
}

li:hover .dropdown-icon .anticon,
li.active .dropdown-icon .anticon {
  transform: scale(0.9) rotate(180deg);
}

.action-item:hover .dropdown-icon {
  opacity: 1;
}

.action-item:hover .dropdown-icon .anticon {
  transform: scale(0.9) rotate(180deg);
}

.cdh-sub-dropdown li a,
.admin-sub-dropdown li a {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  color: inherit;
  text-decoration: none;
}
</style>