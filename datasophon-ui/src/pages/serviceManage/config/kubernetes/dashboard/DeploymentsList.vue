<template>
  <div class="deployments-container">

    
    <!-- Deployments列表区域 -->
    <div class="deployments-list-container">
      <div class="list-header">
        <h3>Deployments</h3>
        <div class="header-actions">
          <a-tooltip title="过滤">
            <a-icon type="filter" class="action-icon" />
          </a-tooltip>
          <a-tooltip title="展开/收起">
            <a-icon type="arrows-alt" class="action-icon" />
          </a-tooltip>
        </div>
      </div>
      <a-spin :spinning="loading">
        <a-table 
          :columns="columns" 
          :dataSource="deployments" 
          :pagination="false"
          :rowKey="record => record.objectMeta && record.objectMeta.namespace && record.objectMeta.name ? 
            `${record.objectMeta.namespace}-${record.objectMeta.name}` : `${record.namespace || 'unknown'}-${record.name || 'unknown'}`"
          class="k8s-table"
        >
          <!-- 使用template槽位定义自定义单元格 -->
          <template #statusDot="{ record }">
            <span class="status-dot" :class="{
              'status-running': record.status === 'Running', 
              'status-warning': record.status === 'Pending',
              'status-danger': record.status === 'Failed',
              'status-unknown': record.status === 'Unknown'
            }"></span>
          </template>
          
          <template #name="{ text }">
            <div class="name-cell">
              <span class="resource-icon">
                <a-icon type="appstore" theme="filled" />
              </span>
              <span class="name-text" :title="text">{{ text }}</span>
            </div>
          </template>
          
          <template #labels="{ text }">
            <div class="tag-list" v-if="text && Object.keys(text).length > 0">
              <a-tag v-for="(value, key) in text" :key="key" color="blue" class="label-tag">
                {{ key }}: {{ value }}
              </a-tag>
            </div>
            <span v-else>-</span>
          </template>
          
          <template #image="{ record }">
            <div class="image-cell" v-if="record.image" :title="record.image">
              {{ record.image }}
            </div>
            <span v-else>-</span>
          </template>
          
          <template #replicas="{ record }">
            <div>
              {{ record.replicas?.ready || 0 }}/{{ record.replicas?.total || 0 }}
            </div>
          </template>
          
          <template #creationTime="{ record }">
            {{ formatTime(record.creationTimestamp) }}
          </template>
          
          <template #action="{ record }">
            <div class="action-buttons">
              <a-button type="link" size="small" @click="handleViewYaml(record)">查看YAML</a-button>
              <a-button type="link" size="small" @click="handleViewDetails(record)">详情</a-button>
            </div>
          </template>
        </a-table>
      </a-spin>
    </div>
    
    <!-- 查看YAML对话框 -->
    <a-modal
      v-model="yamlVisible"
      title="YAML"
      :width="800"
      :footer="null"
      centered
    >
      <div class="yaml-editor">
        <pre>{{ selectedDeploymentYaml }}</pre>
      </div>
    </a-modal>
    
    <!-- 伸缩对话框 -->
    <a-modal
      v-model="scaleVisible"
      title="伸缩 Deployment"
      @ok="handleScaleSubmit"
      :confirmLoading="confirmLoading"
    >
      <p>{{ selectedDeployment?.name }}</p>
      <a-form-item label="副本数">
        <a-input-number 
          v-model="scaleReplicas" 
          :min="0" 
          :max="20" 
          style="width: 100%"
        />
      </a-form-item>
    </a-modal>
    
    <!-- 部署详情组件 -->
    <deployment-detail
      :visible.sync="detailVisible"
      :deployment-name="detailDeploymentName"
      :namespace="detailNamespace"
      :clusterId="clusterId"
    />
  </div>
</template>

<script>
import DeploymentDetail from './DeploymentDetail.vue';

