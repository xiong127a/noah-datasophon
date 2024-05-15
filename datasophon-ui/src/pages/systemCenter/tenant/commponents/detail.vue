<template>
  <div class="container">
    <div class="module">
      <a-tabs v-model="tabType">
        <a-tab-pane key="HDFS" tab="HDFS"></a-tab-pane>
        <a-tab-pane key="YARN" tab="YARN"></a-tab-pane>
        <a-tab-pane key="HBASE" tab="HBASE"></a-tab-pane>
        <a-tab-pane key="HIVE" tab="HIVE"></a-tab-pane>
        <a-tab-pane key="KAFKA" tab="KAFKA"></a-tab-pane>
      </a-tabs>
      <div class="moduleItems" v-show="tabType == 'HDFS'">
        <div class="right">
          <div class="title">
            <span>路径</span>
            <span>存储空间配额(GB)</span>
          </div>
          <div class="content" v-for="(item, index) in resourceList.hdfsResourceList" :key="index">
            <span style="width:30%">{{ item.hdfsPath }}</span>
            <span style="width:70%">{{ item.hdfsSpaceQuota }}</span>
          </div>
        </div>
      </div>
      <div class="moduleItems" v-show="tabType == 'YARN'">
        <div class="right">
          <div class="title">
            <span>父队列名称</span>
            <span>队列名称</span>
            <span>占比</span>
            <span>标签</span>
          </div>
          <div class="content" v-for="(item, index) in resourceList.yarnResourceList" :key="index">
            <span>{{ item.parentQueueName }}</span>
            <span>{{ item.queueName }}</span>
            <span>{{ item.capacityPercent }}</span>
            <span>{{ item.nodeLabel }}</span>
          </div>
        </div>

      </div>
      <div class="moduleItems" v-show="tabType == 'HBASE'">
        <div class="right">
          <div class="title">
            <span>命名空间</span>
            <span>容量(GB)</span>
            <span>RegionServer数量(个)</span>
          </div>
          <div class="content" v-for="(item, index) in resourceList.hbaseResourceList" :key="index">
            <span>{{ item.hbaseNamespace }}</span>
            <span>{{ item.hbaseCapacity }}</span>
            <span>{{ item.hbaseRegionServerNum }}</span>
          </div>
        </div>

      </div>
      <div class="moduleItems" v-show="tabType == 'HIVE'">
        <div class="right">
          <div class="title">
            <span>数据库名</span>
            <span>数据库容量</span>
          </div>
          <div class="content" v-for="(item, index) in resourceList.hiveResourceList" :key="index">
            <span>{{ item.hiveDatabase }}</span>
            <span>{{ item.hiveDatabaseCapacity }}</span>
          </div>
        </div>
      </div>
      <div class="moduleItems" v-show="tabType == 'KAFKA'">
        <div class="right">
          <div class="title">
            <span>topic名称</span>
            <span>存储容量(GB)</span>
            <span>副本数(个)</span>
          </div>
          <div class="content" v-for="(item, index) in resourceList.kafkaResourceList" :key="index">
            <span>{{ item.kafkaTopicName }}</span>
            <span>{{ item.kafkaTopicCapacity }}</span>
            <span>{{ item.kafkaReplicas }}</span>
          </div>
        </div>
      </div>
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
    callBack: Function,
  },
  data () {
    return {
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      tabType: 'HDFS',
      resourceList: {},
    };
  },
  methods: {
    // 点击编辑
    echoUSer () {
      console.log('detaqq', this.detail);
      this.resourceList = { ...this.detail }
    },

  },
  mounted () {
    this.echoUSer();
  },
};
</script>
<style lang="less" scoped>
.container {
  text-align: start;
}

.module {
  width:80%;
  padding: 2px 30px;
  margin-bottom: 40px;
}
.module .moduleItems {
  display: flex;
  border-bottom: 1px solid #f0eded;
  border-top: none;
}

.module .moduleItems .name {
  flex: .15;
  font-weight: 900;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.module .moduleItems .right {
  flex: 1;
}

.module .moduleItems .right .title {
  width: 100%;
  display: flex;
  justify-content: space-around;
  border-bottom: 1px solid #f0eded;
}

.module .moduleItems .right .title span {
  display: inline-block;
  flex: .5;
  text-align: center;
  padding: 10px;
}

.module .moduleItems .right .title .handle {
  display: inline-block;
  flex: .1;
}

.module .moduleItems .right .content {
  display: flex;
  justify-content: space-around;
  align-items: center;

}

.module .moduleItems .right .content span {
  display: inline-block;
  padding: 10px;
  flex: .5;
  text-align: center;
  border-bottom: 1px solid #f0eded;
}

.module .moduleItems .right .content:nth-last-child(1) span {
  border-bottom: none;
}

.module .moduleItems .right .content .btn-opt {
  width: 5%;
}
</style>
