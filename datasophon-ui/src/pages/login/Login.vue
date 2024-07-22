<template>
  <div>
    <!-- <common-layout style="display: none;">
      <div class="top">
        <div class="header">
         <img src="@/assets/img/logo.png" alt /> 
         <span class="logo-name">DataSophon</span>
        </div>
      </div>
      <div class="login">
        <div class="login-title">账号登录</div>
        <div class="project-name">Noah大数据基础平台</div>
        <a-form @submit="onSubmit" :form="form">
          <a-alert type="error" :closable="true" v-show="error" :message="error" showIcon style="margin-bottom: 24px;" />
          <a-form-item>
            <a-input class="login-input" size="large" placeholder="输入用户名" v-decorator="['name', {rules: [{ required: true, message: '请输入用户名', whitespace: true}]}]"></a-input>
          </a-form-item>
          <a-form-item>
            <a-input class="login-input" size="large" placeholder="输入密码" type="password" v-decorator="['password', {rules: [{ required: true, message: '请输入密码', whitespace: true}]}]"></a-input>
          </a-form-item>
          <a-form-item>
            <a-button :loading="logging" style="width: 400px;height: 50px;border-radius: 4px;margin-top: 47px;font-size: 18px" size="large" htmlType="submit" type="primary">登录</a-button>
          </a-form-item>
        </a-form>
      </div>
      <page-footer style="position: absolute;bottom:0;left:0;right:0;display:none" :link-list="footerLinks" :copyright="copyright"></page-footer>
    </common-layout> -->
    <div class="container" v-if="!isSsoLogin">
      <div class="company-logo">
        <img src="@/assets/login-img/company.png">
      </div>
      <div class="product_logo">
        <img src="@/assets/login-img/product.png">
      </div>
      <div class="right">
        <div class="submitform">
          <img src="@/assets/login-img/formbg.png" class="form_img">
          <a-form @submit="onSubmit" :form="form" class="form-cont">
            <a-alert type="error" :closable="true" v-show="error" :message="error" showIcon
              style="margin-bottom: 24px;" />
            <a-form-item>
              <a-input class="login-input" size="large" placeholder="输入用户名" autoComplete="off" allowClear
                v-decorator="['name', { rules: [{ required: true, message: '请输入用户名', whitespace: true }] }]">
                <template #prefix><a-icon type="user" class="icon" :maxLength="255"/></template>
              </a-input>
            </a-form-item>
            <a-form-item>
              <a-input class="login-input" size="large" placeholder="输入密码" type="password" autoComplete="off" allowClear
                v-decorator="['password', { rules: [{ required: true, message: '请输入密码', whitespace: true }] }]">
                <template #prefix><a-icon type="lock" class="icon" :maxLength="255"/></template>
              </a-input>
            </a-form-item>
            <a-form-item>
              <a-button :loading="logging"
                style="width: 400px;height: 40px;border-radius: 4px;margin-top: 10px;font-size: 18px" size="large"
                htmlType="submit" type="primary">登录</a-button>
            </a-form-item>
          </a-form>
        </div>
      </div>
      <div class="copyright">
        北京中兵数字科技集团有限公司 版权所有copyright@2024
      </div>
      <div class="companybot-logo">
        <img src="@/assets/login-img/company.png">
      </div>
    </div>
  </div>
</template>

<script>
// import PageFooter from "@/layouts/footer/PageFooter";

