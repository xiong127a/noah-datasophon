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


 * @Date: 2022-06-08 11:38:30
 * @LastEditTime: 2022-07-14 15:12:08
 * @FilePath: \ddh-ui\src\pages\securityCenter\commponents\addUser.vue
-->
<template>
  <div class="modal-container">
    <table class="form-table">
      <!-- 用户名 -->
      <tr class="form-table-row">
        <td class="form-table-label">
          <span class="indicator" :class="hasUsername ? 'indicator-green' : 'indicator-red'"></span>
          <span class="label-text">用户名称</span>
        </td>
        <td class="form-table-field">
          <a-input
            v-decorator="[
            'username',
            { rules: [{ required: true, message: '用户名称不能为空!' }, { validator: checkName }] },
            ]"
            placeholder="请输入用户名称"
            @change="checkUsername"
            :maxLength="20"
          />
        </td>
      </tr>

      <!-- 密码 -->
      <tr class="form-table-row">
        <td class="form-table-label">
          <span class="indicator" :class="hasPassword ? 'indicator-green' : 'indicator-red'"></span>
          <span class="label-text">用户密码</span>
        </td>
        <td class="form-table-field">
          <a-input
            type="password"
            :disabled="editFlag"
            v-decorator="['password',{ rules: [{ required: true, message: '用户密码不能为空!' }] }]"
            placeholder="请输入用户密码"
            @change="checkPassword"
            :maxLength="20"
          />
        </td>
      </tr>

      <!-- 邮箱 -->
      <tr class="form-table-row">
        <td class="form-table-label">
          <span class="indicator" :class="hasEmail ? 'indicator-green' : 'indicator-red'"></span>
          <span class="label-text">邮箱地址</span>
        </td>
        <td class="form-table-field">
          <a-input
            v-decorator="[
              'email',
              { rules: [
                { required: true, message: '邮箱不能为空!' },
                { pattern: new RegExp(/\w{3,}(\.\w+)*@[A-z0-9]+(\.[A-z]{2,5}){1,2}/), message: '请输入正确的邮箱地址' }
              ] }
            ]"
            placeholder="请输入邮箱"
            @change="checkEmail"
            :maxLength="30"
          />
        </td>
      </tr>

      <!-- 手机 -->
      <tr class="form-table-row">
        <td class="form-table-label">
          <span class="indicator" :class="hasPhone ? 'indicator-green' : 'indicator-red'"></span>
          <span class="label-text">手机号码</span>
        </td>
        <td class="form-table-field">
          <a-input
            v-decorator="['phone',{ rules: [{ required: true, message: '手机号码不能为空!' }] }]"
            placeholder="请输入手机号码"
            @change="checkPhone"
            :maxLength="11"
          />
        </td>
      </tr>
    </table>

    <!-- 按钮区域 -->
    <div class="button-container">
      <a-button @click="formCancel" class="btn-cancel">取 消</a-button>
      <a-button type="primary" @click="handleSubmit" :loading="loading" class="btn-submit">确 认</a-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AddUser',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    detail: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      editFlag: false,
      form: this.$form.createForm(this),
      loading: false,
      hasUsername: false,
      hasPassword: false,
      hasEmail: false,
      hasPhone: false,
    };
  },
  watch: {
    detail: {
      handler(newVal) {
        if (newVal && Object.keys(newVal).length) {
          this.editFlag = true;
          this.$nextTick(() => {
            this.echoUSer();
          });
        } else {
          this.editFlag = false;
        }
      },
      immediate: true,
    }
  },
  methods: {
    checkUsername(e) {
      this.hasUsername = !!e.target.value;
    },
    checkPassword(e) {
      this.hasPassword = !!e.target.value;
    },
    checkEmail(e) {
      this.hasEmail = !!e.target.value;
    },
    checkPhone(e) {
      this.hasPhone = !!e.target.value;
    },
    checkName(rule, value, callback) {
      const param = {
        userName: value,
      };
      if (value && value !== this.detail.username) {
        this.$axiosPost('/scm/getUserByName', param).then(res => {
          if (res.data.result) {
            callback('用户名已存在!');
          } else {
            callback();
      }
        });
      } else {
      callback();
      }
    },
    formCancel() {
      this.form.resetFields();
      this.$emit('cancel');
    },
    handleSubmit() {
      this.form.validateFields((err, values) => {
        if (!err) {
          let userInfo = Object.assign({}, values);
          this.loading = true;
          if (this.editFlag) {
            userInfo = Object.assign({}, this.detail, values);
            this.$axiosPost('scm/updateSysUser', userInfo).then(res => {
              this.loading = false;
              if (res.data.result) {
                this.formCancel();
                this.$message.success('编辑成功');
                this.$emit('ok');
              } else {
                this.$message.warning(res.data.message || '编辑失败');
              }
            }).catch(() => {
              this.loading = false;
            });
          } else {
            this.$axiosPost('scm/addSysUser', userInfo).then(res => {
              this.loading = false;
              if (res.data.result) {
                this.formCancel();
                this.$message.success('添加成功');
                this.$emit('ok');
              } else {
                this.$message.warning(res.data.message || '添加失败');
              }
            }).catch(() => {
              this.loading = false;
            });
          }
        }
      });
    },
    echoUSer() {
      if (JSON.stringify(this.detail) !== "{}") {
        this.form.setFieldsValue({
          username: this.detail.username,
          phone: this.detail.phone,
          password: '',
          email: this.detail.email,
        });
        // 设置指示灯状态
        this.hasUsername = !!this.detail.username;
        this.hasPhone = !!this.detail.phone;
        this.hasEmail = !!this.detail.email;
      }
    },
  },
  mounted() {
    if (JSON.stringify(this.detail) !== "{}") {
      this.editFlag = true;
      this.$nextTick(() => {
    this.echoUSer();
      });
    } else {
      // 初始状态为空，设置所有指示器为红色
      this.hasUsername = false;
      this.hasPassword = false;
      this.hasEmail = false;
      this.hasPhone = false;
    }
    
    // 确保指示器正确显示
    setTimeout(() => {
      const usernameInput = document.querySelector('input[placeholder="请输入用户名称"]');
      const passwordInput = document.querySelector('input[placeholder="请输入用户密码"]');
      const emailInput = document.querySelector('input[placeholder="请输入邮箱"]');
      const phoneInput = document.querySelector('input[placeholder="请输入手机号码"]');
      
      if (usernameInput) {
        usernameInput.addEventListener('input', (e) => {
          this.hasUsername = !!e.target.value;
        });
      }
      
      if (passwordInput) {
        passwordInput.addEventListener('input', (e) => {
          this.hasPassword = !!e.target.value;
        });
      }
      
      if (emailInput) {
        emailInput.addEventListener('input', (e) => {
          this.hasEmail = !!e.target.value;
        });
      }
      
      if (phoneInput) {
        phoneInput.addEventListener('input', (e) => {
          this.hasPhone = !!e.target.value;
        });
      }
    }, 500);
  },
};
</script>

