
<template>
  <div class="user-list">
    <a-card class="mgb16 card-shadow">
      <a-row type="flex" align="middle">
        <a-col :span="22">
          <a-input placeholder="请输入租户名称" class="w252 mgr12" @change="(value) => getVal(value, 'tenantName')" allowClear />
          <a-button class type="primary" icon="search" @click="onSearch"></a-button>
        </a-col>
        <a-col :span="2" style="text-align: right">
          <a-button style="margin-left: 10px;" type="primary" @click="createUser({}, '')">添加租户</a-button>
        </a-col>
      </a-row>
    </a-card>
    <div class="content card-shadow">
      <div class="list" v-for="(item, index) in dataSource" :key="index">
        <div class="top">
          <a-icon type="user" class="icon"></a-icon> <span class="name">{{ item.tenantName }}</span>
        </div>
        <div class="discription">
          <a-tooltip :title="item.desc" placement="top">{{ item.desc && item.desc.length > 52 ? item.desc.slice(0, 52) + '...' :
            item.desc }}</a-tooltip>
        </div>
        <div class="tags">
          <a-button v-for="(i, index) in item.assembly" :key="index" shape="round" size="small"
            style="margin:0px 6px 10px 0px;font-size:12px">
            {{ i }}
          </a-button>
          <!-- <a-tag color="orange" v-for="(item, index) in item.assembly" :key="index">{{ item }}</a-tag> -->
        </div>
        <div class="btns">
          <a-tooltip title="详情" placement="top"><a-icon type="eye" class="icon"
              @click="toDetail(item)"></a-icon></a-tooltip>
          <a-tooltip title="编辑" placement="top"><a-icon type="edit" class="icon"
              @click="createUser(item)"></a-icon></a-tooltip>
          <a-tooltip title="删除" placement="top"><a-icon type="delete" class="icon"
              @click="delectUser(item)"></a-icon></a-tooltip>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AddUser from "./commponents/addUser.vue";
import Detail from "./commponents/detail.vue";
import DelectUser from "./commponents/delectUser.vue";
import { mapGetters, mapState, mapMutations } from "vuex";

export default {
  name: "USER",
  // components: { Detail },
  data () {
    return {
      params: {},
      pagination: {
        total: 0,
        size: 10,
        current: 1,
        showSizeChanger: true,
        sizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      username: '',
      dataSource: [],
      loading: false,
      columns: [
        {
          title: "序号",
          key: "index",
          width: 70,
          customRender: (text, row, index) => {
            return (
              <span>
                {parseInt(
                  this.pagination.current === 1
                    ? index + 1
                    : index +
                    1 +
                    this.pagination.size * (this.pagination.current - 1)
                )}
              </span>
            );
          },
        },
        { title: "租户名称", key: "tenantName", dataIndex: "tenantName" },
        {
          title: "描述",
          key: "otherGroups",
          dataIndex: "otherGroups",
        },
        {
          title: "操作",
          key: "action",
          width: 300,
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                <a
                  class="btn-opt"
                  onClick={() => this.createUser(row)}
                >
                  编辑
                </a>
                <a class="btn-opt" onClick={() => this.delectUser(row)}>
                  删除
                </a>
              </span>
            );
          },
        },
      ],
    };
  },
  computed: {
    ...mapGetters("account", ["user"]),
  },
  methods: {
    tableChange (pagination) {
      this.pagination.current = pagination.current;
      this.pagination.size = pagination.size
      this.getUserList();
    },
    getVal (val, filed) {
      this.params[`${filed}`] = val.target.value;
    },
    //   查询
    onSearch (key) {
      this.pagination.current = 1;
      this.getUserList();
    },
    toDetail (obj) {
      const self = this;
      let width = '50%'
      let content = (
        <Detail detail={obj} />
      );
      this.$confirm({
        width: width,
        title: '租户详情',
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    createUser (obj) {
      const self = this;
      let width = '70%'
      let title = JSON.stringify(obj) === "{}" ? "添加租户" : "编辑租户";
      let content = (
        <AddUser detail={obj} callBack={() => self.getUserList()} />
      );
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
    delectUser (obj) {
      const self = this;
      let width = 400;
      let content = (
        <DelectUser
          sysTypeTxt="租户"
          detail={obj}
          callBack={() => self.getUserList}
        />
      );
      content = <DelectUser sysTypeTxt="租户" detail={obj} api='/ddh/cluster/tenant/delete' callBack={() => self.getUserList()} />
      this.$confirm({
        width: width,
        title: () => {
          return (
            <div>
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    getUserList () {
      this.loading = true;
      let params = {
        clusterId: Number(localStorage.getItem("clusterId") || 1),
        size: this.pagination.size,
        page: this.pagination.current,
        ...this.params,
      };
      this.$axiosGet('/ddh/cluster/tenant/listTenant', params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        this.dataSource.forEach(e => {
          let assembly = []
          if (e.hdfsResourceList.length > 0) {
            assembly.push(e.hdfsResourceList[0].serviceName)
          }
          if (e.hbaseResourceList.length > 0) {
            assembly.push(e.hbaseResourceList[0].serviceName)
          }
          if (e.hiveResourceList.length > 0) {
            assembly.push(e.hiveResourceList[0].serviceName)
          }
          if (e.kafkaResourceList.length > 0) {
            assembly.push(e.kafkaResourceList[0].serviceName)
          }
          if (e.yarnResourceList.length > 0) {
            assembly.push(e.yarnResourceList[0].serviceName)
          }
          e['assembly'] = assembly
        })
        this.pagination.total = res.total;
      });
    },
  },
  mounted () {
    this.getUserList();
  },
};
</script>

<style lang="less" scoped>
.user-list {
  background: #f5f7f8;

  .content {
    width: 100%;
    display: flex;
    justify-content: flex-start;
    flex-wrap: wrap;
    background: #fff;
    padding: 10px;

    .list {
      width: 24.6%;
      margin-right: 5px;
      border: 1px solid #e2dede;
      padding: 10px;
      margin-bottom: 10px;

      .top {
        margin: 10px;

        .name {
          font-weight: 700;
          font-size: 16px;
        }

        .icon {
          color: #2872e0;
          font-size: 20px;
          margin-right: 10px;
        }
      }

      .discription {
        font-size: 12px;
        text-indent: 20px;
        height: 40px;
      }

      .tags {
        margin: 10px 0px;
      }

      .btns {
        text-align: end;

        .icon {
          margin-right: 5px;
          font-size: 16px;
          cursor: pointer;
        }
      }
    }
  }

  .btn-opt {
    border-radius: 1px;
    font-size: 12px;
    color: #0264c8;
    letter-spacing: 0;
    font-weight: 400;
    margin: 0 5px;
  }
}
</style>
