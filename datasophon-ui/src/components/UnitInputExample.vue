<!--
 * @describe: 单位输入框组件示例
 -->
<template>
  <div class="unit-input-example">
    <div class="form-item-row">
      <div class="form-label">WebHDFS enabled</div>
      <div class="form-input">
        <a-checkbox v-model="webHdfsEnabled" />
      </div>
    </div>
    
    <div class="form-item-row">
      <div class="form-label">Hadoop Java 堆大小</div>
      <div class="form-input">
        <unit-input v-model="heapSize" unit="MB" />
      </div>
    </div>

    <div class="form-item-row">
      <div class="form-label">Access time precision</div>
      <div class="form-input">
        <a-input v-model="accessTimePrecision" />
      </div>
    </div>

    <div class="form-item-row">
      <div class="form-label">HDFS Maximum Checkpoint Delay</div>
      <div class="form-input">
        <unit-input v-model="checkpointDelay" unit="seconds" />
      </div>
    </div>

    <div class="form-item-row">
      <div class="form-label">Reserved space for HDFS</div>
      <div class="form-input">
        <unit-input v-model="reservedSpace" unit="bytes" />
      </div>
    </div>

    <div class="form-item-row">
      <div class="form-label">Block replication</div>
      <div class="form-input">
        <a-input v-model="blockReplication" />
      </div>
    </div>
  </div>
</template>

<script>
import UnitInput from './UnitInput.vue'

export default {
  name: 'UnitInputExample',
  components: {
    UnitInput
  },
  data() {
    return {
      webHdfsEnabled: true,
      heapSize: '1024',
      accessTimePrecision: '0',
      checkpointDelay: '21600',
      reservedSpace: '479860077056',
      blockReplication: '3'
    }
  }
}
</script>

<style lang="less" scoped>
.unit-input-example {
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.form-item-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
  
  &:last-child {
    border-bottom: none;
  }
}

.form-label {
  width: 250px;
  font-weight: 500;
  color: #333;
  padding-left: 8px;
}

.form-input {
  flex: 1;
}
</style> 