<template>
  <div class="k8s-config-container">
    <!-- 顶部标题区域 -->
    <div class="page-header">
      <div class="header-icon-wrapper">
        <div class="kubernetes-logo"></div>
      </div>
      <div class="header-content">
        <h2 class="title">{{ serviceLabel || serviceName }} Kubernetes 仪表盘</h2>
        <p class="subtitle">查看和管理{{ serviceLabel || serviceName }}服务的Kubernetes资源</p>
      </div>
      <!-- 命名空间选择器 -->
      <div class="namespace-selector" style="display: none;"><!-- 隐藏命名空间选择器 -->
        <a-select
          v-model="selectedNamespace"
          placeholder="请选择命名空间"
          :loading="namespacesLoading"
          @change="handleNamespaceChange"
          style="width: 200px"
        >
          <a-select-option value="all">所有命名空间</a-select-option>
          <a-select-option v-for="ns in namespaces" :key="ns.name" :value="ns.name">
            {{ ns.name }}
          </a-select-option>
        </a-select>
      </div>
    </div>

    <div class="k8s-dashboard-layout">
      <!-- 左侧导航菜单 -->
      <k8s-sidebar-menu 
        :active-resource="activeResource"
        :resource-counts="resourceCounts"
        @resource-change="handleResourceChange"
      />

      <!-- 右侧内容区域 -->
      <div class="content-area">
        <!-- ConfigMap列表 -->
        <config-map-list
          v-if="activeResource === 'configmap'"
          :clusterId="clusterId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- Deployment仪表板 -->
        <deployment-dashboard
          v-if="activeResource === 'deployments'"
          :clusterId="clusterId"
          :serviceId="serviceId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- 其他资源列表 -->
        <!-- 这里将添加其他资源组件 -->
        
        <!-- Service列表 -->
        <service-list 
          v-if="activeResource === 'service'"
          :clusterId="clusterId"
          :selectedNamespace="selectedNamespace"
        />

        <!-- 其他资源列表保持不变... -->
        <!-- ... existing resources ... -->
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, reactive } from 'vue'
import DeploymentView from './DeploymentView.vue'
import dayjs from 'dayjs'
// 移除对已删除api.js的导入
// import API from '@/api';

// 导入拆分的组件
import K8sSidebarMenu from './K8sSidebarMenu.vue';
import ConfigMapList from './ConfigMapList.vue';
import DeploymentDashboard from './DeploymentDashboard.vue';
import ServiceList from './ServiceList.vue';

