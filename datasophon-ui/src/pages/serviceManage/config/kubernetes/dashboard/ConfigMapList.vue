<template>
  <div class="resource-list">
    <div class="resource-header">
      <h3>Config Maps</h3>
    </div>
    <a-spin :spinning="loading">
      <a-table
        :columns="configMapColumns"
        :dataSource="configMaps"
        :pagination="false"
        :rowKey="record => `${record.namespace}-${record.name}`"
        class="k8s-table"
      >
        <template #action="{ record }">
          <div class="action-buttons">
            <a @click="handleViewConfigMap(record)">查看</a>
            <a-divider type="vertical" />
            <a @click="handleEditConfigMap(record)">编辑</a>
          </div>
        </template>
        <template #labels="{ text }">
          <div class="tag-list" v-if="text && Object.keys(text).length > 0">
            <a-tag v-for="(value, key) in text" :key="key" color="blue" class="label-tag truncate-tag" :title="`${key}: ${value}`">
              {{ key }}: {{ value }}
            </a-tag>
          </div>
          <span v-else>-</span>
        </template>
        <template #time="{ text }">
          <span class="format-time-cell" :title="formatTime(text)">{{ formatTime(text) }}</span>
        </template>
      </a-table>
    </a-spin>
  </div>
</template>

<script>
// import API from '@/api';

export default {
  name: 'ConfigMapList',
  props: {
    clusterId: {
      type: Number,
      required: true
    },
    selectedNamespace: {
      type: String,
      default: 'datasophon'
    }
  },
  data() {
    return {
      configMaps: [],
      configMapsTotalItems: 0,
      loading: false,
      configMapColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '35%',
          slots: { customRender: 'labels' }
        },
        {
          title: '创建时间',
          key: 'creationTime',
          width: '10%',
          sorter: true,
          className: 'normal-column-header', // 添加自定义类名
          customRender: (text, record) => {
            // 获取创建时间
            const timestamp = record.objectMeta?.creationTimestamp;
            if (!timestamp) return '-';
            
            // 格式化为 "x天前" 的形式
            const days = this.getDaysAgo(timestamp);
            return this.$createElement('span', { style: 'white-space: nowrap;' }, `${days}天前`);
          }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ]
    };
  },
  mounted() {
    this.fetchConfigMaps();
  },
  methods: {
    async fetchConfigMaps() {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sConfigMaps, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
        });
        if (res.code === 200 && res.data) {
          // 确保获取configmaps列表，并设置表格数据
          this.configMaps = res.data.configMaps || [];
          this.configMapsTotalItems = res.data.listMeta ? res.data.listMeta.totalItems : 0;
        } else {
          console.error('获取配置映射列表失败:', res ? res.msg : '未知错误');
          this.configMaps = [];
          this.configMapsTotalItems = 0;
        }
      } catch (error) {
        console.error('获取配置映射列表异常:', error);
        this.$message.error('获取配置映射列表失败');
        this.configMaps = [];
        this.configMapsTotalItems = 0;
      } finally {
        this.loading = false;
      }
    },
    handleViewConfigMap(record) {
      // TODO: 实现查看ConfigMap的逻辑
      this.$message.info(`查看ConfigMap ${record.name} 的功能正在开发中`);
    },
    handleEditConfigMap(record) {
      // TODO: 实现编辑ConfigMap的逻辑
      this.$message.info(`编辑ConfigMap ${record.name} 的功能正在开发中`);
    },
    formatTime(time) {
      if (!time) return '-';
      return new Date(time).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    },
    getDaysAgo(timestamp) {
      if (!timestamp) return '-';
      
      const date = new Date(timestamp);
      const now = new Date();
      
      // 计算时间差（毫秒）
      const timeDiff = Math.abs(now - date);
      
      // 转换为天数
      const days = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
      
      return days;
    }
  },
  watch: {
    selectedNamespace() {
      this.fetchConfigMaps();
    },
    clusterId() {
      this.fetchConfigMaps();
    }
  }
};
</script>

<style lang="less" scoped>
@import './styles/k8s-table-styles.less';

.resource-list {
  height: 100%;

  .resource-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 0;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 500;
      color: #333;
    }
  }

  .action-buttons {
    white-space: nowrap;
    
    a {
      color: #1890ff;
      
      &:hover {
        color: #40a9ff;
      }
    }
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    
    .label-tag {
      margin: 2px;
    }
  }
}
</style> 