<!--
/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


 * @Date: 2022-06-09 10:11:22
 * @LastEditTime: 2022-10-27 16:41:30
 * @FilePath: \ddh-ui\src\pages\securityCenter\user.vue
-->

<template>
  <div class="user-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="page-title">用户管理</div>
        <div class="page-description">创建和管理系统用户</div>
      </div>
    </div>

    <!-- 搜索和操作区 -->
    <div class="search-action-container">
      <div class="search-box">
        <a-input 
          placeholder="请输入用户名搜索" 
          class="search-input" 
          @change="(value) => getVal(value, 'username')" 
          allowClear
          prefix-icon="search"
        >
          <a-icon slot="prefix" type="search" />
        </a-input>
        <a-button class="search-button" type="primary" @click="onSearch">搜索</a-button>
      </div>
      <div class="action-box">
        <a-button type="primary" class="add-button" @click="createUser({})">
          <a-icon type="user-add" />
          <span>添加用户</span>
        </a-button>
      </div>
    </div>

    <!-- 用户列表卡片 -->
    <div class="user-list-card">
      <div class="table-container">
        <a-table 
          @change="tableChange" 
          :columns="columns" 
          :loading="loading" 
          :dataSource="dataSource" 
          rowKey="id" 
          :pagination="pagination"
          :row-class-name="setRowClassName"
        ></a-table>
      </div>
    </div>
    
    <!-- 添加/编辑用户弹窗 -->
    <a-modal
      :title="userModalTitle"
      :visible="userModalVisible"
      :footer="null"
      :maskClosable="false"
      @cancel="handleUserModalCancel"
      class="apple-style-modal"
      :destroyOnClose="true"
      width="550px"
    >
      <AddUser 
        :detail="currentUser" 
        @ok="handleUserModalOk" 
        @cancel="handleUserModalCancel"
      />
    </a-modal>
  </div>
</template>

<script>
import AddUser from "./commponents/addUser.vue";
import { mapGetters, mapState, mapMutations } from "vuex";