export default defineComponent({
  name: 'KubernetesDashboard',
  components: {
    DeploymentView,
    K8sSidebarMenu,
    ConfigMapList,
    DeploymentDashboard,
    ServiceList
  },
  props: {
    serviceId: {
      type: [Number, String],
      required: true
    },
    serviceName: {
      type: String,
      required: true,
      default: '未知服务'
    },
    serviceLabel: {
      type: String,
      default: ''
    },
    clusterId: {
      type: Number,
      required: true,
      default: 1
    }
  },
  data() {
    return {
      namespaces: [],
      selectedNamespace: 'datasophon', // 固定使用datasophon命名空间
      namespacesLoading: false,
      activeResource: 'configmap', // 默认显示ConfigMap
      // 工作负载
      cronJobs: [],
      daemonSets: [],
      jobs: [],
      pods: [],
      replicaSets: [],
      replicationControllers: [],
      statefulSets: [],
      // 服务
      services: [],
      pvcs: [],
      ingresses: [],
      ingressClasses: [],
      secrets: [],
      persistentVolumes: [],
      storageClasses: [],
      loading: false,
      // 工作负载表格列配置
      cronJobColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
          slots: { customRender: 'labels' }
        },
        {
          title: '计划',
          dataIndex: 'schedule',
          key: 'schedule',
          width: '15%',
          slots: { customRender: 'schedule' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      daemonSetColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '所需',
          dataIndex: 'desired',
          key: 'desired',
          width: '10%',
        },
        {
          title: '当前',
          dataIndex: 'current',
          key: 'current',
          width: '10%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      deploymentColumns: [
        {
          title: '',
          dataIndex: 'status',
          key: 'status',
          width: '20px',
          customRender: (text, record) => {
            const classNames = ['status-dot'];
            if (record?.pods?.running > 0) classNames.push('status-running');
            if (record?.pods?.pending > 0) classNames.push('status-warning');
            if (record?.pods?.failed > 0) classNames.push('status-danger');
            if (!record?.pods || (!record?.pods.running && !record?.pods.pending && !record?.pods.failed)) 
              classNames.push('status-unknown');
            return <span class={classNames.join(' ')}></span>;
          }
        },
        {
          title: '名称',
          dataIndex: 'objectMeta.name',
          key: 'name',
          width: '20%',
          customRender: (_, record) => {
            return <div class="name-cell">
              <span class="name-text">{record?.objectMeta?.name || '未知'}</span>
            </div>
          }
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '12%'
        },
        {
          title: '镜像',
          dataIndex: 'containerImages',
          key: 'image',
          width: '25%',
          customRender: (_, record) => {
            return <div class="image-cell">
              <a-tooltip title={record?.containerImages ? record.containerImages.join(', ') : ''}>
                <span class="image-text">{record?.containerImages ? record.containerImages.join(', ') : '-'}</span>
              </a-tooltip>
            </div>
          }
        },
        {
          title: '标签',
          key: 'labels',
          width: '20%',
          customRender: (text, record) => {
            if (!record.objectMeta?.labels || Object.keys(record.objectMeta.labels).length === 0) {
              return '-';
            }
            
            // 使用a-tag组件来模拟原始K8s Dashboard中的mat-chip组件
            return h('div', { class: 'labels-container' }, 
              Object.entries(record.objectMeta.labels).map(([key, value]) => {
                return h('a-tag', { 
                  props: { color: 'blue' },
                  class: 'label-chip',
                  key: key
                }, `${key}: ${value}`);
              })
            );
          }
        },
        {
          title: 'Pods',
          dataIndex: 'pods',
          key: 'pods',
          width: '10%',
          customRender: (_, record) => {
            return <div class="pods-display">
              <span>{record?.pods && record.pods.running !== undefined ? record.pods.running : 0} / {record?.pods && record.pods.desired !== undefined ? record.pods.desired : 0}</span>
            </div>
          }
        },
        {
          title: '内部 Endpoints',
          key: 'internalEndpoints',
          width: '15%',
          customRender: (text, record) => {
            // 显示内部端点
            if (!record.internalEndpoint || !record.internalEndpoint.ports || record.internalEndpoint.ports.length === 0) {
              return '-';
            }
            
            const endpoints = [];
            
            // 完全按照Kubernetes Dashboard的方式实现内部端点显示
            record.internalEndpoint.ports.forEach(port => {
              // 显示内部端口
              endpoints.push(h('div', { class: 'internal-endpoint' }, `${record.internalEndpoint.host}:${port.port} ${port.protocol}`));
              
              // 如果存在nodePort，则显示nodePort端口
              if (port.nodePort) {
                endpoints.push(h('div', { class: 'internal-endpoint' }, `${record.internalEndpoint.host}:${port.nodePort} ${port.protocol}`));
              }
            });
            
            return h('div', {}, endpoints);
          }
        },
        {
          title: '外部 Endpoints',
          key: 'externalEndpoints',
          width: '15%',
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
                      endpoints.push(h('div', {}, [
                        h('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.port}`,
                            target: '_blank',
                            rel: 'noopener noreferrer'
                          },
                          class: 'external-endpoint'
                        }, [
                          `${endpoint.host}:${port.port}`,
                          h('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                    
                    if (!port.port && port.nodePort) {
                      endpoints.push(h('div', {}, [
                        h('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.nodePort}`,
                            target: '_blank',
                            rel: 'noopener noreferrer'
                          },
                          class: 'external-endpoint'
                        }, [
                          `${endpoint.host}:${port.nodePort}`,
                          h('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                  });
                } else {
                  endpoints.push(h('div', {}, [
                    h('a', { 
                      attrs: { 
                        href: `http://${endpoint.host}`,
                        target: '_blank',
                        rel: 'noopener noreferrer'
                      },
                      class: 'external-endpoint'
                    }, [
                      endpoint.host,
                      h('i', { class: 'anticon anticon-link external-icon' })
                    ])
                  ]));
                }
              });
              
              return h('div', {}, endpoints);
            }
            
            return '-';
          }
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
            return h('span', { style: 'white-space: nowrap;' }, `${days}天前`);
          }
        }
      ],
      jobColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '完成度',
          dataIndex: 'completions',
          key: 'completions',
          width: '15%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      podColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '20%',
          slots: { customRender: 'labels' }
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '10%',
          slots: { customRender: 'status' }
        },
        {
          title: 'IP',
          dataIndex: 'ip',
          key: 'ip',
          width: '15%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      replicaSetColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '所需副本',
          dataIndex: 'desired',
          key: 'desired',
          width: '10%',
        },
        {
          title: '当前副本',
          dataIndex: 'current',
          key: 'current',
          width: '10%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      replicationControllerColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '所需副本',
          dataIndex: 'desired',
          key: 'desired',
          width: '10%',
        },
        {
          title: '当前副本',
          dataIndex: 'current',
          key: 'current',
          width: '10%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      statefulSetColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '副本',
          dataIndex: 'replicas',
          key: 'replicas',
          width: '10%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      // 现有的表格列配置
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
            return h('span', { style: 'white-space: nowrap;' }, `${days}天前`);
          }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      serviceColumns: [
        {
          title: '名称',
          dataIndex: ['objectMeta', 'name'],
          key: 'name',
          width: '15%',
          customRender: (text, record) => {
            // 绿色状态点和名称一起显示
            return h('div', { style: { display: 'flex', alignItems: 'center' } }, [
              h('span', { 
                class: ['status-dot'], 
                style: { 
                  backgroundColor: '#4caf50', 
                  width: '8px', 
                  height: '8px', 
                  borderRadius: '50%', 
                  display: 'inline-block',
                  marginRight: '8px'
                } 
              }),
              h('span', { class: 'cell-content', title: text || '未知' }, text || '未知')
            ]);
          }
        },
        {
          title: '标签',
          key: 'labels',
          width: '15%',
          customRender: (text, record) => {
            if (!record.objectMeta?.labels || Object.keys(record.objectMeta.labels).length === 0) {
              return '-';
            }
            
            // 使用a-tag组件来模拟原始K8s Dashboard中的mat-chip组件
            return h('div', { class: 'labels-container' }, 
              Object.entries(record.objectMeta.labels).map(([key, value]) => {
                return h('a-tag', { 
                  props: { color: 'blue' },
                  class: 'label-chip',
                  key: key
                }, `${key}: ${value}`);
              })
            );
          }
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: '10%',
          customRender: (text) => {
            return h('span', { class: 'cell-content', title: text || 'NodePort' }, text || 'NodePort');
          }
        },
        {
          title: '集群 IP',
          dataIndex: 'clusterIP',
          key: 'clusterIP',
          width: '10%',
          customRender: (text) => {
            return h('span', { class: 'cell-content', title: text || '-' }, text || '-');
          }
        },
        {
          title: '内部 Endpoints',
          key: 'internalEndpoints',
          width: '15%',
          customRender: (text, record) => {
            // 显示内部端点
            if (!record.internalEndpoint || !record.internalEndpoint.ports || record.internalEndpoint.ports.length === 0) {
              return '-';
            }
            
            const endpoints = [];
            
            // 完全按照Kubernetes Dashboard的方式实现内部端点显示
            record.internalEndpoint.ports.forEach(port => {
              // 创建内部端口文本
              const internalPortText = `${record.internalEndpoint.host}:${port.port} ${port.protocol}`;
              endpoints.push(h('div', { 
                class: 'internal-endpoint',
                title: internalPortText
              }, internalPortText));
              
              // 如果存在nodePort，则显示nodePort端口
              if (port.nodePort) {
                const nodePortText = `${record.internalEndpoint.host}:${port.nodePort} ${port.protocol}`;
                endpoints.push(h('div', { 
                  class: 'internal-endpoint',
                  title: nodePortText
                }, nodePortText));
              }
            });
            
            return h('div', { style: { maxWidth: '100%', overflow: 'hidden' } }, endpoints);
          }
        },
        {
          title: '外部 Endpoints',
          key: 'externalEndpoints',
          width: '10%',
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
                      endpoints.push(h('div', {}, [
                        h('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.port}`,
                            target: '_blank',
                            rel: 'noopener noreferrer',
                            title: portText
                          },
                          class: 'external-endpoint'
                        }, [
                          h('span', { class: 'cell-content' }, portText),
                          h('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                    
                    if (!port.port && port.nodePort) {
                      const nodePortText = `${endpoint.host}:${port.nodePort}`;
                      endpoints.push(h('div', {}, [
                        h('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.nodePort}`,
                            target: '_blank',
                            rel: 'noopener noreferrer',
                            title: nodePortText
                          },
                          class: 'external-endpoint'
                        }, [
                          h('span', { class: 'cell-content' }, nodePortText),
                          h('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                  });
                } else {
                  endpoints.push(h('div', {}, [
                    h('a', { 
                      attrs: { 
                        href: `http://${endpoint.host}`,
                        target: '_blank',
                        rel: 'noopener noreferrer',
                        title: endpoint.host
                      },
                      class: 'external-endpoint'
                    }, [
                      h('span', { class: 'cell-content' }, endpoint.host),
                      h('i', { class: 'anticon anticon-link external-icon' })
                    ])
                  ]));
                }
              });
              
              return h('div', { style: { maxWidth: '100%', overflow: 'hidden' } }, endpoints);
            }
            
            return '-';
          }
        },
        {
          title: '创建时间',
          key: 'createTime',
          dataIndex: ['objectMeta', 'creationTimestamp'],
          width: '15%',
          customRender: (text, record) => {
            // 获取创建时间
            const timestamp = record.objectMeta?.creationTimestamp;
            if (!timestamp) return '-';
            
            // 格式化为 "x天前" 的形式
            const days = this.getDaysAgo(timestamp);
            
            // 返回包含title属性的span，鼠标悬停时显示精确日期
            return h('span', { 
              class: 'format-time-cell', 
              style: 'white-space: nowrap;',
              title: this.formatTime(timestamp)
            }, `${days}天前`);
          }
        }
      ],
      ingressColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '主机',
          dataIndex: 'hosts',
          key: 'hosts',
          width: '25%',
        },
        {
          title: '地址',
          dataIndex: 'address',
          key: 'address',
          width: '15%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      ingressClassColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '控制器',
          dataIndex: 'controller',
          key: 'controller',
          width: '30%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
          slots: { customRender: 'labels' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      secretColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: '15%',
          slots: { customRender: 'type' }
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
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
            return h('span', { style: 'white-space: nowrap;' }, `${days}天前`);
          }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      pvColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
        },
        {
          title: '容量',
          dataIndex: 'capacity',
          key: 'capacity',
          width: '15%',
        },
        {
          title: '访问模式',
          dataIndex: 'accessModes',
          key: 'accessModes',
          width: '15%',
        },
        {
          title: '回收策略',
          dataIndex: 'reclaimPolicy',
          key: 'reclaimPolicy',
          width: '15%',
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '15%',
          slots: { customRender: 'status' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      pvcColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
          slots: { customRender: 'labels' }
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '15%',
          slots: { customRender: 'status' }
        },
        {
          title: '容量',
          dataIndex: 'capacity',
          key: 'capacity',
          width: '20%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      storageClassColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '供应商',
          dataIndex: 'provisioner',
          key: 'provisioner',
          width: '30%',
          slots: { customRender: 'provisioner' }
        },
        {
          title: '回收策略',
          dataIndex: 'reclaimPolicy',
          key: 'reclaimPolicy',
          width: '15%',
        },
        {
          title: '默认类',
          dataIndex: 'isDefault',
          key: 'isDefault',
          width: '15%',
          slots: { customRender: 'default' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      deploymentViewVisible: false,
      currentDeployment: {
        namespace: '',
        name: ''
      },
      // 添加图表相关属性
      cpuChart: null,
      memoryChart: null,
      metricsData: [], // 保存从API获取的指标数据
      serviceLoading: false,
      serviceErrors: [],
      serviceTotalItems: 0,
      resourceStats: {},
      resourceCounts: {},
      
      // 不再自定义API路径，使用全局API
    };
  },
  created() {
    // 不再需要初始化API路径
  },
  methods: {
    // 移除API路径初始化方法
    async fetchK8sResources() {
      // 获取命名空间列表
      this.fetchNamespaces();
        
      // 根据当前选中的资源类型加载数据
      this.updateResourceData();
    },
    // 获取命名空间
    async fetchNamespaces() {
      this.namespacesLoading = true;
      try {
        // 使用全局API对象替代导入的API
        const res = await this.$axiosGet(global.API.getK8sNamespaces, {
          clusterId: this.clusterId,
          serviceName: this.serviceName ? this.serviceName.toUpperCase() : this.serviceName
        });
        
        if (res && res.code === 200) {
          // 确保获取命名空间列表数组
          this.namespaces = res.data && Array.isArray(res.data) ? res.data : (res.data && res.data.namespaces ? res.data.namespaces : []);
        } else {
          console.error('Failed to fetch namespaces:', res ? res.msg : '无响应');
          this.namespaces = [];
        }
      } catch (error) {
        console.error('Error fetching namespaces:', error);
        this.namespaces = [];
      } finally {
        this.namespacesLoading = false;
      }
    },
    // 命名空间变化处理
    handleNamespaceChange(value) {
      this.selectedNamespace = value;
      this.fetchK8sResources(); // 重新加载资源
    },
    
    // 资源类型变化处理
    handleResourceChange(resource) {
      this.activeResource = resource;
      this.updateResourceData();
    },
    
    // 格式化时间
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
    
    // 获取几天前
    getDaysAgo(timestamp) {
      if (!timestamp) return '-';
      
      const date = new Date(timestamp);
      const now = new Date();
      
      // 计算时间差（毫秒）
      const timeDiff = Math.abs(now - date);
      
      // 转换为天数
      const days = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
      
      return days;
    },
    
    // 根据资源类型更新数据
    updateResourceData() {
      // 清除之前的数据
      this.loading = true;
      
      // 资源数据现在由独立组件处理
      // 组件通过v-if="activeResource === '资源类型'"条件渲染，所以不需要在这里调用fetch方法
      console.log(`资源类型已切换为: ${this.activeResource}`);
      
      
      this.loading = false;
    },
    
    // 获取所有资源统计数据
    async fetchResourceStats() {
      try {
        // 显示加载状态
        this.loading = true;
        
        // 确保clusterId存在
        if (!this.clusterId) {
          console.error('clusterId不能为空，当前值:', this.clusterId);
          // 尝试从localStorage获取
          this.clusterId = window.localStorage.getItem('clusterId');
          console.log('从localStorage获取的clusterId:', this.clusterId);
        }
        
        // 构建请求参数
        const params = {
          clusterId: this.clusterId,
          serviceId: this.serviceId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace
        };
        
        // 使用全局API对象
        const apiUrl = global.API.getK8sResourceStats;

        console.log('调用resource-stats接口，参数:', params);
        console.log('请求URL:', apiUrl);
        
        // 调用后端统计接口
        const res = await this.$axiosGet(apiUrl, params);
        
        console.log('resource-stats接口返回结果:', res);
        
        if (res && res.code === 200 && res.data) {
          // 更新资源数量统计数据（现在后端只返回数量而不是资源列表）
          this.resourceStats = res.data;
          
          // 更新资源计数显示
          this.resourceCounts = {
            deployments: res.data.deployments || 0,
            pods: res.data.pods || 0,
            services: res.data.services || 0,
            configMaps: res.data.configMaps || 0,
            ingresses: res.data.ingresses || 0,
            ingressClasses: res.data.ingressClasses || 0,
            secrets: res.data.secrets || 0,
            persistentVolumes: res.data.persistentVolumes || 0,
            persistentVolumeClaims: res.data.persistentVolumeClaims || 0,
            storageClasses: res.data.storageClasses || 0,
            cronJobs: res.data.cronJobs || 0,
            daemonSets: res.data.daemonSets || 0,
            jobs: res.data.jobs || 0,
            replicaSets: res.data.replicaSets || 0,
            replicationControllers: res.data.replicationControllers || 0,
            statefulSets: res.data.statefulSets || 0
          };
          
          console.log('资源统计数据加载成功:', res.data);
        } else {
          console.error('获取资源统计数据失败:', res ? res.msg : '返回结果为空');
          // 初始化空数据
          this.initEmptyResourceCounts();
        }
      } catch (error) {
        console.error('获取资源统计数据异常:', error);
        // 初始化空数据
        this.initEmptyResourceCounts();
      } finally {
        this.loading = false;
      }
    },
    
    // 初始化空的资源计数
    initEmptyResourceCounts() {
      this.resourceCounts = {
        deployments: 0,
        pods: 0,
        services: 0,
        configMaps: 0,
        ingresses: 0,
        ingressClasses: 0,
        secrets: 0,
        persistentVolumes: 0,
        persistentVolumeClaims: 0,
        storageClasses: 0,
        cronJobs: 0,
        daemonSets: 0,
        jobs: 0,
        replicaSets: 0,
        replicationControllers: 0,
        statefulSets: 0
      };
    },
    
    // 保留其他需要的方法
    // ... 保留其他未迁移的资源加载方法 ...
  },
  mounted() {
    if (this.serviceId) {
      // 首先获取所有资源统计数据
      this.fetchResourceStats();
      // 然后获取详细资源数据
      this.fetchK8sResources();
    } else {
      console.error('serviceId is required to fetch K8s resources');
    }
  },
  beforeDestroy() {
    // 清理定时器
    if (this.chartsInterval) {
      clearInterval(this.chartsInterval);
      this.chartsInterval = null;
    }
    
    // 清理图表资源
    if (this.cpuChart && !this.cpuChart.isDisposed()) {
      this.cpuChart.dispose();
      this.cpuChart = null;
    }
    if (this.memoryChart && !this.memoryChart.isDisposed()) {
      this.memoryChart.dispose();
      this.memoryChart = null;
    }
    
    // 移除窗口大小变化监听
    window.removeEventListener('resize', this.resizeCharts);
  },
  watch: {
    serviceId(newVal) {
      if (newVal) {
        this.fetchK8sResources();
      }
    },
    activeResource(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.updateResourceData();
      }
    },
    selectedNamespace(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.updateResourceData();
      }
    }
  }
});
</script>

<style lang="less" scoped>
.k8s-config-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  
  // 页面头部样式
.page-header {
  display: flex;
  align-items: center;
    padding: 16px 24px;
    background-color: #fff;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
    margin-bottom: 16px;

.header-icon-wrapper {
  margin-right: 16px;

.kubernetes-logo {
        width: 40px;
        height: 40px;
        background-image: url('~@/assets/images/kubernetes-logo.svg');
  background-size: contain;
  background-repeat: no-repeat;
      }
}

.header-content {
  flex: 1;

.title {
        margin: 0;
        padding: 0;
        font-size: 18px;
  font-weight: 500;
        color: #333;
        line-height: 1.4;
}

.subtitle {
        margin: 4px 0 0;
        padding: 0;
        font-size: 13px;
        color: #666;
        line-height: 1.4;
      }
}

.namespace-selector {
      margin-left: 16px;
    }
  }
  
  // 整体仪表盘布局
.k8s-dashboard-layout {
  display: flex;
    flex: 1;
    min-height: calc(100vh - 185px);
    padding: 0 24px 16px;
    
    // 左侧导航样式
.sidebar-menu {
      width: 280px;
      min-width: 280px;
      margin-right: 16px;
      background-color: #fff;
      border-radius: 4px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);

.menu-group {
        padding: 12px 0;
        border-bottom: 1px solid #f0f0f0;
        
        &:last-child {
          border-bottom: none;
}

.group-title {
  padding: 8px 16px;
          color: #999;
          font-size: 12px;
          font-weight: 500;
  text-transform: uppercase;
          letter-spacing: 0.5px;
}

.menu-item {
  display: flex;
          justify-content: space-between;
  align-items: center;
          padding: 10px 16px;
          color: #333;
  font-size: 14px;
  cursor: pointer;
          transition: all 0.2s;
          
          &:hover {
            background-color: #f5f7fa;
}

          &.active {
            background-color: #e6f7ff;
            color: #1890ff;
            border-right: 3px solid #1890ff;
            
            .item-count {
              background-color: #1890ff;
              color: #fff;
            }
}

.item-text {
  flex: 1;
}

.item-count {
            display: inline-block;
            min-width: 24px;
            height: 24px;
            line-height: 24px;
            text-align: center;
            padding: 0 6px;
            border-radius: 12px;
            background-color: #f0f0f0;
            color: #666;
  font-size: 12px;
          }
        }
      }
}

    // 右侧内容区域样式
.content-area {
  flex: 1;
      
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
      }
    }
  }
  
  // 仪表板卡片样式
  .k8s-dashboard-card {
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    margin-bottom: 16px;
    overflow: hidden;
    
    &.k8s-resource-card {
      .k8s-card-header {
  display: flex;
        justify-content: space-between;
  align-items: center;
        height: 48px;
        padding: 0 16px;
        background-color: #f7f7f7;
        border-bottom: 1px solid #eee;
        
        .k8s-card-title {
          font-size: 16px;
          font-weight: 500;
          color: #333;
}

        .k8s-card-actions {
  display: flex;
          gap: 12px;
          
          .k8s-action-icon {
            font-size: 16px;
            color: #999;
            cursor: pointer;
            
            &:hover {
              color: #1890ff;
            }
          }
        }
      }
      
      .k8s-card-content {
        padding: 0;
      }
    }
  }
}

// 图表相关样式
.k8s-dashboard-charts {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;

  .k8s-chart-card {
  flex: 1;
    min-width: 400px;
    height: 250px;
  border-radius: 4px;
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
    position: relative;

    .k8s-chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
      height: 48px;
      padding: 0 16px;
      background-color: #f7f7f7;
      border-bottom: 1px solid #e8e8e8;
      
      .k8s-chart-title {
  font-size: 14px;
  font-weight: 500;
        color: #333;
}

      .k8s-chart-actions {
        .k8s-action-icon {
          cursor: pointer;
          color: #999;
          transition: color 0.3s;
          
          &:hover {
            color: #1890ff;
          }
        }
      }
    }
    
    .k8s-chart-content {
      position: relative;
      height: calc(100% - 48px);
      padding: 10px 5px 10px 15px;
      
      .k8s-chart-y-label {
        position: absolute;
        left: -25px;
        top: 50%;
        transform: rotate(-90deg);
        transform-origin: center;
        font-size: 12px;
        color: #666;
        white-space: nowrap;
        z-index: 2;
        width: 80px;
        text-align: center;
}

.chart {
  width: 100%;
  height: 100%;
}
    }
  }
}

/* 响应式布局 */
@media screen and (max-width: 1200px) {
  .k8s-dashboard-container {
    .k8s-dashboard-charts {
      .k8s-chart-card {
        min-width: 300px;
      }
    }
  }
}

@media screen and (max-width: 768px) {
  .k8s-dashboard-container {
    .k8s-dashboard-charts {
      flex-direction: column;
      
      .k8s-chart-card {
        width: 100%;
      }
    }
  }
}

/* 表格通用样式 */
.k8s-table {
  .status-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: 8px;
    
    &.status-running {
      background-color: #52c41a;
    }
    
    &.status-warning {
      background-color: #faad14;
    }
    
    &.status-danger {
      background-color: #f5222d;
    }
    
    &.status-unknown {
      background-color: #d9d9d9;
    }
  }
  
  .name-cell, .image-cell, .pods-display {
    white-space: nowrap;
  overflow: hidden;
    text-overflow: ellipsis;
}

  .tag-list {
  display: flex;
    flex-wrap: wrap;
    
    .label-tag {
      margin: 2px;
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
}

// 额外的表格样式修复
:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 500;
  color: #333;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background-color: #e6f7ff;
}

:deep(.ant-tag) {
  margin-right: 4px;
  margin-bottom: 4px;
  border-radius: 2px;
}

:deep(.ant-spin-container) {
  height: 100%;
}

:deep(.ant-empty-image) {
  margin-top: 32px;
}

:deep(.ant-table-placeholder) {
  height: 100%;
}

.k8s-dashboard {
  &-container {
    min-height: 600px;
    background-color: #fff;
    border-radius: 4px;
    padding: 16px;

    .tabs-container {
      margin-top: 16px;
}

    .service-table {
      margin-top: 16px;
      
      // 状态点样式
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
        margin-right: 8px;

        &.status-running {
  background-color: #52c41a;
}

        &.status-warning {
  background-color: #faad14;
}

        &.status-unknown {
          background-color: #d9d9d9;
        }
      }
      
      // 标签样式
      .labels-container {
  display: flex;
        flex-wrap: wrap;
        gap: 4px;
        
        .label-chip {
          margin-right: 4px;
          margin-bottom: 4px;
          max-width: 100%;
          height: auto;
          line-height: 1.5;
          white-space: normal;
          word-break: break-word;
}
      }
      
      // 表格头部样式
      .ant-table-thead > tr > th {
        background-color: #f5f7fa;
  font-weight: 500;
        color: #262626;
}

      // 表格单元格样式
      .ant-table-tbody > tr > td {
        padding: 10px 16px;
        word-break: break-word;
      }
      
      // 创建时间列样式
      .ant-table-row td:last-child {
  white-space: nowrap;
      }
    }
  }
}

.service-detail-dialog {
  width: 80%;
  max-width: 900px;
}

// 端点样式
.internal-endpoint, .external-endpoint {
  padding: 2px 0;
  word-break: break-all;
}

.external-endpoint {
  display: flex;
  align-items: center;
  
  .external-icon {
    margin-left: 4px;
    font-size: 12px;
  }
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

// 表格样式优化
.ant-table {
  table-layout: fixed;
  
  .ant-table-tbody > tr > td {
    white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
    word-break: keep-all;
  }
  
  // 确保创建时间列正确显示
  .ant-table-row td:last-child {
  white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 为单元格内容添加工具提示
.cell-content {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 100%;
}

// 修复表格在Safari和Firefox中的显示问题
.k8s-config-container {
  .ant-table-wrapper {
    overflow-x: auto;
    
    .ant-table {
      min-width: 1000px; // 确保表格内容有足够的显示空间
      
      // 修复表头标题竖向显示问题
      .ant-table-thead > tr > th {
        white-space: nowrap !important;
        text-align: left !important;
        min-width: 100px; // 确保每列有足够宽度显示标题
      }
      
      // 特别处理创建时间列的表头
      .ant-table-thead > tr > th:last-child {
        min-width: 100px;
        white-space: nowrap !important;
        text-align: left !important;
        writing-mode: horizontal-tb !important; // 强制水平文本
        transform: none !important; // 防止任何旋转
      }
    }
    
    .ant-table-thead > tr > th,
    .ant-table-tbody > tr > td {
      padding: 10px 8px;
      vertical-align: middle;
    }
    
    .ant-table-column-title {
      word-break: keep-all;
      white-space: nowrap;
      text-align: left !important;
      writing-mode: horizontal-tb !important; // 强制水平文本
      transform: none !important; // 防止任何旋转
      display: inline-block !important; // 确保标题按照预期显示
    }
  }
}

// 内部端点样式
.internal-endpoint {
  padding: 2px 0;
  word-break: keep-all;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

// 外部端点样式
.external-endpoint {
  display: flex;
  align-items: center;
  padding: 2px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  
  .external-icon {
    margin-left: 4px;
    font-size: 12px;
    flex-shrink: 0;
  }
}

.normal-column-header {
  .ant-table-column-title {
    writing-mode: horizontal-tb !important;
    transform: none !important;
    white-space: nowrap !important;
    min-width: 90px !important;
    display: inline-block !important;
  }
}

// 修改全局表头样式
.k8s-config-container {
  .ant-table-column-has-sorters {
    .ant-table-column-title {
      writing-mode: horizontal-tb !important;
      transform: none !important;
      white-space: nowrap !important;
    }
  }
}

/* 确保所有表头正常水平显示，不会竖直旋转 */
:deep(.normal-column-header) {
  .ant-table-column-title {
    display: inline-block !important;
    white-space: nowrap !important;
    overflow: visible !important;
    writing-mode: horizontal-tb !important;
    min-width: 90px !important;
    transform: none !important;
  }
}

/* 修复表格内容样式 */
:deep(.ant-table-tbody) {
  td {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

/* 全局修复表头标题垂直显示问题 */
:deep(.ant-table-thead > tr > th) {
  white-space: nowrap !important;
  text-align: left !important;
  
  .ant-table-column-title {
    display: inline-block !important;
    white-space: nowrap !important;
    writing-mode: horizontal-tb !important;
    transform: none !important;
    word-break: keep-all !important;
    min-width: auto !important;
    width: auto !important;
    max-width: 100% !important;
  }
}

:deep(.ant-table-column-has-sorters) {
  .ant-table-column-sorter {
    display: inline-flex !important;
    align-items: center !important;
    vertical-align: middle !important;
    margin-left: 4px !important;
  }
}

/* 特别针对"创建时间"列的表头 */
:deep(.ant-table-thead > tr > th.normal-column-header) {
  .ant-table-column-title {
    display: inline-block !important;
    white-space: nowrap !important;
    writing-mode: horizontal-tb !important;
    transform: none !important;
    word-break: keep-all !important;
  }
}

/* 修复表格内容溢出问题 */
:deep(.ant-table-body) {
  overflow-x: auto !important;
}

:deep(.ant-table-tbody > tr > td) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: keep-all;
}
</style>