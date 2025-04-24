<template>
  <div class="resource-list">
    <div class="resource-header">
      <h3>Config Maps</h3>
    </div>
    <a-spin :spinning="loading">
      <div v-if="!loading">
        <a-table
          :columns="configMapColumns"
          :dataSource="configMaps"
          :pagination="false"
          :rowKey="record => record?.objectMeta?.uid"
          class="k8s-table"
        >
          <!-- Remove scoped slots as we are using customRender now -->
          <!--
          <template #name="{ record }">
            <a @click="handleViewConfigMap(record)" :title="record.objectMeta?.name">{{ record.objectMeta?.name }}</a>
          </template>
          <template #labels="{ record }">
            <div class="tag-list" v-if="record?.objectMeta?.labels && Object.keys(record.objectMeta.labels).length > 0">
              <a-tag v-for="(value, key) in record.objectMeta.labels" :key="key" color="blue" class="label-tag truncate-tag" :title="`${key}: ${value}`">
                {{ key }}: {{ value }}
              </a-tag>
            </div>
            <span v-else>-</span>
          </template>
          <template #creationTime="{ record }">
            <span class="format-time-cell" :title="formatTime(record.objectMeta?.creationTimestamp)">
              {{ getDaysAgo(record.objectMeta?.creationTimestamp) }}天前
            </span>
          </template>
          -->
        </a-table>
        <a-empty v-if="configMaps.length === 0" description="暂无数据" style="margin-top: 20px;"/>
      </div>
      <div v-else style="text-align: center; padding: 50px;">
        <!-- Optional: You can add a placeholder or specific loading message here if needed -->
         <!-- <a-skeleton active /> -->
      </div>
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
          key: 'name',
          width: '30%',
          customRender: (text, record) => {
            const h = this.$createElement;
            if (!record || !record.objectMeta) {
              return h('span', '-');
            }
            return h('a', {
              on: { click: () => this.handleViewConfigMap(record) },
              attrs: { title: record.objectMeta.name }
            }, record.objectMeta.name);
          }
        },
        {
          title: '标签',
          key: 'labels',
          width: '40%',
          customRender: (text, record) => {
            const h = this.$createElement;
            const labels = record?.objectMeta?.labels;
            if (!labels || Object.keys(labels).length === 0) {
              return h('span', '-');
            }
            const tags = Object.entries(labels).map(([key, value]) => {
              return h('a-tag', {
                key: key,
                props: { color: 'blue' },
                class: 'label-tag truncate-tag',
                attrs: { title: `${key}: ${value}` }
              }, `${key}: ${value}`);
            });
            return h('div', { class: 'tag-list' }, tags);
          }
        },
        {
          title: '创建时间',
          key: 'creationTime',
          width: '30%',
          customRender: (text, record) => {
            const h = this.$createElement;
            if (!record || !record.objectMeta || !record.objectMeta.creationTimestamp) {
              return h('span', '-');
            }
            return h('span', {
              class: 'format-time-cell',
              attrs: { title: this.formatTime(record.objectMeta.creationTimestamp) }
            }, `${this.getDaysAgo(record.objectMeta.creationTimestamp)}天前`);
          }
        }
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
        
        if (res.code === 200 && res.data && Array.isArray(res.data.items)) {
          // 适配新的API响应格式，并过滤无效数据
          this.configMaps = res.data.items.filter(item => item && item.objectMeta);
        } else {
          console.error('获取配置映射列表失败:', res ? res.msg : '未知错误或数据格式无效');
          this.configMaps = [];
        }
      } catch (error) {
        console.error('获取配置映射列表异常:', error);
        this.$message.error('获取配置映射列表失败');
        this.configMaps = [];
      } finally {
        this.loading = false;
      }
    },
    handleViewConfigMap(record) {
      this.$message.info(`查看ConfigMap ${record.objectMeta?.name} 的功能正在开发中`);
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
      return Math.floor(timeDiff / (1000 * 60 * 60 * 24));
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

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    
    .label-tag {
      margin: 2px;
    }
  }
}

.format-time-cell {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.truncate-tag {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #f5f7fa;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #e8e8e8;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background-color: #f5f7fa;
}
</style> 