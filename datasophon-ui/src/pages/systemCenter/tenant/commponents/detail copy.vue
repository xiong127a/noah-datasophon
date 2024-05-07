<template>
  <div>
    <div class="module">
      <div class="titleTop">
        <span>组件名</span>
        <span>申请项</span>
      </div>
      <div class="moduleItems" v-show="resourceList.hdfsResourceList.length > 0">
        <div class="name">HDFS </div>
        <div class="right">
          <div class="title">
            <span>路径</span>
            <span>存储空间配额(GB)</span>
          </div>
          <div class="content" v-for="(item, index) in resourceList.hdfsResourceList" :key="index">
            <span style="width:50%">{{ item.hdfsPath }}</span>
            <span style="width:50%">{{ item.hdfsSpaceQuota }}</span>
          </div>
        </div>

      </div>
      <div class="moduleItems" v-show="resourceList.yarnResourceList.length > 0">
        <div class="name">YARN </div>
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
      <div class="moduleItems" v-show="resourceList.hbaseResourceList.length > 0">
        <div class="name">HBASE </div>
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
      <div class="moduleItems" v-show="resourceList.hiveResourceList.length > 0">
        <div class="name">HIVE </div>
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
      <div class="moduleItems" v-show="resourceList.kafkaResourceList.length > 0">
        <div class="name">KAFKA </div>
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
      tabType: 'hdfsResourceList',
      resourceList: {},
    };
  },
  methods: {
    // 点击编辑
    echoUSer () {
        this.resourceList = { ...this.detail }
    },

  },
  mounted () {
    this.echoUSer();
  },
};
</script>
<style lang="less" scoped>
.module {
  width: 90%;
}

.module .titleTop {
  font-weight: 900;
  width: 100%;
  background: #faf8f8;
  border-bottom: 1px solid #ccc;
  padding-bottom: 5px;
}

.titleTop span {
  display: inline-block;
  width: 15%;
  text-align: center;
}

.module .moduleItems {
  display: flex;
  border: 1px solid #ccc;
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
  flex: .85;
}

.module .moduleItems .right .title {
  width: 100%;
  display: flex;
  justify-content: space-around;
  border-bottom: 1px solid #ccc;
}

.module .moduleItems .right .title span {
  display: inline-block;
  flex: .5;
  text-align: center;
  padding: 10px;
  border-left: 1px solid #ccc;
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
  border-left: 1px solid #ccc;
  border-bottom: 1px solid #ccc;
}

.module .moduleItems .right .content:nth-last-child(1) span {
  border-bottom: none;
}

.module .moduleItems .right .content .btn-opt {
  width: 5%;
}

.module .moduleItems .clearDom {
  border-left: 1px solid #ccc;
  display: flex;
  align-items: center;
}

.module .moduleItems .clearDom .clearIcon {
  color: #db0315;
  font-size: 18px;
  vertical-align: middle;
  text-align: center;
  margin: 8px;

  :hover {
    color: @text-color;
  }
}

.plusIcon {
  color: #1979b9;
  font-size: 16px;
  vertical-align: middle;
  text-align: center;
  margin-left: 9px;

  :hover {
    color: @text-color;
  }
}
</style>
