import { useQuery } from '@tanstack/react-query';
import { get } from '@/api/http';
import * as echarts from 'echarts';
import { useEffect, useRef } from 'react';

interface ClusterStats {
  hostCount: number;
  serviceCount: number;
  alarmCount: number;
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  cpuTrend: number[];
  memoryTrend: number[];
  diskTrend: number[];
  timePoints: string[];
}

const ClusterOverview = () => {
  const resourceChartRef = useRef<HTMLDivElement>(null);
  const trendChartRef = useRef<HTMLDivElement>(null);
  
  // 使用React Query获取集群概览数据
  const { data, isLoading, error } = useQuery({
    queryKey: ['clusterOverview'],
    queryFn: async () => {
      // 这里调用实际的API接口
      try {
        return await get<ClusterStats>('/cluster/overview');
      } catch (error) {
        console.error('获取集群概览数据失败', error);
        // 临时模拟数据用于展示
        const mockData: ClusterStats = {
          hostCount: 12,
          serviceCount: 8,
          alarmCount: 3,
          cpuUsage: 45,
          memoryUsage: 68,
          diskUsage: 72,
          cpuTrend: [40, 42, 44, 45, 43, 42, 45],
          memoryTrend: [65, 63, 68, 70, 69, 67, 68],
          diskTrend: [70, 70, 71, 71, 72, 72, 72],
          timePoints: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        };
        return mockData;
      }
    }
  });
  
  // 初始化图表
  useEffect(() => {
    if (!data || !resourceChartRef.current || !trendChartRef.current) return;
    
    // 资源使用图表
    const resourceChart = echarts.init(resourceChartRef.current);
    resourceChart.setOption({
      tooltip: {
        trigger: 'item'
      },
      legend: {
        top: '2%',
        left: 'center'
      },
      series: [
        {
          name: '资源使用',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 20,
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: [
            { value: data.cpuUsage, name: 'CPU使用' },
            { value: data.memoryUsage, name: '内存使用' },
            { value: data.diskUsage, name: '磁盘使用' }
          ]
        }
      ]
    });
    
    // 趋势图表
    const trendChart = echarts.init(trendChartRef.current);
    trendChart.setOption({
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['CPU使用率', '内存使用率', '磁盘使用率']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.timePoints
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: '{value}%'
        }
      },
      series: [
        {
          name: 'CPU使用率',
          type: 'line',
          data: data.cpuTrend,
          smooth: true
        },
        {
          name: '内存使用率',
          type: 'line',
          data: data.memoryTrend,
          smooth: true
        },
        {
          name: '磁盘使用率',
          type: 'line',
          data: data.diskTrend,
          smooth: true
        }
      ]
    });
    
    // 窗口大小变化时，重新调整图表大小
    const handleResize = () => {
      resourceChart.resize();
      trendChart.resize();
    };
    window.addEventListener('resize', handleResize);
    
    return () => {
      window.removeEventListener('resize', handleResize);
      resourceChart.dispose();
      trendChart.dispose();
    };
  }, [data]);
  
  // 加载中状态
  if (isLoading) {
    return (
      <div className="flex justify-center py-10">
        <div className="loader">加载中...</div>
      </div>
    );
  }

  // 错误状态
  if (error || !data) {
    return (
      <div className="text-center py-10 text-red-500">
        加载集群概览数据失败，请刷新页面重试
      </div>
    );
  }
  
  return (
    <div className="container mx-auto px-4 py-6">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800">集群总览</h1>
        <p className="text-gray-600 mt-1">查看集群整体运行状态</p>
      </div>
      
      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <div className="p-3 rounded-full bg-blue-100 text-blue-500 mr-4">
              <span className="text-2xl">🖥️</span>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">主机数量</p>
              <p className="text-2xl font-semibold">{data.hostCount}</p>
            </div>
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <div className="p-3 rounded-full bg-green-100 text-green-500 mr-4">
              <span className="text-2xl">⚙️</span>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">服务数量</p>
              <p className="text-2xl font-semibold">{data.serviceCount}</p>
            </div>
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <div className="p-3 rounded-full bg-yellow-100 text-yellow-500 mr-4">
              <span className="text-2xl">🔔</span>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">告警数量</p>
              <p className="text-2xl font-semibold">{data.alarmCount}</p>
            </div>
          </div>
        </div>
      </div>
      
      {/* 图表区域 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        {/* 资源使用图表 */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-medium mb-4">资源使用情况</h3>
          <div ref={resourceChartRef} style={{ height: '400px' }}></div>
        </div>
        
        {/* 趋势图表 */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-medium mb-4">资源使用趋势</h3>
          <div ref={trendChartRef} style={{ height: '400px' }}></div>
        </div>
      </div>
    </div>
  );
};

export default ClusterOverview; 