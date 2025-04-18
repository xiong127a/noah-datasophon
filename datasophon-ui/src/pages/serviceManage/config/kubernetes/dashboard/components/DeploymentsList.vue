<template>
  <div class="deployments-container">
    <!-- 图表区域 -->
    <div class="charts-container">
      <div class="chart-card">
        <div class="chart-header">
          <h3>CPU 使用率</h3>
          <a-icon type="fullscreen" />
        </div>
        <div class="chart-content">
          <div class="chart" ref="cpuChart"></div>
        </div>
      </div>
      
      <div class="chart-card">
        <div class="chart-header">
          <h3>内存使用率</h3>
          <a-icon type="fullscreen" />
        </div>
        <div class="chart-content">
          <div class="chart" ref="memoryChart"></div>
        </div>
      </div>
    </div>
    
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
          :rowKey="record => record.name"
          class="k8s-table"
        >
          <template #statusDot="{ record }">
            <span class="status-dot" :class="{'status-running': record.status === 'Running', 'status-warning': record.status === 'Warning'}"></span>
          </template>
          
          <template #name="{ record }">
            <div class="name-cell">
              <span class="name-text">{{ record.name }}</span>
            </div>
          </template>
          
          <template #image="{ text }">
            <div class="image-cell">
              <a-tooltip :title="text">
                <span class="image-text">{{ text }}</span>
              </a-tooltip>
            </div>
          </template>
          
          <template #labels="{ text }">
            <div class="tag-list" v-if="text && Object.keys(text).length > 0">
              <a-tag v-for="(value, key) in text" :key="key" color="blue">
                {{ key }}: {{ value }}
              </a-tag>
            </div>
            <span v-else>-</span>
          </template>
          
          <template #pods="{ record }">
            <span>{{ record.readyReplicas || 0 }} / {{ record.replicas || 0 }}</span>
          </template>
          
          <template #creationTime="{ text }">
            <span>{{ formatTime(text) }}</span>
          </template>
          
          <template #action="{ record }">
            <div class="action-buttons">
              <a-dropdown :trigger="['click']">
                <a-button type="link" size="small">
                  操作 <a-icon type="down" />
                </a-button>
                <a-menu slot="overlay">
                  <a-menu-item @click="handleViewYaml(record)">查看YAML</a-menu-item>
                  <a-menu-item @click="handleViewDetails(record)">查看详情</a-menu-item>
                  <a-menu-item @click="handleEditDeployment(record)">编辑</a-menu-item>
                  <a-menu-item @click="handleScaleDeployment(record)">伸缩</a-menu-item>
                  <a-menu-item @click="handleDeleteDeployment(record)">删除</a-menu-item>
                </a-menu>
              </a-dropdown>
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
  </div>
</template>

<script>
export default {
  name: 'DeploymentsList',
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
      deployments: [],
      columns: [
        {
          title: '',
          dataIndex: 'status',
          key: 'status',
          width: '20px',
          slots: { customRender: 'statusDot' }
        },
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
          slots: { customRender: 'name' }
        },
        {
          title: '镜像',
          dataIndex: 'image',
          key: 'image',
          width: '25%',
          slots: { customRender: 'image' }
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '20%',
          slots: { customRender: 'labels' }
        },
        {
          title: 'Pods',
          key: 'pods',
          width: '10%',
          slots: { customRender: 'pods' }
        },
        {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'creationTime',
          width: '15%',
          slots: { customRender: 'creationTime' },
          sorter: (a, b) => {
            return new Date(a.createTime) - new Date(b.createTime);
          }
        },
        {
          title: '操作',
          key: 'action',
          width: '10%',
          slots: { customRender: 'action' }
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
      // 图表数据
      cpuData: [],
      memoryData: []
    };
  },
  mounted() {
    this.fetchDeployments();
    this.initCharts();
    // 每30秒刷新一次数据
    this.refreshInterval = setInterval(() => {
      this.fetchDeployments();
      this.updateCharts();
    }, 30000);
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
            xAxis: {
              type: 'category',
              data: this.generateTimeAxis(),
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
              data: this.generateRandomData(0, 0.01),
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
            xAxis: {
              type: 'category',
              data: this.generateTimeAxis(),
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
              data: this.generateRandomData(50, 100),
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
        const times = this.generateTimeAxis();
        const cpuData = this.generateRandomData(0, 0.01);
        const memoryData = this.generateRandomData(50, 100);
        
        this.cpuChart.setOption({
          xAxis: {
            data: times
          },
          series: [{
            data: cpuData
          }]
        });
        
        this.memoryChart.setOption({
          xAxis: {
            data: times
          },
          series: [{
            data: memoryData
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
    async fetchDeployments() {
      this.loading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sDeployments, {
          clusterId: this.clusterId,
          namespace: this.namespace === 'all' ? null : this.namespace
        });
        
        if (res.code === 200) {
          this.deployments = res.data || [];
          // 为每个deployment添加状态属性，用于展示状态点
          this.deployments.forEach(item => {
            // 如果所有副本都就绪，显示为Running，否则为Warning
            item.status = item.readyReplicas === item.replicas ? 'Running' : 'Warning';
          });
        } else {
          this.$message.error('获取Deployments失败: ' + res.msg);
          this.deployments = [];
        }
      } catch (error) {
        console.error('获取Deployments出错:', error);
        this.$message.error('获取Deployments出错: ' + error.message);
        this.deployments = [];
      } finally {
        this.loading = false;
      }
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
      this.selectedDeploymentYaml = `apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${record.name}
  namespace: ${record.namespace}
  labels:
${this.formatLabelsForYaml(record.labels)}
spec:
  replicas: ${record.replicas}
  selector:
    matchLabels:
${this.formatLabelsForYaml(record.selector)}
  template:
    metadata:
      labels:
${this.formatLabelsForYaml(record.selector)}
    spec:
      containers:
      - name: ${record.name}
        image: ${record.image}
        ports:
        - containerPort: 80
        resources:
          limits:
            cpu: 500m
            memory: 512Mi
          requests:
            cpu: 200m
            memory: 256Mi`;
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
      this.$message.info(`查看Deployment ${record.name} 的详情功能正在开发中`);
    },
    
    // 编辑Deployment
    handleEditDeployment(record) {
      this.$message.info(`编辑Deployment ${record.name} 的功能正在开发中`);
    },
    
    // 显示伸缩对话框
    handleScaleDeployment(record) {
      this.selectedDeployment = record;
      this.scaleReplicas = record.replicas;
      this.scaleVisible = true;
    },
    
    // 提交伸缩请求
    handleScaleSubmit() {
      this.confirmLoading = true;
      
      // 模拟API请求
      setTimeout(() => {
        this.selectedDeployment.replicas = this.scaleReplicas;
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