export default {
  name: "USER",
  components: {
    AddUser
  },
  data() {
    return {
      params: {},
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
        size: 'large',
      },
      username:'',
      dataSource: [],
      loading: false,
      userModalVisible: false,
      userModalTitle: '添加用户',
      currentUser: {},
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
                        this.pagination.pageSize * (this.pagination.current - 1)
                )}
              </span>
            );
          },
        },
        { 
          title: "用户名", 
          key: "username", 
          dataIndex: "username",
          sorter: (a, b) => a.username.localeCompare(b.username),
        },
        {
          title: "邮箱",
          key: "email",
          dataIndex: "email",
        },
        { 
          title: "电话", 
          key: "phone", 
          dataIndex: "phone" 
        },
        { 
          title: "创建时间", 
          key: "createTime", 
          dataIndex: "createTime",
          sorter: (a, b) => new Date(a.createTime) - new Date(b.createTime),
        },
        {
          title: "操作",
          key: "action",
          width: 140,
          customRender: (text, row, index) => {
            return (
              this.username == "admin" ? (
                <div class="action-buttons">
                  <a-button
                    type="link"
                    class="edit-button"
                    onClick={() => this.createUser(row)}
                  >
                    <a-icon type="edit" />
                    编辑
                  </a-button>
                  { row.userType !==1 ? 
                    <a-button
                      type="link" 
                      class="delete-button"
                      onClick={() => this.delectUser(row)}
                    >
                      <a-icon type="delete" />
                    删除
                    </a-button> 
                    : 
                    <a-button
                      type="link"
                      class="delete-button disabled"
                      disabled
                    >
                      <a-icon type="delete" />
                    删除
                    </a-button>
                  }
                </div>
              ) :
                row.username == this.username ? (
                  <div class="action-buttons">
                    <a-button
                      type="link"
                      class="edit-button"
                      onClick={() => this.createUser(row)}
                    >
                      <a-icon type="edit" />
                      编辑
                    </a-button>
                    <a-button
                      type="link" 
                      class="delete-button"
                      onClick={() => this.delectUser(row)}
                    >
                      <a-icon type="delete" />
                    删除
                    </a-button>
                  </div>
                ) : (
                  <div class="action-buttons">
                    <a-button
                      type="link"
                      class="edit-button disabled"
                      disabled
                    >
                      <a-icon type="edit" />
                    编辑
                    </a-button>
                    <a-button
                      type="link" 
                      class="delete-button disabled"
                      disabled
                    >
                      <a-icon type="delete" />
                    删除
                    </a-button>
                  </div>
                )
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
    // 设置表格行样式
    setRowClassName(record, index) {
      return index % 2 === 0 ? 'table-row-light' : 'table-row-dark';
    },
    
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.getUserList();
    },
    getVal(val, filed) {
      this.params[`${filed}`] = val.target.value;
    },
    //   查询
    onSearch() {
      this.pagination.current = 1;
      this.getUserList();
    },
    createUser(obj) {
      this.currentUser = obj;
      this.userModalTitle = JSON.stringify(obj) === "{}" ? "添加用户" : "编辑用户";
      this.userModalVisible = true;
    },
    delectUser(obj) {
      const self = this;
      // 动态导入DelectUser组件，避免ESLint警告
      import('./commponents/delectUser.vue').then(DelectUser => {
      let content = (
          <DelectUser.default
          sysTypeTxt="用户"
          detail={obj}
          callBack={() => self.getUserList()}
        />
      );
      this.$confirm({
          width: 400,
        title: () => {
          return (
              <div class="delete-title">
              <a-icon
                  type="exclamation-circle"
                  theme="filled"
                  class="warning-icon"
              />
                <span>确认删除</span>
            </div>
          );
        },
        content,
        closable: true,
          okButtonProps: {style: {display: 'none'}},
          cancelButtonProps: {style: {display: 'none'}},
        icon: () => {
          return <div />;
        },
      });
      });
    },
    handleUserModalOk() {
      this.userModalVisible = false;
      this.getUserList();
    },
    handleUserModalCancel() {
      this.userModalVisible = false;
    },
    getUserList() {
      this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        username: this.params.username || "",
      };
      this.username =  this.user.username
      this.$axiosPost(global.API.getUserList, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        this.pagination.total = res.total;
      });
    },
  },
  mounted() {
    this.getUserList();
  },
};
</script>

