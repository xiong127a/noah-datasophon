import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { get } from '@/api/http';
import * as echarts from 'echarts';
import { useEffect, useRef } from 'react';

interface ServiceDetailProps {
  serviceId: string;
}

interface ServiceDetail {
  id: string;
  name: string;
  status: 'running' | 'stopped' | 'warning';
  description: string;
  version: string;
  lastUpdate: string;
  nodes: number;
  uptime: string;
  memoryUsage: number;
  cpuUsage: number;
  storageUsage?: number;
  healthChecks: {
    name: string;
    status: 'success' | 'warning' | 'error';
    message: string;
  }[];
  components: {
    name: string;
    status: 'running' | 'stopped' | 'warning';
    host: string;
    port: number;
  }[];
}

const ServiceDetail = ({ serviceId }: ServiceDetailProps) => {
  const [activeTab, setActiveTab] = useState('overview');
  const cpuChartRef = useRef<HTMLDivElement>(null);
  const memoryChartRef = useRef<HTMLDivElement>(null);
  
  // 使用React Query获取服务详情
  const { data: service, isLoading, error } = useQuery({
    queryKey: ['service', serviceId],
    queryFn: async () => {
      // 这里调用实际的API接口
      try {
        return await get<ServiceDetail>(`/services/${serviceId}`);
      } catch (error) {
        console.error('获取服务详情失败', error);
        // 临时模拟数据用于展示
        const mockData: ServiceDetail = {
          id: serviceId,
          name: 'HDFS',
          status: 'running',
          description: 'Hadoop分布式文件系统',
          version: '3.3.4',
          lastUpdate: '2023-05-15',
          nodes: 5,
          uptime: '15天4小时',
          memoryUsage: 68,
          cpuUsage: 45,
          storageUsage: 72,
          healthChecks: [
            { name: '节点状态', status: 'success', message: '所有节点正常运行' },
            { name: '数据复制', status: 'success', message: '数据块正常复制' },
            { name: '存储空间', status: 'warning', message: '存储空间使用超过70%' },
          ],
          components: [
            { name: 'NameNode', status: 'running', host: '192.168.1.101', port: 9000 },
            { name: 'DataNode-1', status: 'running', host: '192.168.1.102', port: 9001 },
            { name: 'DataNode-2', status: 'running', host: '192.168.1.103', port: 9001 },
            { name: 'DataNode-3', status: 'warning', host: '192.168.1.104', port: 9001 },
            { name: 'DataNode-4', status: 'running', host: '192.168.1.105', port: 9001 },
          ],
        };
        return mockData;
      }
    }
  });

  // 初始化图表
  useEffect(() => {
    if (!service || !cpuChartRef.current || !memoryChartRef.current) return;

    // CPU使用率图表
    const cpuChart = echarts.init(cpuChartRef.current);
    cpuChart.setOption({
      series: [
        {
          type: 'gauge',
          progress: {
            show: true,
            width: 18,
          },
          axisLine: {
            lineStyle: {
              width: 18,
            },
          },
          axisTick: {
            show: false,
          },
          splitLine: {
            length: 15,
            lineStyle: {
              width: 2,
              color: '#999',
            },
          },
          axisLabel: {
            distance: 25,
            color: '#999',
            fontSize: 12,
          },
          anchor: {
            show: true,
            showAbove: true,
            size: 25,
            itemStyle: {
              borderWidth: 10,
            },
          },
          title: {
            show: true,
            fontSize: 14,
          },
          detail: {
            valueAnimation: true,
            formatter: '{value}%',
            fontSize: 30,
          },
          data: [
            {
              value: service.cpuUsage,
              name: 'CPU使用率',
            },
          ],
        },
      ],
    });

    // 内存使用率图表
    const memoryChart = echarts.init(memoryChartRef.current);
    memoryChart.setOption({
      series: [
        {
          type: 'gauge',
          progress: {
            show: true,
            width: 18,
          },
          axisLine: {
            lineStyle: {
              width: 18,
            },
          },
          axisTick: {
            show: false,
          },
          splitLine: {
            length: 15,
            lineStyle: {
              width: 2,
              color: '#999',
            },
          },
          axisLabel: {
            distance: 25,
            color: '#999',
            fontSize: 12,
          },
          anchor: {
            show: true,
            showAbove: true,
            size: 25,
            itemStyle: {
              borderWidth: 10,
            },
          },
          title: {
            show: true,
            fontSize: 14,
          },
          detail: {
            valueAnimation: true,
            formatter: '{value}%',
            fontSize: 30,
          },
          data: [
            {
              value: service.memoryUsage,
              name: '内存使用率',
            },
          ],
        },
      ],
    });

    // 窗口大小变化时，重新调整图表大小
    const handleResize = () => {
      cpuChart.resize();
      memoryChart.resize();
    };
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      cpuChart.dispose();
      memoryChart.dispose();
    };
  }, [service]);

  // 加载中状态
  if (isLoading) {
    return (
      <div className="flex justify-center py-10">
        <div className="loader">加载中...</div>
      </div>
    );
  }

  // 错误状态
  if (error || !service) {
    return (
      <div className="text-center py-10 text-red-500">
        加载服务详情失败，请刷新页面重试
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-6">
      {/* 面包屑导航 */}
      <div className="mb-6 text-sm">
        <Link to="/service-manage" className="text-blue-500 hover:underline">服务管理</Link>
        <span className="mx-2">/</span>
        <span className="text-gray-600">{service.name} 详情</span>
      </div>

      {/* 服务标题和状态 */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">{service.name}</h1>
          <p className="text-gray-600 mt-1">{service.description}</p>
        </div>
        <div>
          <span 
            className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium
            ${
              service.status === 'running' ? 'bg-green-100 text-green-800' :
              service.status === 'warning' ? 'bg-yellow-100 text-yellow-800' :
              'bg-red-100 text-red-800'
            }`}
          >
            {
              service.status === 'running' ? '运行中' :
              service.status === 'warning' ? '警告' : '已停止'
            }
          </span>
        </div>
      </div>

      {/* 基本信息卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <div className="bg-white p-4 rounded-lg shadow">
          <p className="text-sm text-gray-500">版本</p>
          <p className="text-xl font-semibold">{service.version}</p>
        </div>
        <div className="bg-white p-4 rounded-lg shadow">
          <p className="text-sm text-gray-500">节点数</p>
          <p className="text-xl font-semibold">{service.nodes}</p>
        </div>
        <div className="bg-white p-4 rounded-lg shadow">
          <p className="text-sm text-gray-500">运行时间</p>
          <p className="text-xl font-semibold">{service.uptime}</p>
        </div>
        <div className="bg-white p-4 rounded-lg shadow">
          <p className="text-sm text-gray-500">最近更新</p>
          <p className="text-xl font-semibold">{service.lastUpdate}</p>
        </div>
      </div>

      {/* 标签导航 */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex -mb-px">
          <button
            className={`py-3 px-6 font-medium text-sm ${
              activeTab === 'overview'
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setActiveTab('overview')}
          >
            概览
          </button>
          <button
            className={`py-3 px-6 font-medium text-sm ${
              activeTab === 'components'
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setActiveTab('components')}
          >
            组件
          </button>
          <button
            className={`py-3 px-6 font-medium text-sm ${
              activeTab === 'config'
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setActiveTab('config')}
          >
            配置
          </button>
          <button
            className={`py-3 px-6 font-medium text-sm ${
              activeTab === 'logs'
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setActiveTab('logs')}
          >
            日志
          </button>
        </nav>
      </div>

      {/* 概览内容 */}
      {activeTab === 'overview' && (
        <div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
            {/* CPU使用率图表 */}
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-lg font-medium mb-4">CPU使用率</h3>
              <div ref={cpuChartRef} style={{ height: '300px' }}></div>
            </div>

            {/* 内存使用率图表 */}
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-lg font-medium mb-4">内存使用率</h3>
              <div ref={memoryChartRef} style={{ height: '300px' }}></div>
            </div>
          </div>

          {/* 健康状态列表 */}
          <div className="bg-white rounded-lg shadow overflow-hidden mb-8">
            <div className="px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-medium">健康状态检查</h3>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      检查项
                    </th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      状态
                    </th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      详情
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {service.healthChecks.map((check, index) => (
                    <tr key={index}>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {check.name}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium
                          ${
                            check.status === 'success' ? 'bg-green-100 text-green-800' :
                            check.status === 'warning' ? 'bg-yellow-100 text-yellow-800' :
                            'bg-red-100 text-red-800'
                          }`}
                        >
                          {
                            check.status === 'success' ? '正常' :
                            check.status === 'warning' ? '警告' : '错误'
                          }
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {check.message}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* 组件内容 */}
      {activeTab === 'components' && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200">
            <h3 className="text-lg font-medium">服务组件</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    组件名
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    状态
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    主机
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    端口
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {service.components.map((component, index) => (
                  <tr key={index}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {component.name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium
                        ${
                          component.status === 'running' ? 'bg-green-100 text-green-800' :
                          component.status === 'warning' ? 'bg-yellow-100 text-yellow-800' :
                          'bg-red-100 text-red-800'
                        }`}
                      >
                        {
                          component.status === 'running' ? '运行中' :
                          component.status === 'warning' ? '警告' : '已停止'
                        }
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {component.host}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {component.port}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <button className="text-blue-600 hover:text-blue-900 mr-3">
                        重启
                      </button>
                      <button className="text-blue-600 hover:text-blue-900">
                        查看日志
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 配置内容 */}
      {activeTab === 'config' && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-medium mb-4">服务配置</h3>
          <p className="text-gray-500">配置页面内容...</p>
        </div>
      )}

      {/* 日志内容 */}
      {activeTab === 'logs' && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-medium mb-4">服务日志</h3>
          <p className="text-gray-500">日志页面内容...</p>
        </div>
      )}
    </div>
  );
};

export default ServiceDetail; 