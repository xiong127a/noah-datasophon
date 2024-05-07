<template>
  <div style="padding-top: 10px">
    <div class="title">租户列表</div>
    <ul class="content">
      <li v-for="(user, index) in tenantList" :key="user.tenantId">
        <a-checkbox v-model="user.checked" @change="(value) => changeCheck(value, index)">{{ user.tenantName
        }}</a-checkbox>
      </li>
    </ul>
    <div class="ant-modal-confirm-btns-new">
      <a-button class="btn" type="" @click.stop="handleSubmit"><a-icon type="check" class="iconBtn"></a-icon></a-button>
      <a-button @click.stop="formCancel" class="btnCancel"><a-icon type="close" size="20px"
          class="iconBtn"></a-icon></a-button>
    </div>

  </div>
</template>
<script>


export default {
  props: {
    callBack: Function,
    sysTypeTxt: String,
    detail: Object,
    checkedList: Array,
  },
  data () {
    return {
      labelCol: {
        xs: { span: 6 },
        sm: { span: 2 },
      },
      wrapperCol: {
        xs: { span: 18 },
        sm: { span: 18 },
      },
      tenantList: [],
      form: this.$form.createForm(this),
    }
  },
  methods: {
    getUserList () {
      this.loading = true;
      let params = {
        clusterId: Number(localStorage.getItem("clusterId") || 1),
        size: 1000,
        page: 1,
      };
       this.tenantList = []
      this.$axiosGet('/ddh/cluster/tenant/listTenant', params).then((res) => {
        this.tenantList = res.data;
        this.tenantList.forEach((j, index) => {
          this.checkedList.forEach(e => {
            if (e.tenantId == j.id) {
              this.tenantList[index].checked = true
            }
          })
        })
      });
    },
    changeCheck (val, index) {
      this.tenantList = this.tenantList.map((e, index1) => {
        if (index == index1) {
          this.$set(e, 'checked', val.target.checked)
        }
        return e
      })
    },
    formCancel () {
      this.callBack();
    },
    handleSubmit () {
      let arr = []
      this.tenantList.forEach(e => {
        if (e.checked) {
          arr.push(e.id)
        }
      })
      arr = [...new Set(arr)]
      let deleteID = this.checkedList.filter(item1 => !arr.find(item2 => item2 == item1.tenantId))
      let addID = arr.filter(item2 => !this.checkedList.find(item1 => item1.tenantId == item2))
      let delId = []
      deleteID.forEach(e => {
        delId.push(e.tenantId)
      })
      if (addID.length !== 0) {
        let params = {
          clusterId: Number(localStorage.getItem("clusterId") || 1),
          userId: this.detail.id,
          tenantIds: addID.toString(),
        };
        this.$axiosGet('/ddh/cluster/user/tenant/add', params).then((res) => {
          if (res.code === 200) {
            this.$message.success("授权成功");
            // this.$destroyAll();
            this.callBack();
          }
        });
      }
      if (deleteID.length !== 0) {
        let params = {
          clusterId: Number(localStorage.getItem("clusterId") || 1),
          userId: this.detail.id,
          tenantIds: delId.toString(),
        };
        this.$axiosDelete('/ddh/cluster/user/tenant/delete', params).then((res) => {
          if (res.code === 200) {
            // this.$message.success("授权成功");
            // this.$destroyAll();
            this.callBack();
          }
        });
      }

    },

  },
  mounted () {
    this.getUserList()

  }
}
</script>
<style lang="less" scoped>
.content {
  max-height: 300px;
  overflow: auto;
}

.title {
  text-align: center;
  padding-bottom: 20px;
}

.btn {
  margin-right: 10px;
  color: #fff;
  background: #0B7FAD;
  border-radius: 4px
}

.btnCancel {
  color: #333;
  background: #fff;
  border-radius: 4px
}

.iconBtn {
  font-size: 16px;
}

.ant-modal-confirm-btns-new {
  margin: 0 10px 0px;
}

ul li {
  list-style-type: none;
}

.ant-popover-inner-content {
  padding: 10px;
}
</style>