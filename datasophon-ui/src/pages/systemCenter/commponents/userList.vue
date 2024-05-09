<template>
  <div style="padding-top: 10px">
    <div class="main">
      <div class="content">
        <div class="title">
          租户名称
        </div>
        <div v-for="(user, index) in tenantList" :key="user.tenantId"
          :class="activeIndex == index ? 'activeTenant' : 'tenantItem'">
          <a-checkbox v-model="user.checked" @change="(value) => changeCheck(value, index)"> <label class="checkbox-label"
              @click.stop="handleClick(user, index)">
              {{ user.tenantName }}
            </label> </a-checkbox>
        </div>
      </div>
      <div class="detail">
        <div class="title">
          {{ tenantName }}详情
        </div>
        <Detail :detail="detailInfo" :key="key"></Detail>
      </div>
    </div>
    <div class="ant-modal-confirm-btns-new">
      <a-button style="margin-right: 10px" type="primary" @click.stop="handleSubmit">确认</a-button>
      <a-button @click.stop="formCancel">取消</a-button>
    </div>

  </div>
</template>
<script>
import Detail from '../tenant/commponents/detail.vue'
export default {
  props: {
    callBack: Function,
    sysTypeTxt: String,
    detail: Object,
  },
  components: {
    Detail
  },
  data () {
    return {
      tenantName: '',
      activeIndex: '0',
      detailInfo: {},
      checkedList: [],
      key: 0,
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
        this.tenantName = res.data[0].tenantName
        this.detailInfo = res.data[0]
        console.log('this.checkedList', this.checkedList);
        this.tenantList && this.tenantList.forEach((j, index) => {
          this.checkedList && this.checkedList.forEach(e => {
            if (e.tenantId == j.id) {
              this.tenantList[index]['checked'] = true
            }
          })

        })
      });
    },
    changeCheck (val, index) {
      this.tenantList = this.tenantList && this.tenantList.map((e, index1) => {
        if (index == index1) {
          this.$set(e, 'checked', val.target.checked)
        }
        return e
      })
    },
    formCancel () {
      this.$destroyAll();
      this.callBack();
    },
    getTentant (id) {
      let params = {
        clusterId: Number(localStorage.getItem("clusterId") || 1),
        userId: id,
      };
      this.$axiosGet('/ddh/cluster/user/tenant/getListByUserId', params).then((res) => {
        if (res.code === 200) {
          this.checkedList = res.data
        }
      });
    },
    handleSubmit () {
      let arr = []
      this.tenantList && this.tenantList.forEach(e => {
        if (e.checked) {
          arr.push(e.id)
        }
      })
      arr = [...new Set(arr)]
      let deleteID = this.checkedList && this.checkedList.filter(item1 => arr && !arr.find(item2 => item2 == item1.tenantId))
      let addID = arr && arr.filter(item2 => this.checkedList && !this.checkedList.find(item1 => item1.tenantId == item2))
      console.log('add', addID);
      console.log('deleteID', deleteID);
      let delId = []
      deleteID && deleteID.forEach(e => {
        delId.push(e.tenantId)
      })
      if (addID && addID.length !== 0) {
        let params = {
          clusterId: Number(localStorage.getItem("clusterId") || 1),
          userId: this.detail.id,
          tenantIds: addID.toString(),
        };
        this.$axiosGet('/ddh/cluster/user/tenant/add', params).then((res) => {
          if (res.code === 200) {
            this.$message.success("授权成功");
            this.$destroyAll();
            this.callBack();
          }
        });
      }
      if (deleteID && deleteID.length !== 0) {
        let params = {
          clusterId: Number(localStorage.getItem("clusterId") || 1),
          userId: this.detail.id,
          tenantIds: delId.toString(),
        };
        this.$axiosDelete('/ddh/cluster/user/tenant/delete', params).then((res) => {
          if (res.code === 200) {
            this.$message.success("授权成功");
            this.$destroyAll();
            this.callBack();
          }
        });
      }
      if (deleteID.length == 0 && addID.length == 0) {
        this.$message.success("授权成功");
        this.$destroyAll();
      }
    },
    handleClick (val, index) {
      this.activeIndex = index
      this.detailInfo = val
      this.key++
      this.tenantName = val.tenantName
    },
  },

  mounted () {
    this.getTentant(this.detail.id)
    this.getUserList()
  }
}
</script>
<style lang="less" scoped>
.main {
  display: flex;
  padding: 0px 20px;
  justify-content: space-between;

  .detail {
    flex: .7;
    border: 1px solid #ccc;
  }

  .content {
    max-height: 300px;
    overflow: auto;
    flex: .27;
    border: 1px solid #ccc;
    padding-bottom: 20px;

    .tenantItem {
      padding-left: 20px;
      border-bottom: 1px solid #e6e3e3;
      color: #2872e0;
      line-height: 40px;
    }

    .activeTenant {
      line-height: 40px;
      padding-left: 20px;
      border-bottom: 1px solid #e6e3e3;
      color: #fff;
      background-color: rgba(225, 239, 255, )
    }

    .checkbox-label {
      cursor: pointer;
      color: #2872e0;
    }
  }
}

.title {
  line-height: 40px;
  background-color: rgb(242, 242, 242);
  text-indent: 20px;
  border-bottom: 1px solid #e6e3e3;
  font-weight: 700;
  color: #333;
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


.ant-popover-inner-content {
  padding: 10px;
}
</style>