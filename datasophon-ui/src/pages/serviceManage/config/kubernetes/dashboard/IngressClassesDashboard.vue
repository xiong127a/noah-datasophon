<template>
  <div class="resource-list">
    <!-- IngressClasses列表区域 -->
    <div class="kubernetes-dashboard-card kubernetes-resource-card">
      <div class="kubernetes-card-header">
        <span class="kubernetes-card-title">Ingress Classes</span>
        <div class="kubernetes-card-actions">
          <a-icon type="bars" class="kubernetes-action-icon" />
          <a class="kubernetes-card-collapse-icon">
            <a-icon type="minus" />
          </a>
        </div>
      </div>
      <div class="kubernetes-card-content">
        <a-spin :spinning="loading">
          <a-table 
            :columns="columns" 
            :dataSource="ingressClasses" 
            :pagination="false"
            :rowKey="getRowKey"
            class="kubernetes-table"
            :table-layout="'auto'"
            :bordered="false"
            size="middle"
          >
            <template slot="name" slot-scope="text, record">
              <span class="name-text" :title="getObjectMetaName(record) || '未知'">
                {{ getObjectMetaName(record) || '未知' }}
              </span>
            </template>

            <template slot="controller" slot-scope="text, record">
              <span class="controller-cell" :title="getControllerName(record) || '-'">
                {{ getControllerName(record) || '-' }}
              </span>
            </template>

            <template slot="creationTime" slot-scope="text, record">
              <span class="time-cell" :title="formatTime(getCreationTimestamp(record))">
                {{ getDaysAgo(getCreationTimestamp(record)) }}
              </span>
            </template>
          </a-table>
          
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
export default {
  name: 'IngressClassesDashboard',
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
      ingressClasses: [],
      loading: false,
      // 分页相关数据
      pageNum: 1, // 当前页码
      pageSize: 10, // 每页记录数
      totalItems: 0, // 总记录数
      totalPages: 1, // 总页数
      columns: [
        {
          title: '名称',
          key: 'name',
          className: 'name-column',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: '控制器',
          key: 'controller',
          className: 'controller-column',
          scopedSlots: { customRender: 'controller' }
        },
        {
          title: '创建时间',
          key: 'creationTime',
          className: 'time-column',
          scopedSlots: { customRender: 'creationTime' }
        }
      ]
    };
  },
  mounted() {
    this.fetchIngressClasses();
  },
  methods: {
    async fetchIngressClasses() {
      this.loading = true;
      try {
        // 构建请求参数
        const params = {
          clusterId: this.clusterId,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        };
        
        const res = await this.$axiosGet(global.API.getKubernetesIngressClasses, params);
        
        if (res.code === 200) {
          // 确保获取IngressClasses列表数组
          let ingressClassesList = res.data && res.data.items ? res.data.items : [];
          
          // 处理数据，确保每个项都有必要的属性
          this.ingressClasses = ingressClassesList.map(ingressClass => {
            // 如果为null或undefined，返回一个空对象
            if (!ingressClass) return { objectMeta: {} };
            
            // 确保objectMeta存在
            if (!ingressClass.objectMeta) ingressClass.objectMeta = {};
            
            return ingressClass;
          });
          
          // 设置分页相关数据
          this.totalItems = res.data && res.data.total ? res.data.total : ingressClassesList.length;
          this.totalPages = res.data && res.data.totalPages ? res.data.totalPages : 1;
          
          console.log("处理后的ingressClasses数据:", this.ingressClasses);
          console.log("分页信息:", { pageNum: this.pageNum, pageSize: this.pageSize, totalItems: this.totalItems, totalPages: this.totalPages });
        } else {
          console.error('Failed to fetch IngressClasses:', res.msg);
          this.ingressClasses = [];
          this.totalItems = 0;
          this.totalPages = 1;
        }
      } catch (error) {
        console.error('Error fetching IngressClasses:', error);
        this.ingressClasses = [];
        this.totalItems = 0;
        this.totalPages = 1;
      } finally {
        this.loading = false;
      }
    },
    
    // 分页事件处理方法
    onPageChange(page) {
      this.pageNum = page;
      this.fetchIngressClasses();
    },
    
    onShowSizeChange(current, size) {
      this.pageNum = 1; // 重置到第一页
      this.pageSize = size;
      this.fetchIngressClasses();
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
    getRowKey(record) {
      return record && record.objectMeta && record.objectMeta.uid ? 
             record.objectMeta.uid : 
             Math.random().toString(36).substring(2);
    },
    
    getObjectMetaName(record) {
      return record && record.objectMeta ? record.objectMeta.name : null;
    },
    
    getControllerName(record) {
      return record ? record.controller : null;
    },
    
    getCreationTimestamp(record) {
      return record && record.objectMeta ? record.objectMeta.creationTimestamp : null;
    }
  },
  watch: {
    clusterId() {
      this.pageNum = 1; // 重置到第一页
      this.fetchIngressClasses();
    },
    selectedNamespace() {
      this.pageNum = 1; // 重置到第一页
      this.fetchIngressClasses();
    }
  }
};
</script>

<style lang="less" scoped>
@import 'styles/kubernetes-table-styles.less';

.name-text {
  cursor: pointer;
  display: inline-block;
  max-width: 100%;
  
  &:hover {
    color: #1890ff;
    text-decoration: underline;
  }
}

.controller-cell {
  word-break: break-word;
  line-height: 1.5;
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 分页容器样式 */
.pagination-container {
  margin-top: 16px;
  text-align: right;
}
</style> 