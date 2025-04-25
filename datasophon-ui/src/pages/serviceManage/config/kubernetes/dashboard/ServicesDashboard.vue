<template>
  <div class="resource-list">
    <!-- Services列表区域 -->
    <div class="k8s-dashboard-card k8s-resource-card">
      <div class="k8s-card-header">
        <span class="k8s-card-title">Services</span>
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
            :columns="serviceColumns"
            :dataSource="services"
            :pagination="false"
            :rowKey="record => `${record.namespace}-${record.name}`"
            class="k8s-table"
          >
            <template #action="{ record }">
              <div class="action-buttons">
                <a @click="handleViewService(record)">查看</a>
                <a-divider type="vertical" />
                <a @click="handleEditService(record)">编辑</a>
              </div>
            </template>
            <template #name="{ text, record }">
              <div class="resource-name">
                <div class="resource-icon">
                  <span class="k8s-icon">K8S</span>
                </div>
                <a @click="viewDeployment(record)" :title="text">{{ text }}</a>
              </div>
            </template>
            <template #labels="{ text }">
              <div class="label-container">
                <a-tag v-for="(value, key) in text" :key="key" color="blue" class="label-tag truncate-tag" :title="`${key}: ${value}`">
                  {{ key }}: {{ value }}
                </a-tag>
              </div>
            </template>
            <template #status="{ record }">
              <a-tag :color="getStatusColor(record)" :title="getStatusText(record)">
                {{ getStatusText(record) }}
              </a-tag>
            </template>
            <template #creationTime="{ record }">
              <span class="format-time-cell">
                {{ formatTime(record.objectMeta?.creationTimestamp) }}
              </span>
            </template>
            <template #internalEndpoints="{ record }">
              <!-- 显示内部端点 -->
              <div v-if="record.internalEndpoint && record.internalEndpoint.ports && record.internalEndpoint.ports.length > 0">
                <div v-for="(port, index) in record.internalEndpoint.ports" :key="index">
                  <div class="internal-endpoint" :title="`${record.internalEndpoint.host}:${port.port} ${port.protocol}`">
                    {{record.internalEndpoint.host}}:{{port.port}} {{port.protocol}}
                  </div>
                  <div v-if="port.nodePort" class="internal-endpoint" :title="`${record.internalEndpoint.host}:${port.nodePort} ${port.protocol}`">
                    {{record.internalEndpoint.host}}:{{port.nodePort}} {{port.protocol}}
                  </div>
                </div>
              </div>
              <span v-else>-</span>
            </template>
            <template #image="{ text }">
              <span class="long-text-cell" :title="text || '-'">{{ text || '-' }}</span>
            </template>
          </a-table>
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script>
// import API from '@/api';