<style lang="less" scoped>
.modal-container {
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif;
}

/* 使用表格布局强制左右排列 */
.form-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0 16px;
  margin-bottom: 20px;
}

.form-table-row {
  margin-bottom: 16px;
}

.form-table-label {
  width: 110px;
  text-align: right;
  padding-right: 12px;
  vertical-align: middle;
  
  .indicator {
    display: inline-block;
    width: 10px !important;
    height: 10px !important;
    border-radius: 50%;
    margin-right: 8px;
    box-sizing: content-box;
    border: 1px solid transparent;
    z-index: 1000;
    position: relative;
    
    &.indicator-red {
      background-color: #ff453a !important;
      box-shadow: 0 0 6px rgba(255, 69, 58, 0.8) !important;
      animation: pulse-red 1.5s infinite;
    }
    
    &.indicator-green {
      background-color: #32d74b !important;
      box-shadow: 0 0 6px rgba(50, 215, 75, 0.8) !important;
    }
  }

  .label-text {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.85);
    font-weight: 500;
  }
}

.form-table-field {
  width: auto;
  text-align: left;
  vertical-align: middle;
  
  /deep/ .ant-input {
    width: 100%;
    max-width: 320px;
    border-radius: 6px;
    height: 36px;
    border: 1px solid #d9d9d9;
    transition: all 0.3s;
    
    &:hover {
      border-color: #1890ff;
    }
    
    &:focus {
      border-color: #1890ff;
      box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
    }
  }
}

.button-container {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
  
  .btn-cancel {
    margin-right: 8px;
  }
  
  .btn-submit {
    background-color: #1890ff;
    border-color: #1890ff;
    
    &:hover, &:focus {
      background-color: #40a9ff;
      border-color: #40a9ff;
    }
  }
}

/* 脈動動畫 */
@keyframes pulse-red {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 69, 58, 0.9);
    transform: scale(0.95);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(255, 69, 58, 0);
    transform: scale(1.05);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 69, 58, 0);
    transform: scale(0.95);
  }
}
</style>
