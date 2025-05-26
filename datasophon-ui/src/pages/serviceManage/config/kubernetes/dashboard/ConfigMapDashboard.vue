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
            :rowKey="getRowKey"
            class="k8s-table"
            :bordered="false"
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
          
          <!-- 添加分页器 -->
          <div class="pagination-container" v-if="totalItems > 0">
            <a-pagination
              v-if="totalPages > 1"
              :current="pageNum"
              :pageSize="pageSize"
              :total="totalItems"
              :showTotal="total => `共 ${total} 条记录`"
              :pageSizeOptions="['10', '20', '50', '100']"
              showSizeChanger
              @change="onPageChange"
              @showSizeChange="onShowSizeChange"
            />
          </div>
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
      loading: false,
      // 分页相关数据
      pageNum: 1, // 当前页码
      pageSize: 10, // 每页记录数
      totalItems: 0, // 总记录数
      totalPages: 1, // 总页数
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
        // 构建请求参数，添加分页相关参数
        const params = {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        };
        
        const res = await this.$axiosGet(global.API.getK8sConfigMaps, params);
        
        if (res.code === 200 && res.data) {
          // 确保获取ConfigMaps列表数组
          let configMapsList = res.data.items ? res.data.items : [];
          
          // 处理数据，确保每个项都有必要的属性
          this.configMaps = configMapsList.filter(item => item && item.objectMeta);
          
          // 设置分页相关数据
          this.totalItems = res.data.total ? res.data.total : configMapsList.length;
          this.totalPages = res.data.totalPages ? res.data.totalPages : 1;
          
          console.log("处理后的ConfigMaps数据:", this.configMaps);
          console.log("分页信息:", { 
            pageNum: this.pageNum, 
            pageSize: this.pageSize, 
            totalItems: this.totalItems, 
            totalPages: this.totalPages 
          });
        } else {
          console.error(res.msg || '获取ConfigMap列表失败');
          this.configMaps = [];
          this.totalItems = 0;
          this.totalPages = 1;
        }
      } catch (error) {
        console.error('获取ConfigMap列表异常');
        console.error('获取ConfigMap列表失败');
        this.configMaps = [];
        this.totalItems = 0;
        this.totalPages = 1;
      } finally {
        this.loading = false;
      }
    },
    
    // 分页事件处理方法
    onPageChange(page) {
      this.pageNum = page;
      this.fetchConfigMaps();
    },
    
    onShowSizeChange(current, size) {
      this.pageNum = 1; // 重置到第一页
      this.pageSize = size;
      this.fetchConfigMaps();
    },
    
    // 行键获取方法
    getRowKey(record) {
      return record && record.objectMeta && record.objectMeta.uid ? 
             record.objectMeta.uid : 
             Math.random().toString(36).substring(2);
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
      this.pageNum = 1; // 重置到第一页
      this.fetchConfigMaps();
    },
    clusterId() {
      this.pageNum = 1; // 重置到第一页
      this.fetchConfigMaps();
    }
  }
};
</script>

<style lang="less" scoped>
@import './styles/k8s-table-styles.less';

// ConfigMapList没有特有的样式，直接使用公共样式

/* 分页容器样式 */
.pagination-container {
  margin-top: 16px;
  text-align: right;
}
</style> 