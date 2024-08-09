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
 * @LastEditTime: 2022-06-15 17:06:20
 * @FilePath: \ddh-ui\src\pages\colonyManage\commponents\addColony.vue
-->
<template>
  <div style="padding-top: 20px">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="form-content">
      <a-form-item label="集群名称">
        <a-input id="error" v-decorator="[
          'clusterName',
          { rules: [{ required: true, message: '集群名称不能为空!' }] },
        ]" placeholder="请输入集群名称" />
      </a-form-item>
      <a-form-item label="集群编码">
        <a-input id="error" v-decorator="[
          'clusterCode',
          { rules: [{ required: true, message: '集群编码不能为空!' }] },
        ]" placeholder="请输入集群编码" />
      </a-form-item>
      <a-form-item label="集群框架">
        <a-select v-decorator="['clusterFrame', { rules: [{ required: true, message: '集群框架不能为空!' }] }]"
          placeholder="请选择集群框架">
          <a-select-option :value="item.frameCode" v-for="(item, index) in frameList" :key="index">{{ item.frameCode
            }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="集群部署方式">
        <a-tooltip placement="top" >
          <template #title>
            <span>
              <p>PVM：适用于需要并行处理大规模计算任务的场景，支持将多个计算机资源组合成一个强大的计算集群。</p>
              <p>K8S：适用于需要管理和部署容器化应用程序的场景，强调的是自动化、可扩展和高可用的应用程序管理</p>
            </span>
          </template>
          <a-icon type="info-circle" class="iconInfo" />
        </a-tooltip>

        <a-select v-decorator="['depType', { rules: [{ required: true, message: '集群部署方式不能为空!' }] }]"
          placeholder="请选择集群部署方式">
          <a-select-option :value="item" v-for="(item, index) in depTypeList" :key="index">{{ item
            }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="kubernetes命名空间">
        <a-input v-decorator="['namespace', { rules: [{ required: true, message: 'kubernetes命名空间不能为空!' }] }]"
          placeholder="请输入kubernetes命名空间">
        </a-input>
      </a-form-item>
      <a-form-item label="kubeConfig">
        <a-textarea v-decorator="['kubeConfig', { rules: [{ required: true, message: 'kubeConfig不能为空!' }] }]"
          placeholder="请输入kubeConfig" style="width:100%;height: 300px;">
        </a-textarea>
      </a-form-item>
    </a-form>
    <div class="ant-modal-confirm-btns-new">
      <a-button
        style="margin-right: 10px"
        type="primary"
        @click.stop="handleSubmit"
        :loading="loading"
        >确认</a-button
      >
      <a-button @click.stop="formCancel">取消</a-button>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    detail: {
      type: Object,
      default: function () {
        return {};
      },
    },
    callBack:Function
  },
  data() {
    return {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 5 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 19 },
      },
      form: this.$form.createForm(this),
      value1: "",
      loading: false,
      frameList: [], //集群框架列表
      depTypeList: ['K8S', 'PVM'], //部署方式列表
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
        console.log(values);
        if (!err) {
          const params = {
            "clusterName": values.clusterName,
            "clusterCode": values.clusterCode,
            "clusterFrame": values.clusterFrame,
            "depType": values.depType,
            "namespace": values.namespace,
            "kubeConfig": values.kubeConfig,
          }
          if (JSON.stringify(this.detail) !== '{}') params.id = this.detail.id
          this.loading = true;
          const ajaxApi = JSON.stringify(this.detail) !== '{}' ? global.API.updateColony : global.API.saveColony
          this.$axiosJsonPost(ajaxApi+"?clusterId="+this.detail.id, params).then((res) => {
            this.loading = false;
            if (res.code === 200) {
              this.$message.success('保存成功', 2)
              this.$destroyAll();
              _this.callBack();
            }
          }).catch((err) => {});
        }
      });
    },
    getFrameList() {
      this.$axiosPost(global.API.getFrameList, {}).then((res) => {
        if (res.code === 200) {
          this.frameList = res.data
          if (JSON.stringify(this.detail) !== '{}') {
            this.form.getFieldsValue(['clusterName', 'clusterFrame', 'clusterCode', 'depType', 'namespace', 'kubeConfig',])
            this.form.setFieldsValue({
              clusterName:this.detail.clusterName,
              clusterFrame: this.detail.clusterFrame,
              clusterCode: this.detail.clusterCode,
              depType: this.detail.depType,
              namespace: this.detail.namespace,
              kubeConfig: this.detail.kubeConfig,
            })
          }
        }
      })
    }
  },
  mounted() {
    this.getFrameList()
  },
};
</script>
<style lang="less" scoped>
.form-content {
  padding: 0px 32px 10px 30px;
}

/deep/ .ant-form-item {
  position: relative;
}

.iconInfo {
  position: absolute;
  top: 0px;
  left: -17px;
  cursor: pointer;

  &:hover {
    color: 'red';
  }
}

/deep/ .ant-form-item-label {
  text-align: end;

}

/deep/ .ant-form-item-label>label {
  margin-right: 10px;

}
</style>
