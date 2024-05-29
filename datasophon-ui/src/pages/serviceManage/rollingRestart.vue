
<template>
  <div style="padding-top: 20px">
    <a-form
      :label-col="labelCol"
      :wrapper-col="wrapperCol"
      :form="form"
      class="p0-32-10-32 form-content"
    >
      <a-form-item label="实例数量">
        <a-input-number  :min="1" :max="10"
          id="batchCount"
          v-decorator="[
            'batchCount',
            {initialValue:rollingRestartParam.batchCount, rules: [{ required: true, message: '每个批次启动几个实例，不能为空!' }] },
          ]"
        />  每个批次启动几个实例
      </a-form-item>
      <a-form-item label="等待时间">
        <a-input-number  :min="0" :max="600"
          id="batchSeparationInSeconds"
          v-decorator="[
            'batchSeparationInSeconds',
            { initialValue:rollingRestartParam.batchSeparationInSeconds,rules: [{ required: true, message: '批次之间的执行间隔时间秒，不能为空!' }] },
          ]"
        />  批次之间的执行间隔时间秒
      </a-form-item>
      <a-form-item label="容错">
        <a-input-number  :min="0" :max="100"
          id="taskFailureTolerance"
          v-decorator="[
            'taskFailureTolerance',
            { initialValue:rollingRestartParam.taskFailureTolerance,rules: [{ required: true, message: '失败节点容错数量，不能为空!' }] },
          ]"
        />  失败节点容错数量
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
      rollingRestartParam:{
        batchCount:1,
        batchSeparationInSeconds:120,
        taskFailureTolerance:0,
      }
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
          const params = {
            batchCount: values.batchCount,
            batchSeparationInSeconds: values.batchSeparationInSeconds,
            taskFailureTolerance: values.taskFailureTolerance
          }
          console.log(params)
          _this.callBack(params);
        }
      });
      this.formCancel();
    }
  },
  mounted() {
  },
};
</script>
<style lang="less" scoped>
</style>
