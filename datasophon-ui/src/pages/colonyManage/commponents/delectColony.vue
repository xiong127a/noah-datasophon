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


 * @Date: 2022-06-08 17:37:26
 * @LastEditTime: 2022-06-10 14:38:13
 * @FilePath: \ddh-ui\src\pages\colonyManage\commponents\delectColony.vue
-->
<template>
  <div class="compact-confirm-dialog">
    <div class="confirm-content">
      <span>确认删除当前{{ sysTypeTxt }}？</span>
    </div>
    <div class="confirm-buttons">
      <a-button
        type="primary"
        class="mac-btn primary-btn"
        @click.stop="handleSubmit"
        >确定</a-button>
      <a-button class="mac-btn" @click.stop="formCancel">取消</a-button>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    callBack: Function,
    sysTypeTxt: String,
    detail: Object,
  },
  data() {
    return {
      form: this.$form.createForm(this),
    };
  },
  methods: {
    handleSubmit(e) {
      let self = this;
      e.preventDefault();
      const params  = JSON.stringify([this.detail.id])
      this.$axiosPostUpload(global.API.deleteColony+"?clusterId="+this.detail.id, params)
        .then((res) => {
          this.loading = false;
          if (res.code === 200) {
            this.$message.success("删除成功", 2);
            this.$destroyAll();
            self.callBack();
          }
        })
        .catch((err) => {});
    },
    formCancel() {
      this.$destroyAll();
    },
  },
  mounted() {},
};
</script>
<style lang="less" scoped>
.compact-confirm-dialog {
  display: flex;
  flex-direction: column;
  padding: 20px 0;
  
  .confirm-content {
    padding: 5px 15px 20px;
    text-align: center;
    font-size: 15px;
    
    span {
      white-space: nowrap;
    }
  }
  
  .confirm-buttons {
    display: flex;
    justify-content: center;
    gap: 12px;
    padding-top: 10px;
    
    .ant-btn {
      min-width: 80px;
      border-radius: 10px;
      transition: all 0.3s;
      
      &.mac-btn {
        height: 32px;
        font-size: 14px;
        border-radius: 10px;
        
        &.primary-btn {
          background: #1890ff;
          border-color: #1890ff;
          color: white;
          
          &:hover {
            background: #40a9ff;
            border-color: #40a9ff;
          }
        }
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
        }
      }
    }
  }
}
</style>