<style lang="less" scoped>
.user-management {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', Arial, sans-serif;
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
  
  /* 页面头部样式 */
  .page-header {
    background: #ffffff;
    padding: 32px;
    border-radius: 16px;
    margin-bottom: 24px;
    border: 1px solid rgba(0, 0, 0, 0.06);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    
    .header-content {
      .page-title {
        font-size: 28px;
        font-weight: 600;
        color: #1f2937;
        margin: 0 0 8px 0;
        letter-spacing: -0.025em;
      }

      .page-description {
        color: #6b7280;
        margin: 0;
        font-size: 15px;
        line-height: 1.5;
      }
    }
  }
  
  /* 搜索和操作区域 */
  .search-action-container {
    background: #ffffff;
    border-radius: 16px;
    padding: 24px;
    margin-bottom: 24px;
    border: 1px solid rgba(0, 0, 0, 0.06);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .search-box {
      display: flex;
      align-items: center;
      
      .search-input {
        width: 300px;
        margin-right: 12px;
        border-radius: 8px;
        
        /deep/ .ant-input {
          height: 40px;
          font-size: 14px;
        }
        
        /deep/ .ant-input-prefix {
          color: #9ca3af;
          margin-right: 8px;
        }
      }
      
      .search-button {
        height: 40px;
        border-radius: 8px;
        background: #007AFF;
        border-color: #007AFF;
        
        &:hover, &:focus {
          background: #0056CC;
          border-color: #0056CC;
        }
      }
    }
    
    .action-box {
      .add-button {
        height: 40px;
        border-radius: 8px;
        background: #007AFF;
        border-color: #007AFF;
        font-weight: 500;
        
        &:hover, &:focus {
          background: #0056CC;
          border-color: #0056CC;
          transform: translateY(-1px);
          box-shadow: 0 2px 5px rgba(0, 86, 204, 0.2);
        }
        
        .anticon {
          margin-right: 6px;
        }
      }
    }
  }
  
  /* 用户列表卡片 */
  .user-list-card {
    background: #ffffff;
    border-radius: 16px;
    overflow: hidden;
    border: 1px solid rgba(0, 0, 0, 0.06);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    
    .table-container {
      padding: 8px;
      
      /* 表格样式调整 */
      /deep/ .ant-table {
        background: transparent;
        
        .ant-table-thead > tr > th {
          background: #f8fafc;
          color: #374151;
          font-weight: 600;
          padding: 16px;
          border-bottom: 1px solid #e5e7eb;
          transition: all 0.3s;
          
          &:hover {
            background: #f1f5f9;
          }
          
          &.ant-table-column-sort {
            background: #f0f7ff;
          }
        }
        
        .ant-table-tbody > tr > td {
          padding: 16px;
          border-bottom: 1px solid #f3f4f6;
          transition: all 0.3s;
        }
        
        .ant-table-tbody > tr.table-row-light {
          background: #ffffff;
          
          &:hover > td {
            background: #f8fafc;
          }
        }
        
        .ant-table-tbody > tr.table-row-dark {
          background: #fafafa;
          
          &:hover > td {
            background: #f3f4f6;
          }
        }
        
        /* 分页样式 */
        .ant-pagination {
          margin: 16px 0;
          
          .ant-pagination-item {
            border-radius: 8px;
            
            &-active {
              border-color: #007AFF;
              
              a {
                color: #007AFF;
              }
            }
          }
          
          .ant-pagination-prev, .ant-pagination-next {
            .ant-pagination-item-link {
              border-radius: 8px;
            }
          }
          
          .ant-pagination-options {
            .ant-select-selection {
              border-radius: 8px;
            }
          }
        }
      }
    }
  }
  
  /* 操作按钮样式 */
  .action-buttons {
    display: flex;
    justify-content: flex-start;
    gap: 8px;
    
    .edit-button, .delete-button {
      padding: 0 8px;
      height: 28px;
      line-height: 28px;
      font-size: 13px;
      border-radius: 6px;
      transition: all 0.3s;
      
      .anticon {
        margin-right: 4px;
    font-size: 12px;
      }
    }
    
    .edit-button {
      color: #0070f3;
      
      &:hover {
        background: #e6f7ff;
      }
      
      &.disabled {
        color: rgba(0, 0, 0, 0.25);
        
        &:hover {
          background: transparent;
        }
      }
    }
    
    .delete-button {
      color: #f5222d;
      
      &:hover {
        background: #fff1f0;
      }
      
      &.disabled {
        color: rgba(0, 0, 0, 0.25);
        
        &:hover {
          background: transparent;
        }
      }
    }
  }
}

/* 确认删除弹窗样式 */
/deep/ .delete-title {
  display: flex;
  align-items: center;
  
  .warning-icon {
    color: #faad14;
    font-size: 18px;
    margin-right: 8px;
  }
}

/* 蚂蚁UI自定义样式 */
/deep/ .ant-modal {
  .ant-modal-content {
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
    
    .ant-modal-header {
      padding: 16px 24px;
      background: #f9fafb;
      border-bottom: 1px solid #f3f4f6;
      
      .ant-modal-title {
        font-weight: 600;
        font-size: 16px;
        color: #1f2937;
      }
    }
    
    .ant-modal-body {
      padding: 24px;
    }
  }
}

/* 添加用户弹窗样式 */
/deep/ .apple-style-modal {
  .ant-modal-content {
    .ant-modal-body {
      padding: 0;
    }
  }
}
</style>
