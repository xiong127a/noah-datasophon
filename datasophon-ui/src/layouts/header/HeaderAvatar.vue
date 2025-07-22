<template>
  <a-dropdown :trigger="['hover']" placement="bottomRight" :getPopupContainer="() => $refs.headerAvatar">
    <div class="header-avatar" style="cursor: pointer" ref="headerAvatar">
      <svg-icon icon-class="avatar" class="avatar-icon" />
      <span class="name">{{ user.username }}</span>
    </div>
    <a-menu slot="overlay" class="user-menu">
      <a-menu-item @click="viewUserInfo">
        <a-icon type="user" />
        <span>个人中心</span>
      </a-menu-item>
      <a-menu-divider />
      <a-menu-item @click="logout">
        <a-icon type="poweroff" />
        <span>退出登录</span>
      </a-menu-item>
    </a-menu>
  </a-dropdown>
</template>

<script>
import { mapGetters, mapState, mapMutations } from "vuex";
import { logout } from "@/services/user";
import UserInfo from "./UserInfo.vue";
export default {
  name: "HeaderAvatar",
  computed: {
    ...mapGetters("account", ["user"]),
    ...mapGetters("setting", ["isCluster"]),
  },
  methods: {
    ...mapMutations("setting", ["setIsCluster", "setMenuData"]),
    viewUserInfo() {
      let width = 400;
      let title = "个人中心";
      let content = <UserInfo />;
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    logout() {
      this.$axiosPost(global.API.loginOut, {}).then((res) => {
        this.ssoLogout()
        logout();
        localStorage.removeItem("isCluster");
        this.setIsCluster("");
        location.href = location.origin + location.pathname
      });
    },
    ssoLogout(){
      this.$axiosGet('/ddh/sso/logout', {}).then(res => {})
    },
    toCluster() {
      localStorage.removeItem("isCluster");
      this.setIsCluster("");
      this.$router.push("/colony-manage/colony-list");
    },
  },
};
</script>

<style lang="less">
/* 基础样式 - 由父组件AdminHeader控制 */
.header-avatar {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  
  .avatar-icon {
    transition: all 0.3s ease;
  }
  
  .avatar,
  .name {
    align-self: center;
    transition: all 0.3s ease;
  }
  
  .avatar {
    margin-right: 8px;
  }
}

/* 用户下拉菜单 - 苹果风格 */
.user-menu {
  min-width: 160px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(30px) saturate(180%);
  border-radius: 12px;
  box-shadow: 
    0 12px 28px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  
  .ant-dropdown-menu-item,
  .ant-menu-item {
    padding: 10px 16px;
    transition: all 0.2s;
    
    .anticon {
      font-size: 16px;
      color: #007aff;
      margin-right: 10px;
    }
    
    span {
      font-size: 14px;
      color: #1d1d1f;
    }
    
    &:hover {
      background-color: rgba(0, 122, 255, 0.08);
      
      .anticon {
        color: #0056d3;
      }
      
      span {
        color: #007aff;
      }
    }
  }
  
  .ant-menu-item-divider,
  .ant-dropdown-menu-item-divider {
    background-color: rgba(0, 0, 0, 0.06);
    margin: 4px 0;
  }
}

/* 修复Ant Design下拉菜单容器问题 */
.ant-dropdown-placement-bottomRight {
  .ant-dropdown-menu-root {
    box-shadow: none !important;
    background: transparent !important;
    border: none !important;
  }
}
</style>