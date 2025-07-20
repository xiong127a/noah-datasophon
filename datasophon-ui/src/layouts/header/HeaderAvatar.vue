<template>
  <a-dropdown>
    <div class="header-avatar" style="cursor: pointer">
      <svg-icon icon-class="avatar" class="avatar-icon" />
      <span class="name">{{ user.username }}</span>
    </div>
    <a-menu :class="['avatar-menu']" slot="overlay">
      <a-menu-item @click="viewUserInfo">
        <a-icon type="user" />
        <span>个人中心</span>
      </a-menu-item>
      <a-menu-divider />
      <a-menu-item @click="logout">
        <a-icon style="margin-right: 8px" type="poweroff" />
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
    // isCluster () {
    //   const isCluster = localStorage.getItem('isCluster')
    //   return isCluster
    // }
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
        // this.$router.push("/login");
     
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
      // localStorage.removeItem('menuData')
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
.avatar-menu {
  width: 180px !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(30px) saturate(180%) !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  border-radius: 16px !important;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.15),
    0 8px 25px rgba(0, 0, 0, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.06) !important;
  padding: 8px !important;
  
  .ant-menu-item {
    border-radius: 12px !important;
    margin-bottom: 4px !important;
    padding: 12px 16px !important;
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
    display: flex !important;
    align-items: center !important;
    
    &:last-child {
      margin-bottom: 0 !important;
    }
    
    &:hover {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.08) 0%, rgba(0, 122, 255, 0.12) 100%) !important;
      transform: translateX(4px) !important;
    }
    
    .anticon {
      font-size: 16px !important;
      color: #007aff !important;
      margin-right: 12px !important;
      transition: all 0.3s ease !important;
    }
    
    span {
      font-size: 14px !important;
      font-weight: 500 !important;
      color: #1d1d1f !important;
      letter-spacing: -0.1px !important;
      transition: all 0.3s ease !important;
    }
    
    &:hover .anticon {
      color: #0056d3 !important;
      transform: scale(1.1) !important;
    }
    
    &:hover span {
      color: #007aff !important;
    }
  }
  
  .ant-menu-item-divider {
    background: rgba(0, 0, 0, 0.06) !important;
    margin: 8px 12px !important;
    height: 1px !important;
  }
}
</style>