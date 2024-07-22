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
 * @LastEditTime: 2022-07-13 18:35:19
 * @FilePath: \ddh-ui\src\pages\alarmManage\commponents\addGroup.vue
-->
<template>
  <div style="padding-top: 20px">
    <a-form
        :label-col="labelCol"
        :wrapper-col="wrapperCol"
        :form="form"
        class="p0-32-10-32 form-content"
    >
      <a-form-item label="通知组名称">
        <a-input
            id="noticeGroupName"
            v-decorator="[
            'noticeGroupName',
            { rules: [{ required: true, message: '通知组名称不能为空!' }] },
          ]"
            placeholder="请输入组名称"
            :maxLength="255"
        />
      </a-form-item>


      <a-form-item label="通知组用户">
        <a-select
            v-model="userIds"
                  labelInValue mode="multiple"
                  value="userIds"
                  v-decorator="['userIds', { rules: [{ required: true, message: '通知组类别不能为空!' }]}]"
                  placeholder="请选择通知组类别">

          <a-select-option :value="item.id" v-for="(item,index) in userList" :key="index" >{{ item.username }}
          </a-select-option>
        </a-select>

      </a-form-item>
    </a-form>
    <div class="ant-modal-confirm-btns-new">
      <a-button
          style="margin-right: 10px"
          type="primary"
          @click.stop="handleSubmit"
          :loading="loading"
      >确认
      </a-button
      >
      <a-button @click.stop="formCancel">取消</a-button>
    </div>
  </div>
</template>
<script>
import { ref } from 'vue';

export default {
  props: {
    detail: {
      type: Object,
      default: function () {
        return {};
      },
    },
    callBack: Function
  },
  data() {
    return {
      labelCol: {
        xs: {span: 24},
        sm: {span: 5},
      },
      wrapperCol: {
        xs: {span: 24},
        sm: {span: 19},
      },
      form: this.$form.createForm(this),
      value1: "",
      loading: false,
      userList: [],
      userIds: [],
    };
  },
  watch: {},
  methods: {
    formCancel() {
      this.$destroyAll();
    },
    handleSubmit(e) {
      const _this = this
      e.preventDefault();
      this.form.validateFields((err, values) => {
        if (!err) {
          let ids = values.userIds.map(item => {
            return {id: item.key};
          })
          console.log(this.userIds)
          const params = {
            "noticeGroupName": values.noticeGroupName,
            "userIds": ids
          }
          if (JSON.stringify(this.detail) !== '{}') params.id = this.detail.id

          this.loading = true;
          const ajaxApi = JSON.stringify(this.detail) !== '{}' ? global.API.updateNotice : global.API.saveNotice
          this.$axiosJsonPost(ajaxApi, params).then((res) => {
            this.loading = false;
            if (res.code === 200) {
              this.$message.success('保存成功', 2)
              this.$destroyAll();
              _this.callBack();
            }
          }).catch((err) => {
          });
        }
      });
    },

    getAlarmCate() {
      const params = {
        pageSize: 200,
        page: 1,
      };
      this.loading = true;
      this.$axiosPost(global.API.getUserList, params).then((res) => {
        this.loading = false;
        this.userList = res.data;

        if (JSON.stringify(this.detail) !== '{}') {

          this.form.setFieldsValue({
            noticeGroupName: this.detail.noticeGroupName,
            userIds: this.detail.userIds ? this.detail.userIds.map(user => {
              return { value: user.username, key: user.id };
            }) : [],
          })
        }
      });
    }
  },
  mounted() {
    this.getAlarmCate()
  },
};
</script>
<style lang="less" scoped>
</style>
