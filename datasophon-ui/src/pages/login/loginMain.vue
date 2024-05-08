<template>
  <div>
    <Login></Login>
  </div>
</template>
<script>
import Login from './Login.vue'
export default {
  name: "LoginMain",
  components: { Login },
  data () {
    return {
      isShow: true,
      form: this.$form.createForm(this),
    };
  },
};
</script>

