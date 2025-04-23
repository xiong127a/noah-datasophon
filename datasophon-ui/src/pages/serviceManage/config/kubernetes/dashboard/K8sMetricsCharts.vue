<template>
  <div class="k8s-dashboard-charts">
    <!-- CPU Usage Chart -->
    <div class="k8s-chart-card">
      <div class="k8s-chart-header">
        <div class="k8s-chart-title">CPU Usage</div>
        <div class="k8s-chart-actions">
          <a-icon type="fullscreen" class="k8s-action-icon" />
        </div>
      </div>
      <div class="k8s-chart-content">
        <div class="k8s-chart-y-label">CPU (cores)</div>
        <div ref="cpuChart" class="chart"></div>
      </div>
    </div>
    
    <!-- Memory Usage Chart -->
    <div class="k8s-chart-card">
      <div class="k8s-chart-header">
        <div class="k8s-chart-title">Memory Usage</div>
        <div class="k8s-chart-actions">
          <a-icon type="fullscreen" class="k8s-action-icon" />
        </div>
      </div>
      <div class="k8s-chart-content">
        <div class="k8s-chart-y-label">Memory (bytes)</div>
        <div ref="memoryChart" class="chart"></div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'K8sMetricsCharts',
  props: {
    metricsData: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      cpuChart: null,
      memoryChart: null,
      chartsInterval: null
    };
  },
  mounted() {
    this.initCharts();
    
    // 设置定时器更新图表
    this.chartsInterval = setInterval(() => {
      this.$emit('update-charts');
    }, 30000);  // 每30秒更新一次
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
  methods: {
    initCharts() {
      // 由于页面可能未加载完成，延迟初始化
      this.$nextTick(() => {
        const echarts = require('echarts');
        
        // 获取CPU和内存指标数据
        let cpuMetric = this.metricsData && this.metricsData.length > 0 
          ? this.metricsData.find(metric => metric.metricName === 'cpu/usage_rate') 
          : null;
        
        let memoryMetric = this.metricsData && this.metricsData.length > 0 
          ? this.metricsData.find(metric => metric.metricName === 'memory/usage') 
          : null;
        
        // 提取时间轴和数据点
        let xAxisData = [];
        let cpuData = [];
        let memoryData = [];
        
        // 处理CPU指标数据
        if (cpuMetric && cpuMetric.dataPoints && cpuMetric.dataPoints.length > 0) {
          // 排序数据点，确保时间顺序正确
          const sortedDataPoints = [...cpuMetric.dataPoints].sort((a, b) => a.x - b.x);
          
          // 提取x轴数据
          sortedDataPoints.forEach(point => {
            const date = new Date(point.x * 1000);
            const timeStr = `${date.getHours()}:${date.getMinutes() < 10 ? '0' : ''}${date.getMinutes()}`;
            xAxisData.push(timeStr);
            cpuData.push(point.y);
          });
        } else {
          // 如果没有数据，使用默认时间轴
          xAxisData = this.generateTimeAxis();
          cpuData = new Array(xAxisData.length).fill(0);
        }
        
        // 处理内存指标数据
        if (memoryMetric && memoryMetric.dataPoints && memoryMetric.dataPoints.length > 0) {
          const sortedDataPoints = [...memoryMetric.dataPoints].sort((a, b) => a.x - b.x);
          memoryData = sortedDataPoints.map(point => point.y);
        } else {
          memoryData = new Array(xAxisData.length).fill(0);
        }
        
        // 初始化CPU使用率图表
        if (this.$refs.cpuChart) {
          this.cpuChart = echarts.init(this.$refs.cpuChart, null, {
            renderer: 'canvas',
            useDirtyRect: true,
            devicePixelRatio: window.devicePixelRatio
          });
          const cpuOption = {
            title: {
              show: false
            },
            grid: {
              left: '8%',
              right: '2%',
              bottom: '10%',
              top: '5%',
              containLabel: false
            },
            tooltip: {
              trigger: 'axis',
              formatter: '{b}<br/>{a}: {c} cores'
            },
            xAxis: {
              type: 'category',
              data: xAxisData,
              axisLine: {
                lineStyle: {
                  color: '#E0E0E0'
                }
              },
              axisTick: {
                alignWithLabel: true,
                lineStyle: {
                  color: '#E0E0E0'
                }
              },
              axisLabel: {
                color: '#666',
                fontSize: 10
              },
              splitLine: {
                show: true,
                lineStyle: {
                  color: ['#f0f0f0'],
                  type: 'dashed'
                }
              }
            },
            yAxis: {
              type: 'value',
              name: '',
              nameLocation: 'end',
              nameGap: 15,
              nameTextStyle: {
                color: '#666',
                fontSize: 10,
                padding: [0, 0, 0, 10]
              },
              min: 0,
              max: 0.01, // 与官方一致，Y轴最大值固定为0.01
              axisLine: {
                show: false
              },
              axisTick: {
                show: false
              },
              axisLabel: {
                color: '#666',
                fontSize: 10,
                formatter: '{value}'
              },
              splitLine: {
                show: true,
                lineStyle: {
                  color: ['#f0f0f0'],
                  type: 'dashed'
                }
              }
            },
            series: [{
              name: 'CPU Usage',
              data: cpuData,
              type: 'line',
              smooth: true,
              symbol: 'none',
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: 'rgba(83, 231, 139, 0.8)' // 更接近官方的绿色
                  }, {
                    offset: 1, color: 'rgba(83, 231, 139, 0.1)'
                  }]
                }
              },
              itemStyle: {
                color: '#53e78b' // 更接近官方的绿色
              },
              lineStyle: {
                width: 2,
                color: '#53e78b' // 更接近官方的绿色
              }
            }]
          };
          this.cpuChart.setOption(cpuOption);
        }
        
        // 初始化内存使用率图表
        if (this.$refs.memoryChart) {
          this.memoryChart = echarts.init(this.$refs.memoryChart, null, {
            renderer: 'canvas',
            useDirtyRect: true,
            devicePixelRatio: window.devicePixelRatio
          });
          
          // 计算内存单位和转换
          const maxMemory = Math.max(...memoryData);
          const memoryInMi = maxMemory / (1024 * 1024);
          const yAxisMax = 20; // 固定为20 Mi，与官方一致
          
          const memoryOption = {
            title: {
              show: false
            },
            grid: {
              left: '8%',
              right: '2%',
              bottom: '10%',
              top: '5%',
              containLabel: false
            },
            tooltip: {
              trigger: 'axis',
              formatter: function(params) {
                const value = params[0].value / (1024 * 1024); // 转换为Mi
                return params[0].axisValue + '<br/>' + params[0].seriesName + ': ' + value.toFixed(2) + ' Mi';
              }
            },
            xAxis: {
              type: 'category',
              data: xAxisData,
              axisLine: {
                lineStyle: {
                  color: '#E0E0E0'
                }
              },
              axisTick: {
                alignWithLabel: true,
                lineStyle: {
                  color: '#E0E0E0'
                }
              },
              axisLabel: {
                color: '#666',
                fontSize: 10
              },
              splitLine: {
                show: true,
                lineStyle: {
                  color: ['#f0f0f0'],
                  type: 'dashed'
                }
              }
            },
            yAxis: {
              type: 'value',
              name: '',
              nameLocation: 'end',
              nameGap: 15,
              nameTextStyle: {
                color: '#666',
                fontSize: 10,
                padding: [0, 0, 0, 10]
              },
              min: 0,
              max: yAxisMax,
              axisLine: {
                show: false
              },
              axisTick: {
                show: false
              },
              axisLabel: {
                color: '#666',
                fontSize: 10,
                formatter: '{value} Mi'
              },
              splitLine: {
                show: true,
                lineStyle: {
                  color: ['#f0f0f0'],
                  type: 'dashed'
                }
              }
            },
            series: [{
              name: 'Memory Usage',
              data: memoryData.map(value => value), // 原始字节值
              type: 'line',
              smooth: true,
              symbol: 'none',
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: 'rgba(66, 133, 244, 0.9)' // 使用更亮的蓝色，增加不透明度
                  }, {
                    offset: 1, color: 'rgba(66, 133, 244, 0.2)'
                  }]
                }
              },
              itemStyle: {
                color: '#4285f4' // Google蓝
              },
              lineStyle: {
                width: 2,
                color: '#4285f4' // Google蓝
              }
            }]
          };
          this.memoryChart.setOption(memoryOption);
        }
        
        // 添加窗口大小变化监听，调整图表大小
        window.addEventListener('resize', this.resizeCharts, { passive: true });
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
    
    // 调整图表大小
    resizeCharts() {
      if (this.cpuChart && !this.cpuChart.isDisposed()) {
        this.cpuChart.resize();
      }
      if (this.memoryChart && !this.memoryChart.isDisposed()) {
        this.memoryChart.resize();
      }
    },
    
    // 更新图表
    updateCharts() {
      this.initCharts();
    }
  },
  watch: {
    metricsData: {
      handler() {
        this.updateCharts();
      },
      deep: true
    }
  }
};
</script>

<style lang="less" scoped>
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
  .k8s-dashboard-charts {
    .k8s-chart-card {
      min-width: 300px;
    }
  }
}

@media screen and (max-width: 768px) {
  .k8s-dashboard-charts {
    flex-direction: column;
    
    .k8s-chart-card {
      width: 100%;
    }
  }
}
</style> 