// import CommonLayout from "@/layouts/CommonLayout";
import { login, getRoutesConfig } from "@/services/user";
import { setAuthorization } from "@/utils/request";
import { loadRoutes } from "@/utils/routerUtil";
import { mapMutations } from "vuex";
import { mapGetters,mapState } from "vuex";
import { logout } from "@/services/user";
export default {
  name: "Login",
  // components: { CommonLayout, PageFooter },
  data () {
    return {
      logging: false,
      error: "",
      form: this.$form.createForm(this),

      isSsoLogin:true,
      back: '',
      ticket: '',
      token:''
    };
  },
  computed: {
    systemName () {
      return this.$store.state.setting.systemName;
    },
    ...mapGetters("account", ["user"]),
    ...mapState("setting", ["footerLinks", "copyright"]),
  },
  created () {
      if(localStorage.getItem('satoken')){
        this.$router.push("/colony-manage/colony-list");
      }else{
          this.$axiosGet('/ddh/ssoEnable').then((res) => {
              this.isSsoLogin = res.data?res.data:false;
              if(this.isSsoLogin){
                // true  的时候 用户管理模块隐藏（未实现2024-5-8）

                this.back = location.pathname; 
                this.ticket = this.getParameterByName('ticket');  //方舟登录
                this.token = this.getParameterByName('token') // hh第三方网址进来的用户token
               
                let pathTicket = this.ticket?this.ticket:this.token;
                console.log("ticket或者token",pathTicket)
                
                if (pathTicket && this.isSsoLogin) {
                  this.doLoginByTicket(pathTicket);
                }else{
                    this.goSsoAuthUrl()  
                }
              }
          })
      }
  },
  methods: {
    ...mapMutations("account", ["setUser", "setPermissions", "setRoles"]),
    onSubmit (e) {
      e.preventDefault();
      this.form.validateFields((err) => {
        if (!err) {
          this.logging = true;
          const username =this.form.getFieldValue("name");
          const password = this.form.getFieldValue("password");
          this.$axiosPost(global.API.login, { username, password }).then(
            (res) => this.afterLogin(res)
          );
        }
      });
    },
    afterLogin (res) {
      this.logging = false;
      const loginRes = res.data;
      if (res.code === 200) {
        setAuthorization({ sessionId: loginRes.sessionId });
        this.setUser(res.userInfo);
        loadRoutes()
        this.$store.commit('setting/setIsCluster', '')
        this.$router.push("/colony-manage/colony-list");
        this.$message.success("登录成功", 3);
      }
    },

    //sso 重定向至认证中心
    goSsoAuthUrl () {
      let param = {
        clientLoginUrl: location.origin+location.pathname+location.hash
      }
      this.$axiosGet('/ddh/sso/getSsoAuthUrl', param).then(res => {
        if (res.code === 200) {
          location.href = res.data;
        }
      })
    },

    // 根据ticket值登录
    async doLoginByTicket (ticket) {
      let param = {
        ticket: ticket
      }
      let res = await this.$axiosGet('/ddh/sso/doLoginByTicket', param);
      if (res.code === 200) {
        localStorage.setItem('satoken', null);
        localStorage.setItem('satoken', res.data);
        
        this.$axiosGet('/ddh/saveSsoUser',"").then((res) => {
         
          if (res.code === 200) {
            const username = res.data.username;
            const password = res.data.password;
            this.$axiosPost(global.API.login, { username, password }).then(
              (res) => this.afterLogin(res)
            );
          }
        })
      } else {
        this.$message.warning(res.msg);
        localStorage.removeItem("isCluster");

        this.$axiosGet('/ddh/sso/logout', {}).then(res => {})  //sso 退出
        logout();  //基础平台 退出
        // this.$router.push('/login')
        location.href = location.origin + location.pathname+'#/login';
        location.reload()
      }
      
    },
    //获取地址栏上的参数
    getParameterByName(name) {
      name = name.replace(/[\\[\]]/, '\\$&');
      var regex = new RegExp('[\\?&]' + name + '=([^&#]*)');
      var results = regex.exec(location.href);
      return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '));
    }
    
  },
};
</script>