export default {
  name: 'ServiceList',
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
      services: [],
      serviceTotalItems: 0,
      loading: false,
      serviceColumns: [
        {
          title: '名称',
          dataIndex: ['objectMeta', 'name'],
          key: 'name',
          width: null,
          className: 'name-column',
          customRender: (text, record) => {
            // 绿色状态点和名称一起显示
            return this.$createElement('div', { class: 'name-cell', style: { display: 'flex', alignItems: 'center' } }, [
              this.$createElement('span', { 
                class: ['status-dot', 'status-running']
              }),
              this.$createElement('span', { attrs: { title: text || '未知' } }, text || '未知')
            ]);
          }
        },
        {
          title: '标签',
          key: 'labels',
          width: null,
          className: 'labels-column',
          customRender: (text, record) => {
            if (!record.objectMeta?.labels || Object.keys(record.objectMeta.labels).length === 0) {
              return this.$createElement('span', { class: 'empty-value' }, '-');
            }
            
            // 使用a-tag组件来模拟原始K8s Dashboard中的mat-chip组件
            const tags = Object.entries(record.objectMeta.labels).map(([key, value]) => {
              return this.$createElement('a-tag', { 
                props: { color: 'blue' },
                class: 'label-tag',
                key: key,
                attrs: { title: `${key}: ${value}` }
              }, `${key}: ${value}`);
            });
            
            return this.$createElement('div', { class: 'labels-container' }, tags);
          }
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: null,
          customRender: (text) => {
            return this.$createElement('span', { attrs: { title: text || 'NodePort' } }, text || 'NodePort');
          }
        },
        {
          title: '集群 IP',
          dataIndex: 'clusterIP',
          key: 'clusterIP',
          width: null,
          customRender: (text) => {
            return this.$createElement('span', { attrs: { title: text || '-' } }, text || '-');
          }
        },
        {
          title: '内部 Endpoints',
          key: 'internalEndpoints',
          width: null,
          customRender: (text, record) => {
            // 显示内部端点
            if (!record.internalEndpoint || !record.internalEndpoint.ports || record.internalEndpoint.ports.length === 0) {
              return this.$createElement('span', { class: 'empty-value' }, '-');
            }
            
            const endpoints = [];
            
            // 完全按照Kubernetes Dashboard的方式实现内部端点显示
            record.internalEndpoint.ports.forEach(port => {
              // 创建内部端口文本
              const internalPortText = `${record.internalEndpoint.host}:${port.port} ${port.protocol}`;
              endpoints.push(this.$createElement('div', { 
                class: 'internal-endpoint',
                attrs: { title: internalPortText }
              }, internalPortText));
              
              // 如果存在nodePort，则显示nodePort端口
              if (port.nodePort) {
                const nodePortText = `${record.internalEndpoint.host}:${port.nodePort} ${port.protocol}`;
                endpoints.push(this.$createElement('div', { 
                  class: 'internal-endpoint',
                  attrs: { title: nodePortText }
                }, nodePortText));
              }
            });
            
            return this.$createElement('div', { style: { maxWidth: '100%', overflow: 'hidden' } }, endpoints);
          }
        },
        {
          title: '外部 Endpoints',
          key: 'externalEndpoints',
          width: null,
          customRender: (text, record) => {
            // 检查externalEndpoints是否为空数组
            const hasExternalEndpoints = record.externalEndpoints && record.externalEndpoints.length > 0;
            
            // 如果externalEndpoints不为空，显示外部端点
            if (hasExternalEndpoints) {
              const endpoints = [];
              
              record.externalEndpoints.forEach(endpoint => {
                if (endpoint.ports && endpoint.ports.length > 0) {
                  endpoint.ports.forEach(port => {
                    if (port.port) {
                      const portText = `${endpoint.host}:${port.port}`;
                      endpoints.push(this.$createElement('div', {}, [
                        this.$createElement('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.port}`,
                            target: '_blank',
                            rel: 'noopener noreferrer',
                            title: portText
                          },
                          class: 'external-endpoint'
                        }, [
                          this.$createElement('span', {}, portText),
                          this.$createElement('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                    
                    if (!port.port && port.nodePort) {
                      const nodePortText = `${endpoint.host}:${port.nodePort}`;
                      endpoints.push(this.$createElement('div', {}, [
                        this.$createElement('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.nodePort}`,
                            target: '_blank',
                            rel: 'noopener noreferrer',
                            title: nodePortText
                          },
                          class: 'external-endpoint'
                        }, [
                          this.$createElement('span', {}, nodePortText),
                          this.$createElement('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                  });
                } else {
                  endpoints.push(this.$createElement('div', {}, [
                    this.$createElement('a', { 
                      attrs: { 
                        href: `http://${endpoint.host}`,
                        target: '_blank',
                        rel: 'noopener noreferrer',
                        title: endpoint.host
                      },
                      class: 'external-endpoint'
                    }, [
                      this.$createElement('span', {}, endpoint.host),
                      this.$createElement('i', { class: 'anticon anticon-link external-icon' })
                    ])
                  ]));
                }
              });
              
              return this.$createElement('div', { style: { maxWidth: '100%', overflow: 'hidden' } }, endpoints);
            }
            
            return this.$createElement('span', { class: 'empty-value' }, '-');
          }
        },
        {
          title: '创建时间',
          key: 'createTime',
          dataIndex: ['objectMeta', 'creationTimestamp'],
          width: null,
          className: 'time-column',
          customRender: (text, record) => {
            // 获取创建时间
            const timestamp = record.objectMeta?.creationTimestamp;
            if (!timestamp) return this.$createElement('span', { class: 'empty-value' }, '-');
            
            // 返回包含title属性的span，鼠标悬停时显示精确日期
            return this.$createElement('span', { 
              class: 'time-cell',
              attrs: { title: this.formatTime(timestamp) }
            }, `${this.getDaysAgo(timestamp)}`);
          }
        }
      ]
    };
  },
  mounted() {
    this.fetchServices();
  },
  methods: {
    async fetchServices() {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sServices, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace
        });
        
        if (res.code === 200 && res.data) {
          // 处理服务数据
          this.services = res.data.services || [];
          this.serviceTotalItems = res.data.listMeta?.totalItems || 0;
        } else {
          this.services = [];
          this.serviceTotalItems = 0;
          console.error('获取服务列表失败:', res.msg);
        }
      } catch (error) {
        console.error('获取服务列表失败:', error);
        this.$message.error('获取服务列表失败');
        this.services = [];
        this.serviceTotalItems = 0;
      } finally {
        this.loading = false;
      }
    },
    handleViewService(record) {
      // TODO: 实现查看Service的逻辑
      this.$message.info(`查看Service ${record.name} 的功能正在开发中`);
    },
    handleEditService(record) {
      // TODO: 实现编辑Service的逻辑
      this.$message.info(`编辑Service ${record.name} 的功能正在开发中`);
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
    },
    getStatusColor(record) {
      // 实现根据记录状态返回相应颜色的逻辑
      return 'blue'; // 临时返回，实际实现需要根据实际情况判断
    },
    getStatusText(record) {
      // 实现根据记录状态返回相应文本的逻辑
      return 'Running'; // 临时返回，实际实现需要根据实际情况判断
    },
    viewDeployment(record) {
      // 实现查看Deployment的逻辑
      this.$message.info(`查看Deployment ${record.name} 的功能正在开发中`);
    }
  },
  watch: {
    selectedNamespace() {
      this.fetchServices();
    },
    clusterId() {
      this.fetchServices();
    }
  }
};
</script>

<style lang="less" scoped>
@import './styles/k8s-table-styles.less';

// 只保留特定于ServiceList的样式，其他的都从公共样式文件继承
.resource-name {
  display: flex;
  align-items: center;

  .resource-icon {
    margin-right: 8px;
    
    .k8s-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      border-radius: 4px;
      background-color: #1890ff;
      color: white;
      font-size: 12px;
      font-weight: bold;
    }
  }

  a {
    color: #1890ff;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.label-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style> 