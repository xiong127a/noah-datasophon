<template>
  <div>
  </div>
</template>

<script>
// import { getAuthUrl, getSatoken } from '@/api/user';

export default {
  name: "ssoLogin",
  data () {
    return {
      showDialog: true,
      back: '',
      ticket: '',
    }
  },
  created () {
    this.back = location.pathname //this.getParam('back', '/')
    // this.ticket = this.getParam('ticket')
    this.ticket = ''
    console.log('this.ticket', this.ticket);
    if (this.ticket) {
      this.doLoginByTicket(this.ticket);
    } else {
      this.goSsoAuthUrl()
    }
  },
  methods: {
    // 重定向至认证中心
    goSsoAuthUrl () {
      let param = {
        clientLoginUrl: location.href
      }
      this.$axiosGet('/ddh/sso/getSsoAuthUrl', param).then(res => {
        console.log(res, 'rr');
        if (res.code === 200) {
          location.href = res.data;
        }
      })
    },

    // 根据ticket值登录
    doLoginByTicket (ticket) {
      let param = {
        ticket: ticket
      }
      this.$axiosGet('/ddh/sso/doLoginByTicket', param).then(async res => {
        if (res.code === 200) {
          localStorage.setItem('satoken', null);
          localStorage.setItem('satoken', res.data);
          // let ssotoken = localStorage.getItem('satoken')
          let param = {
            token: res.data
          }
          // this.$axiosGet('/ddh/sso/saveSsoUser', param).then((res) => {
          //   console.log(res, 'rr')

          //   if (res.code === 200) {
          //     console.log('saveSsoUser', res.data)
          //   }
          // })
          let url = decodeURIComponent(this.back);
          location.href = decodeURIComponent(this.back);
        } else {
          // console.log('ss');
          // this.$message.warning("dd", res.msg);
          // this.$store.dispatch('user/loginOut')
        }
      })
    },

    // // 从url中查询到指定名称的参数值
    // getParam (name, defaultValue) {
    //   return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.href) || [, ""])[1].replace(/\+/g, '%20')) || defaultValue;
    // },
  }
}
</script>