<style lang="less" scoped>
.common-layout {
  .top {
    text-align: center;

    .header {
      padding: 0 0 0 40px;
      height: 44px;
      line-height: 44px;
      display: flex;
      justify-items: center;
      align-items: center;

      a {
        text-decoration: none;
      }

      .logo {
        height: 44px;
        vertical-align: top;
        margin-right: 16px;
      }

      .logo-name {
        font-size: 24px;
        color: #333333;
        letter-spacing: 0;
        text-align: center;
        font-weight: 600;
        padding-left: 10px;
      }

      .title {
        font-size: 33px;
        color: @title-color;
        // font-family: "Myriad Pro", "Helvetica Neue", Arial, Helvetica,
        // sans-serif;
        font-weight: 600;
        position: relative;
        top: 2px;
      }
    }

    .desc {
      font-size: 14px;
      color: @text-color-second;
      margin-top: 12px;
      margin-bottom: 40px;
    }
  }

  .login {
    text-align: center;
    position: absolute;
    right: 38%;
    width: 490px;
    height: 600px;
    // height: 644px;
    margin: 70px 0 0 0;
    box-shadow: 0px 2px 10px 0px rgba(0, 0, 0, 0.1);
    border-radius: 4px;

    .login-title {
      margin: 54px 0 0 40px;
      width: 76px;
      height: 50px;
      font-size: 18px;
      color: #333333;
      letter-spacing: 0;
      line-height: 50px;
      font-weight: 600;
      border-bottom: 2px solid #bcc0c8;
    }

    .project-name {
      font-size: 26px;
      color: #333330;
      letter-spacing: 0;
      text-align: center;
      font-weight: 600;
      margin: 55px 0;
    }

    .login-input {
      width: 100px;
      height: 40px;
      border: 1px solid rgba(196, 204, 219, 1);
      border-radius: 4px;
      background-color: #333330;
    }
  }
}


.container {
  position: relative;
  width: 100vw;
  height: 100vh;
  background-image: url('../../assets/login-img/loginbg.png');
  background-repeat: no-repeat;
  overflow: hidden;
  background-size: 100% 110%;
  cursor: pointer;
  user-select: none;

  .company-logo {
    position: absolute;
    left: 150px;
    top: 80px;
    transform: translate(-50%, -50%);

    img {
      height: 50px;
    }
  }

  .product_logo {
    position: absolute;
    left: 50%;
    top: 160px;
    transform: translate(-50%, -50%);

    img {
      height: 36px;
    }
  }

  .copyright {
    position: absolute;
    left: 50%;
    bottom: 60px;
    transform: translate(-50%, -50%);
    color: #fff;
    font-size: 16px;
  }

  .companybot-logo {
    position: absolute;
    left: 50%;
    bottom: 10px;
    transform: translate(-50%, -50%);

    img {
      height: 30px;
    }
  }

  .right {
    width: 500px;
    display: flex;
    position: absolute;
    left: 50%;
    top: 55%;
    border-radius: 8px;
    transform: translate(-50%, -50%);
    overflow: hidden;

    .left {
      color: #fff;
    }

    .submitform {
      width: 650px;
      height: 550px;
      padding-top: 50px;

      .form_img {
        width: 500px;
        height: 350px;
        z-index: 1px;
        position: absolute;
      }

      .form-cont {
        width: 100%;
        padding: 60px 40px 40px 40px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        z-index: 2;

        .login-input {
          width: 400px;
          height: 45px;
          border: 1px solid rgb(84, 162, 239);
          border-radius: 4px;
          background-color: rgba(255, 255, 255, .01);

          .icon {
            color: rgb(159, 206, 255);
            font-size: 18px;
            margin: 0px -10px;
          }
        }
      }

      .sumbtn {
        width: 100%;
        height: 40px;
        margin-top: 20px;
      }


    }

    .codeImage {
      width: 100%;
      height: 100%;
    }

  }
}

/deep/.ant-input-affix-wrapper .ant-input-prefix {
  left: 15px;
}

.ant-input-affix-wrapper .ant-input:not(:first-child) {
  padding-left: 150px;
}

/deep/.has-error .ant-input {
  border-color: none;
  background-color: none;
}

/deep/ .ant-input {
  background-color: none;
//  color: #fff;
}

/deep/ .has-error .ant-input:focus {
  border: none;
  box-shadow: none;
}

/deep/ .has-error .ant-input {
  border: none;
  box-shadow: none;
}

/deep/ .ant-input-lg {
  height: 42px;
  padding: 6px 11px;
  font-size: 14px;
  //color: #fff;
  border: none;
  background-color: none;
}

/deep/ .ant-input-affix-wrapper-lg {
  background: none !important;
}
 /deep/.ant-input-affix-wrapper  {
    //padding-left: 46px;
  } 
</style>