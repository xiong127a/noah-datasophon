<template>
  <div class="resource-list">
    <!-- IngressClasses列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Ingress Classes</span>
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
            :columns="columns" 
            :dataSource="ingressClasses" 
            :pagination="false"
            :rowKey="record => record?.objectMeta?.uid || Math.random().toString(36).substring(2)"
            class="k8s-table"
            :table-layout="'auto'"
            :bordered="false"
            size="middle"
          >
            <template slot="name" slot-scope="text, record">
              <span class="name-text" :title="record?.objectMeta?.name || '未知'">
                {{ record?.objectMeta?.name || '未知' }}
              </span>
            </template>

            <template slot="controller" slot-scope="text, record">
              <span class="controller-cell" :title="record?.controller || '-'">
                {{ record?.controller || '-' }}
              </span>
            </template>

            <template slot="creationTime" slot-scope="text, record">
              <span class="time-cell" :title="formatTime(record.objectMeta?.creationTimestamp)">
                {{ getDaysAgo(record.objectMeta?.creationTimestamp) }}
              </span>
            </template>
          </a-table>
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
        const res = await this.$axiosGet(global.API.getK8sIngressClasses, {
          clusterId: this.clusterId
        });
        
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
          
          console.log("处理后的ingressClasses数据:", this.ingressClasses);
        } else {
          console.error('Failed to fetch IngressClasses:', res.msg);
          this.ingressClasses = [];
        }
      } catch (error) {
        console.error('Error fetching IngressClasses:', error);
        this.ingressClasses = [];
      } finally {
        this.loading = false;
      }
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
    }
  },
  watch: {
    clusterId() {
      this.fetchIngressClasses();
    }
  }
};
</script>

<style lang="less" scoped>
@import './styles/k8s-table-styles.less';

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
</style> 