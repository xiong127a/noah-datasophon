<template>
  <div>
    <div v-if="!isShow">
      <Login></Login>
    </div>
    <div v-if="isShow">
      <ssoLogin></ssoLogin>
    </div>
  </div>
</template>
<script>
import Login from './Login.vue'
import ssoLogin from './ssoLogin.vue'
export default {
  name: "LoginMain",
  components: { Login, ssoLogin },
  data () {
    return {
      isShow: '',
      form: this.$form.createForm(this),
    };
  },
  created () {
    this.getInfo()
  },
  methods: {
    getInfo () {
      this.$axiosGet('/ddh/ssoEnable').then((res) => {
        this.isShow = res.data
      })
    },

  },
};

</script>