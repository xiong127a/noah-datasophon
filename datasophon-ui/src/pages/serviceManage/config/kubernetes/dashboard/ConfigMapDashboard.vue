<template>
  <div class="resource-list">
    <!-- ConfigMap列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Config Maps</span>
        <div class="k8s-card-actions">
          <a-icon type="bars" class="k8s-action-icon" />
          <a class="k8s-card-collapse-icon">
            <a-icon type="minus" />
          </a>
        </div>
      </div>
      <div class="k8s-card-content">
        <a-spin :spinning="loading">
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
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script>
// import API from '@/api';

export default {
  name: 'ConfigMapDashboard',
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
          width: null,
          className: 'name-column',
          customRender: (text, record) => {
            const h = this.$createElement;
            if (!record || !record.objectMeta) {
              return h('span', { class: 'empty-value' }, '-');
            }
            return h('a', {
              on: { click: () => this.handleViewConfigMap(record) },
              attrs: { title: record.objectMeta.name },
              class: 'name-cell'
            }, record.objectMeta.name);
          }
        },
        {
          title: '标签',
          key: 'labels',
          width: null,
          className: 'labels-column',
          customRender: (text, record) => {
            const h = this.$createElement;
            const labels = record?.objectMeta?.labels;
            if (!labels || Object.keys(labels).length === 0) {
              return h('span', { class: 'empty-value' }, '-');
            }
            const tags = Object.entries(labels).map(([key, value]) => {
              return h('a-tag', {
                key: key,
                props: { color: 'blue' },
                class: 'label-tag',
                attrs: { title: `${key}: ${value}` }
              }, `${key}: ${value}`);
            });
            return h('div', { class: 'labels-container' }, tags);
          }
        },
        {
          title: '创建时间',
          key: 'creationTime',
          width: null,
          className: 'time-column',
          customRender: (text, record) => {
            const h = this.$createElement;
            if (!record || !record.objectMeta || !record.objectMeta.creationTimestamp) {
              return h('span', { class: 'empty-value' }, '-');
            }
            return h('span', {
              class: 'time-cell',
              attrs: { title: this.formatTime(record.objectMeta.creationTimestamp) }
            }, `${this.getDaysAgo(record.objectMeta.creationTimestamp)}`);
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
      
      // 转换为天数、小时、分钟
      const days = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((timeDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((timeDiff % (1000 * 60 * 60)) / (1000 * 60));
      
      // 根据时间差返回不同格式
      if (days > 0) {
        return `${days}天前`;
      } else if (hours > 0) {
        return `${hours}小时前`;
      } else if (minutes > 0) {
        return `${minutes}分钟前`;
      } else {
        return '刚刚';
      }
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

// ConfigMapList没有特有的样式，直接使用公共样式
</style> 