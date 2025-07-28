import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/router';
import { get } from '@/api/http';

interface ServiceItem {
  id: string;
  name: string;
  status: 'running' | 'stopped' | 'warning';
  description: string;
  version: string;
  lastUpdate: string;
}

const ServiceOverview = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  
  // 使用React Query获取服务列表
  const { data: services = [], isLoading, error } = useQuery({
    queryKey: ['services'],
    queryFn: async () => {
      // 这里调用实际的API接口
      try {
        return await get<ServiceItem[]>('/services');
      } catch (error) {
        console.error('获取服务列表失败', error);
        // 临时模拟数据用于展示
        return [
          {
            id: '1',
            name: 'HDFS',
            status: 'running',
            description: 'Hadoop分布式文件系统',
            version: '3.3.4',
            lastUpdate: '2023-05-15',
          },
          {
            id: '2',
            name: 'YARN',
            status: 'running',
            description: 'Hadoop资源管理系统',
            version: '3.3.4',
            lastUpdate: '2023-05-15',
          },
          {
            id: '3',
            name: 'Spark',
            status: 'running',
            description: '大数据计算引擎',
            version: '3.3.1',
            lastUpdate: '2023-05-10',
          },
          {
            id: '4',
            name: 'Hive',
            status: 'warning',
            description: '数据仓库',
            version: '3.1.3',
            lastUpdate: '2023-05-12',
          },
          {
            id: '5',
            name: 'HBase',
            status: 'stopped',
            description: '分布式NoSQL数据库',
            version: '2.4.12',
            lastUpdate: '2023-05-08',
          },
        ];
      }
    }
  });

  // 根据搜索词过滤服务
  const filteredServices = services.filter(service => 
    service.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    service.description.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // 点击服务卡片，跳转到服务详情页
  const handleServiceClick = (serviceId: string) => {
    navigate({ to: `/service-manage/service-list/${serviceId}` });
  };

  // 获取每种状态的服务数量
  const runningCount = services.filter(s => s.status === 'running').length;
  const stoppedCount = services.filter(s => s.status === 'stopped').length;
  const warningCount = services.filter(s => s.status === 'warning').length;

  return (
    <div className="container mx-auto px-4 py-6">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800">大数据基础平台</h1>
        <p className="text-gray-600 mt-1">管理和监控您的大数据服务</p>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white rounded-lg shadow p-6 border-l-4 border-green-500">
          <div className="flex justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500">运行中服务</p>
              <p className="text-2xl font-semibold mt-1">{runningCount}</p>
            </div>
            <div className="text-green-500 text-2xl">✓</div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-6 border-l-4 border-yellow-500">
          <div className="flex justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500">警告服务</p>
              <p className="text-2xl font-semibold mt-1">{warningCount}</p>
            </div>
            <div className="text-yellow-500 text-2xl">⚠</div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-6 border-l-4 border-red-500">
          <div className="flex justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500">停止服务</p>
              <p className="text-2xl font-semibold mt-1">{stoppedCount}</p>
            </div>
            <div className="text-red-500 text-2xl">✕</div>
          </div>
        </div>
      </div>

      {/* 搜索和过滤 */}
      <div className="flex justify-between mb-6">
        <input
          type="text"
          placeholder="搜索服务..."
          className="px-4 py-2 border rounded-md w-64"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {/* 服务列表 */}
      {isLoading ? (
        <div className="flex justify-center py-10">
          <div className="loader">加载中...</div>
        </div>
      ) : error ? (
        <div className="text-center py-10 text-red-500">
          加载服务失败，请刷新页面重试
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredServices.map((service) => (
            <div
              key={service.id}
              className="bg-white rounded-lg shadow-md hover:shadow-lg transition cursor-pointer"
              onClick={() => handleServiceClick(service.id)}
            >
              <div className="p-6">
                <div className="flex justify-between items-center mb-4">
                  <h3 className="text-lg font-semibold">{service.name}</h3>
                  <span 
                    className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium
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
                <p className="text-gray-600 text-sm mb-3">{service.description}</p>
                <div className="flex justify-between text-xs text-gray-500">
                  <span>版本: {service.version}</span>
                  <span>最近更新: {service.lastUpdate}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {filteredServices.length === 0 && !isLoading && (
        <div className="text-center py-10 text-gray-500">
          没有找到匹配的服务
        </div>
      )}
    </div>
  );
};

export default ServiceOverview; 