export default {
  name: 'DeploymentsList',
  components: {
    DeploymentDetail
  },
  props: {
    clusterId: {
      type: Number,
      required: true
    },
    namespace: {
      type: String,
      default: 'all'
    }
  },
  data() {
    return {
      loading: false,
      chartLoading: false,
      deployments: [],
      deploymentStatus: {
        running: 0,
        pending: 0,
        failed: 0,
        succeeded: 0,
        unknown: 0
      },
      // 图表数据
      cpuData: [],
      memoryData: [],
      timeAxis: [],
      columns: [
        {
          title: '状态',
          dataIndex: 'status',
          width: 80,
          fixed: 'left',
          scopedSlots: { customRender: 'statusDot' }
        },
        {
          title: '名称',
          dataIndex: 'name',
          width: 200,
          fixed: 'left',
          scopedSlots: { customRender: 'name' }
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          width: 120,
        },
        {
          title: '镜像',
          dataIndex: 'image',
          width: 280,
          ellipsis: true,
          scopedSlots: { customRender: 'image' }
        },
        {
          title: '标签',
          dataIndex: 'labels',
          width: 200,
          scopedSlots: { customRender: 'labels' }
        },
        {
          title: 'Pod',
          dataIndex: 'replicas',
          width: 80,
          scopedSlots: { customRender: 'replicas' }
        },
        {
          title: '创建时间',
          dataIndex: 'creationTimestamp',
          width: 180,
          sorter: (a, b) => new Date(a.creationTimestamp) - new Date(b.creationTimestamp),
          scopedSlots: { customRender: 'creationTime' }
        },
        {
          title: '操作',
          dataIndex: 'action',
          fixed: 'right',
          width: 200,
          scopedSlots: { customRender: 'action' }
        }
      ],
      // YAML对话框
      yamlVisible: false,
      selectedDeployment: null,
      selectedDeploymentYaml: '',
      // 伸缩对话框
      scaleVisible: false,
      scaleReplicas: 1,
      confirmLoading: false,
      // 部署详情组件
      detailVisible: false,
      detailDeploymentName: '',
      detailNamespace: ''
    };
  },
  mounted() {
    this.fetchDeployments();
    this.fetchMetricsData();
    this.initCharts();
    // 每30秒刷新一次数据
    this.refreshInterval = setInterval(() => {
      this.fetchDeployments();
      this.fetchMetricsData();
    }, 30000);
    
    // 添加表格的事件委托
    this.setupTableEvents();
  },
  updated() {
    // 当组件更新时重新绑定事件
    this.setupTableEvents();
  },
  beforeDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
    // 销毁图表实例，避免内存泄漏
    if (this.cpuChart) {
      this.cpuChart.dispose();
    }
    if (this.memoryChart) {
      this.memoryChart.dispose();
    }
  },
  watch: {
    namespace() {
      this.fetchDeployments();
    }
  },
  methods: {
    // 获取集群资源监控数据
    async fetchMetricsData() {
      this.chartLoading = true;
      try {
        // 使用serviceInstanceId作为serviceId参数
        const serviceId = this.$route.query.serviceInstanceId || 40; // 使用默认值40
        
        const res = await this.$axiosGet(global.API.getK8sDeploymentMetrics, {
          clusterId: this.clusterId,
          serviceId: serviceId,
          namespace: this.namespace === 'all' ? null : this.namespace
        });
        
        if (res.code === 200) {
          // 处理后端返回的监控数据
          const metricsData = res.data || {};
          
          // 设置时间轴数据
          this.timeAxis = metricsData.timeAxis || this.generateTimeAxis();
          
          // 设置CPU和内存数据
          this.cpuData = metricsData.cpuData || this.generateRandomData(0, 0.01);
          this.memoryData = metricsData.memoryData || this.generateRandomData(50, 100);
          
          // 更新图表
          this.updateCharts();
        } else {
          console.error('获取监控数据失败:', res.msg);
          // 如果获取失败，使用模拟数据
          this.timeAxis = this.generateTimeAxis();
          this.cpuData = this.generateRandomData(0, 0.01);
          this.memoryData = this.generateRandomData(50, 100);
          this.updateCharts();
        }
      } catch (error) {
        console.error('获取监控数据出错:', error);
        // 出错时使用模拟数据
        this.timeAxis = this.generateTimeAxis();
        this.cpuData = this.generateRandomData(0, 0.01);
        this.memoryData = this.generateRandomData(50, 100);
        this.updateCharts();
      } finally {
        this.chartLoading = false;
      }
    },

    // 初始化图表
    initCharts() {
      // 由于页面可能未加载完成，延迟初始化
      this.$nextTick(() => {
        const echarts = require('echarts');
        
        // 初始化CPU使用率图表
        if (this.$refs.cpuChart) {
          this.cpuChart = echarts.init(this.$refs.cpuChart);
          const cpuOption = {
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              top: '3%',
              containLabel: true
            },
            tooltip: {
              trigger: 'axis',
              formatter: '{b}<br/>{a}: {c} cores'
            },
            xAxis: {
              type: 'category',
              data: this.timeAxis.length > 0 ? this.timeAxis : this.generateTimeAxis(),
              axisTick: {
                alignWithLabel: true
              }
            },
            yAxis: {
              type: 'value',
              name: 'CPU (cores)',
              min: 0
            },
            series: [{
              name: 'CPU使用',
              data: this.cpuData.length > 0 ? this.cpuData : this.generateRandomData(0, 0.01),
              type: 'line',
              smooth: true,
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: 'rgba(128, 255, 165, 0.8)'
                  }, {
                    offset: 1, color: 'rgba(128, 255, 165, 0.1)'
                  }]
                }
              },
              itemStyle: {
                color: '#10b981'
              },
              lineStyle: {
                width: 2,
                color: '#10b981'
              }
            }]
          };
          this.cpuChart.setOption(cpuOption);
        }
        
        // 初始化内存使用率图表
        if (this.$refs.memoryChart) {
          this.memoryChart = echarts.init(this.$refs.memoryChart);
          const memoryOption = {
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              top: '3%',
              containLabel: true
            },
            tooltip: {
              trigger: 'axis',
              formatter: '{b}<br/>{a}: {c} Mi'
            },
            xAxis: {
              type: 'category',
              data: this.timeAxis.length > 0 ? this.timeAxis : this.generateTimeAxis(),
              axisTick: {
                alignWithLabel: true
              }
            },
            yAxis: {
              type: 'value',
              name: 'Memory (bytes)',
              min: 0,
              max: 200,
              axisLabel: {
                formatter: '{value} Mi'
              }
            },
            series: [{
              name: '内存使用',
              data: this.memoryData.length > 0 ? this.memoryData : this.generateRandomData(50, 100),
              type: 'line',
              smooth: true,
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: 'rgba(100, 149, 237, 0.8)'
                  }, {
                    offset: 1, color: 'rgba(100, 149, 237, 0.1)'
                  }]
                }
              },
              itemStyle: {
                color: '#3b82f6'
              },
              lineStyle: {
                width: 2,
                color: '#3b82f6'
              }
            }]
          };
          this.memoryChart.setOption(memoryOption);
        }
        
        // 添加窗口大小变化监听，调整图表大小
        window.addEventListener('resize', this.resizeCharts);
      });
    },
    
    // 生成时间轴数据
    generateTimeAxis() {
      const now = new Date();
      const times = [];
      for (let i = 12; i >= 0; i--) {
        const time = new Date(now.getTime() - i * 60000);
        times.push(time.getHours() + ':' + (time.getMinutes() < 10 ? '0' : '') + time.getMinutes());
      }
      return times;
    },
    
    // 生成随机数据用于图表展示
    generateRandomData(min, max) {
      const data = [];
      for (let i = 0; i < 13; i++) {
        data.push((Math.random() * (max - min) + min).toFixed(4));
      }
      return data;
    },
    
    // 更新图表
    updateCharts() {
      if (this.cpuChart && this.memoryChart) {
        this.cpuChart.setOption({
          xAxis: {
            data: this.timeAxis
          },
          series: [{
            data: this.cpuData
          }]
        });
        
        this.memoryChart.setOption({
          xAxis: {
            data: this.timeAxis
          },
          series: [{
            data: this.memoryData
          }]
        });
      }
    },
    
    // 调整图表大小
    resizeCharts() {
      if (this.cpuChart) {
        this.cpuChart.resize();
      }
      if (this.memoryChart) {
        this.memoryChart.resize();
      }
    },
    
    // 获取Deployments列表
    fetchDeployments() {
      this.loading = true
      const params = {
        clusterId: this.clusterId
      }
      if (this.namespace && this.namespace !== 'All namespaces') {
        params.namespace = this.namespace
      }
      global.API.getK8sDeployments(params).then(res => {
        console.log('获取Deployments数据:', res)
        if (res.code === 200 && res.data) {
          // 处理状态数量
          this.statusData = [
            { name: '运行', value: res.data.status?.running || 0, color: '#52c41a' },
            { name: '等待', value: res.data.status?.pending || 0, color: '#faad14' },
            { name: '失败', value: res.data.status?.failed || 0, color: '#ff4d4f' }
          ]
          // 处理deployments数据
          if (res.data.deployments && Array.isArray(res.data.deployments)) {
            this.deployments = res.data.deployments.map(deployment => {
              // 从嵌套结构中提取数据
              const objectMeta = deployment.objectMeta || {};
              const pods = deployment.pods || {};
              const containerImages = deployment.containerImages || [];
              const typeMeta = deployment.typeMeta || {};
              
              // 确定pod状态
              let status = 'Unknown';
              if (pods.running > 0) {
                status = 'Running';
              } else if (pods.pending > 0) {
                status = 'Pending';
              } else if (pods.failed > 0) {
                status = 'Failed';
              }
              
              // 创建映射后的对象
              const mappedDeployment = {
                // 使用objectMeta中的数据
                name: objectMeta.name,
                namespace: objectMeta.namespace,
                labels: objectMeta.labels || {},
                creationTimestamp: objectMeta.creationTimestamp,
                
                // 使用pods中的数据
                status: status,
                replicas: {
                  ready: pods.running || 0,
                  total: pods.desired || 0
                },
                
                // 使用containerImages数据
                image: containerImages.join(', '),
                
                // 保留原始数据结构供其他函数使用
                objectMeta: objectMeta,
                pods: pods,
                containerImages: containerImages,
                typeMeta: typeMeta,
                
                // 默认值
                restartCount: 0,
                podName: '',
                nodeName: '',
                podStatus: '',
                podIp: '',
                hostIp: '',
                age: '',
                resourceVersion: ''
              };
              
              console.log('处理后的部署数据:', mappedDeployment);
              return mappedDeployment;
            })
          } else {
            this.deployments = []
          }
          console.log('最终部署列表数据:', this.deployments);
          
          // 处理累计指标数据用于图表
          if (res.data.cumulativeMetrics && res.data.cumulativeMetrics.length > 0) {
            this.podCumulativeMetrics = res.data.cumulativeMetrics
            this.initCharts()
          } else {
            this.podCumulativeMetrics = []
          }
        } else {
          this.$message.error(res.msg || '获取Deployments数据失败')
          this.deployments = []
          this.statusData = [
            { name: '运行', value: 0, color: '#52c41a' },
            { name: '等待', value: 0, color: '#faad14' },
            { name: '失败', value: 0, color: '#ff4d4f' }
          ]
        }
      }).catch(error => {
        console.error('获取Deployments数据出错:', error)
        this.$message.error('获取Deployments数据出错: ' + (error.message || '未知错误'))
        this.deployments = []
        this.statusData = [
          { name: '运行', value: 0, color: '#52c41a' },
          { name: '等待', value: 0, color: '#faad14' },
          { name: '失败', value: 0, color: '#ff4d4f' }
        ]
      }).finally(() => {
        this.loading = false
      })
    },
    
    // 处理指标数据，提取CPU和内存使用情况
    processMetricsData(data) {
      if (data && data.cumulativeMetrics && data.cumulativeMetrics.length >= 2) {
        // 获取CPU数据并处理
        const cpuMetric = data.cumulativeMetrics.find(m => m.metricName === 'cpu/usage_rate');
        if (cpuMetric && cpuMetric.dataPoints && cpuMetric.dataPoints.length > 0) {
          // 提取时间和数据点，格式化为小时:分钟
          this.timeAxis = cpuMetric.dataPoints.map(point => {
            const date = new Date(point.x * 1000);
            return `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
          });
          
          this.cpuData = cpuMetric.dataPoints.map(point => point.y);
        }
        
        // 获取内存数据并处理
        const memoryMetric = data.cumulativeMetrics.find(m => m.metricName === 'memory/usage');
        if (memoryMetric && memoryMetric.dataPoints && memoryMetric.dataPoints.length > 0) {
          this.memoryData = memoryMetric.dataPoints.map(point => {
            // 转换为MB以便阅读
            return (point.y / (1024 * 1024)).toFixed(2);
          });
        }
        
        // 更新图表显示
        this.updateCharts();
        } else {
        this.resetChartData();
      }
    },
    
    // 重置图表数据为模拟数据，用于API失败时的降级显示
    resetChartData() {
      this.timeAxis = this.generateTimeAxis();
      this.cpuData = this.generateRandomData(0, 0.01);
      this.memoryData = this.generateRandomData(50, 100);
      this.updateCharts();
    },
    
    // 格式化时间显示
    formatTime(time) {
      if (!time) return '-';
      
      const date = new Date(time);
      const now = new Date();
      const diff = now - date;
      
      // 如果小于1天，显示"x小时前"或"x分钟前"
      if (diff < 24 * 60 * 60 * 1000) {
        const hours = Math.floor(diff / (60 * 60 * 1000));
        if (hours > 0) {
          return hours + ' 小时前';
        }
        const minutes = Math.floor(diff / (60 * 1000));
        return minutes + ' 分钟前';
      }
      
      // 如果小于1周，显示"x天前"
      if (diff < 7 * 24 * 60 * 60 * 1000) {
        const days = Math.floor(diff / (24 * 60 * 60 * 1000));
        return days + ' 天前';
      }
      
      // 如果小于1年，显示"x月前"
      if (diff < 365 * 24 * 60 * 60 * 1000) {
        const months = Math.floor(diff / (30 * 24 * 60 * 60 * 1000));
        return months + ' 月前';
      }
      
      // 否则显示"x年前"
      const years = Math.floor(diff / (365 * 24 * 60 * 60 * 1000));
      return years + ' 年前';
    },
    
    // 查看YAML
    async handleViewYaml(record) {
      this.selectedDeployment = record;
      this.yamlVisible = true;
      
      try {
        // 构建请求URL
        const apiUrl = `/api/v1/deployment/${record.objectMeta.namespace}/${record.objectMeta.name}`;
        const res = await this.$axiosGet(apiUrl);
        
        if (res && res.yaml) {
          this.selectedDeploymentYaml = res.yaml;
        } else {
          // 如果API没有返回YAML或请求失败，生成一个基本的YAML
      this.selectedDeploymentYaml = `apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${record.objectMeta.name}
  namespace: ${record.objectMeta.namespace}
  labels:
${this.formatLabelsForYaml(record.objectMeta.labels)}
spec:
  replicas: ${record.pods.desired}
  selector:
    matchLabels:
${this.formatLabelsForYaml(record.objectMeta.labels)}
  template:
    metadata:
      labels:
${this.formatLabelsForYaml(record.objectMeta.labels)}
    spec:
      containers:
      - name: ${record.objectMeta.name}
        image: ${record.containerImages ? record.containerImages[0] : 'unknown'}`;
        }
      } catch (error) {
        console.error('获取YAML失败:', error);
        // 生成基本YAML作为降级方案
        this.selectedDeploymentYaml = `apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${record.objectMeta.name}
  namespace: ${record.objectMeta.namespace}`;
      }
    },
    
    // 格式化标签为YAML格式
    formatLabelsForYaml(labels) {
      if (!labels || Object.keys(labels).length === 0) {
        return '    {}';
      }
      
      let result = '';
      for (const key in labels) {
        result += `    ${key}: ${labels[key]}\n`;
      }
      return result;
    },
    
    // 查看详情
    handleViewDetails(record) {
      this.detailDeploymentName = record.objectMeta.name;
      this.detailNamespace = record.objectMeta.namespace;
      this.detailVisible = true;
    },
    
    // 编辑Deployment
    handleEditDeployment(record) {
      this.$message.info(`编辑Deployment ${record.name} 的功能正在开发中`);
    },
    
    // 显示伸缩对话框
    handleScaleDeployment(record) {
      this.selectedDeployment = record;
      this.scaleReplicas = record.replicas.total;
      this.scaleVisible = true;
    },
    
    // 提交伸缩请求
    handleScaleSubmit() {
      this.confirmLoading = true;
      
      // 模拟API请求
      setTimeout(() => {
        // 更新本地数据
        if (this.selectedDeployment && this.selectedDeployment.pods) {
          this.selectedDeployment.pods.desired = this.scaleReplicas;
          this.selectedDeployment.replicas.total = this.scaleReplicas;
        }
        
        this.confirmLoading = false;
        this.scaleVisible = false;
        this.$message.success(`已将 ${this.selectedDeployment.name} 的副本数调整为 ${this.scaleReplicas}`);
        this.fetchDeployments(); // 刷新列表
      }, 1000);
    },
    
    // 删除Deployment
    handleDeleteDeployment(record) {
      this.$confirm({
        title: '确认删除',
        content: `确定要删除Deployment "${record.name}"吗？`,
        okText: '删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => {
          // 模拟删除操作
          this.$message.success(`Deployment ${record.name} 已删除`);
          // 实际环境中应调用API删除
          this.fetchDeployments(); // 刷新列表
        }
      });
    },
    
    // 设置表格事件委托
    setupTableEvents() {
      this.$nextTick(() => {
        // 找到表格容器
        const tableContainer = document.querySelector('.k8s-table');
        if (!tableContainer) return;
        
        // 移除旧的事件监听器避免重复
        tableContainer.removeEventListener('click', this.handleTableClick);
        
        // 添加新的事件监听器
        tableContainer.addEventListener('click', this.handleTableClick);
      });
    },
    
    // 处理表格点击事件
    handleTableClick(event) {
      const target = event.target;
      
      // 判断是否点击了查看YAML按钮
      if (target.classList.contains('view-yaml-btn')) {
        const name = target.getAttribute('data-record-name');
        const record = this.deployments.find(item => item.objectMeta.name === name);
        if (record) {
          this.handleViewYaml(record);
        }
      }
      
      // 判断是否点击了查看详情按钮
      if (target.classList.contains('view-details-btn')) {
        const name = target.getAttribute('data-record-name');
        const record = this.deployments.find(item => item.objectMeta.name === name);
        if (record) {
          this.handleViewDetails(record);
        }
      }
      
      // 判断是否点击了部署名称
      if (target.classList.contains('name-text')) {
        const row = target.closest('tr');
        if (row && row.getAttribute('data-row-key')) {
          const key = row.getAttribute('data-row-key');
          const parts = key.split('-');
          if (parts.length >= 2) {
            const namespace = parts[0];
            const name = parts.slice(1).join('-');
            const record = this.deployments.find(item => 
              item.objectMeta.name === name && item.objectMeta.namespace === namespace);
            
            if (record) {
              this.handleViewDetails(record);
            }
          }
        }
      }
    },
    
    // 确保对象是可迭代的标签对象
    ensureLabelsObject(labels) {
      if (!labels) {
        return {};
      }
      
      // 如果是字符串，尝试解析为JSON
      if (typeof labels === 'string') {
        try {
          // 如果是空字符串或"{}"，返回空对象
          if (labels === '' || labels === '{}') {
            return {};
          }
          
          const parsed = JSON.parse(labels);
          if (parsed && typeof parsed === 'object') {
            return parsed;
          }
          
          // 如果解析结果不是对象，创建一个包含原始值的对象
          return { value: labels };
        } catch (e) {
          console.warn('解析标签字符串失败:', e);
          // 创建一个包含原始字符串的对象
          return { value: labels };
        }
      }
      
      // 如果已经是对象类型，直接返回
      if (typeof labels === 'object') {
        return labels;
      }
      
      // 其他类型，创建包含原始值的对象
      return { value: String(labels) };
    }
  }
};
</script>

<style scoped>
.deployments-container {
  width: 100%;
}

.charts-container {
  display: flex;
  width: 100%;
  margin-bottom: 24px;
  gap: 16px;
}

.chart-card {
  flex: 1;
  background-color: #ffffff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  overflow: hidden;
  border: 1px solid #eaeaea;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.chart-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.chart-content {
  padding: 16px;
  height: 200px;
}

.chart {
  width: 100%;
  height: 100%;
}

.deployments-list-container {
  background-color: #ffffff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  overflow: hidden;
  border: 1px solid #eaeaea;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.list-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-icon {
  cursor: pointer;
  padding: 6px;
  color: #666;
}

.action-icon:hover {
  color: #1890ff;
}

.k8s-table {
  width: 100%;
}

.k8s-table :deep(.ant-table-thead > tr > th) {
  background-color: #f8f9fa;
  font-weight: 500;
  color: #333;
  font-size: 13px;
}

.k8s-table :deep(.ant-table-tbody > tr > td) {
  padding: 12px 16px;
  font-size: 13px;
  word-break: break-word;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #d9d9d9;
}

.status-running {
  background-color: #52c41a;
}

.status-warning {
  background-color: #faad14;
}

.status-danger {
  background-color: #ff4d4f;
}

.status-unknown {
  background-color: #d9d9d9;
}

.name-cell {
  display: flex;
  align-items: center;
}

.name-text {
  font-weight: 500;
  color: #1890ff;
  cursor: pointer;
}

.name-text:hover {
  text-decoration: underline;
}

.image-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-width: 100%;
  overflow: hidden;
}

.yaml-editor {
  max-height: 500px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 12px;
  background-color: #f8f9fa;
  font-family: 'SF Mono', Monaco, Menlo, Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .charts-container {
    flex-direction: column;
  }
  
  .chart-content {
    height: 150px;
  }
}
</style> 