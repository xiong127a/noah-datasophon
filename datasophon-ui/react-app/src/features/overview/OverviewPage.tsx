import React from 'react';

const OverviewPage = () => {
  return (
    <div className="container mx-auto px-6 py-6">
      <h1 className="text-2xl font-semibold mb-6">集群总览</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* 集群状态卡片 */}
        <div className="bg-white rounded-xl shadow-md p-6">
          <div className="flex items-center">
            <div className="w-12 h-12 rounded-lg bg-primary-50 flex-center mr-4">
              <div className="i-carbon-cloud text-primary-500 text-2xl"></div>
            </div>
            <div>
              <div className="text-sm text-gray-500">集群状态</div>
              <div className="text-xl font-medium">运行中</div>
            </div>
          </div>
          <div className="mt-4 flex justify-between">
            <div className="text-center">
              <div className="text-2xl font-semibold text-primary-500">12</div>
              <div className="text-sm text-gray-500">节点总数</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-semibold text-green-500">10</div>
              <div className="text-sm text-gray-500">运行中</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-semibold text-amber-500">1</div>
              <div className="text-sm text-gray-500">警告</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-semibold text-red-500">1</div>
              <div className="text-sm text-gray-500">异常</div>
            </div>
          </div>
        </div>
        
        {/* 资源使用卡片 */}
        <div className="bg-white rounded-xl shadow-md p-6">
          <div className="flex items-center">
            <div className="w-12 h-12 rounded-lg bg-primary-50 flex-center mr-4">
              <div className="i-carbon-chart-line text-primary-500 text-2xl"></div>
            </div>
            <div>
              <div className="text-sm text-gray-500">资源使用</div>
              <div className="text-xl font-medium">正常</div>
            </div>
          </div>
          <div className="mt-4 grid grid-cols-2 gap-4">
            <div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-500">CPU</span>
                <span className="text-sm font-medium">65%</span>
              </div>
              <div className="h-2 bg-gray-100 rounded-full mt-1">
                <div className="h-2 bg-primary-500 rounded-full" style={{width: '65%'}}></div>
              </div>
            </div>
            <div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-500">内存</span>
                <span className="text-sm font-medium">48%</span>
              </div>
              <div className="h-2 bg-gray-100 rounded-full mt-1">
                <div className="h-2 bg-primary-500 rounded-full" style={{width: '48%'}}></div>
              </div>
            </div>
          </div>
        </div>
        
        {/* 服务状态卡片 */}
        <div className="bg-white rounded-xl shadow-md p-6">
          <div className="flex items-center">
            <div className="w-12 h-12 rounded-lg bg-primary-50 flex-center mr-4">
              <div className="i-carbon-application text-primary-500 text-2xl"></div>
            </div>
            <div>
              <div className="text-sm text-gray-500">服务状态</div>
              <div className="text-xl font-medium">运行中</div>
            </div>
          </div>
          <div className="mt-4 flex justify-between">
            <div className="text-center">
              <div className="text-2xl font-semibold text-primary-500">15</div>
              <div className="text-sm text-gray-500">服务总数</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-semibold text-green-500">14</div>
              <div className="text-sm text-gray-500">运行中</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-semibold text-red-500">1</div>
              <div className="text-sm text-gray-500">已停止</div>
            </div>
          </div>
        </div>
        
        {/* 告警信息卡片 */}
        <div className="bg-white rounded-xl shadow-md p-6">
          <div className="flex items-center">
            <div className="w-12 h-12 rounded-lg bg-primary-50 flex-center mr-4">
              <div className="i-carbon-notification text-primary-500 text-2xl"></div>
            </div>
            <div>
              <div className="text-sm text-gray-500">告警信息</div>
              <div className="text-xl font-medium">3 条未处理</div>
            </div>
          </div>
          <div className="mt-4 space-y-2">
            <div className="flex items-center">
              <div className="w-2 h-2 rounded-full bg-red-500 mr-2"></div>
              <div className="text-sm text-gray-600 truncate">节点 node-03 磁盘空间不足</div>
            </div>
            <div className="flex items-center">
              <div className="w-2 h-2 rounded-full bg-amber-500 mr-2"></div>
              <div className="text-sm text-gray-600 truncate">HDFS 空间使用率超过 85%</div>
            </div>
            <div className="flex items-center">
              <div className="w-2 h-2 rounded-full bg-amber-500 mr-2"></div>
              <div className="text-sm text-gray-600 truncate">Yarn 资源池队列延迟</div>
            </div>
          </div>
        </div>
      </div>
      
      {/* 集群使用趋势 */}
      <div className="mt-8">
        <h2 className="text-lg font-medium mb-4">集群使用趋势</h2>
        <div className="bg-white rounded-xl shadow-md p-6 h-80 flex-center">
          <div className="text-gray-400">集群使用趋势图表将在此处显示</div>
        </div>
      </div>
      
      {/* 最近活动 */}
      <div className="mt-8">
        <h2 className="text-lg font-medium mb-4">最近活动</h2>
        <div className="bg-white rounded-xl shadow-md overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  操作类型
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  详情
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  时间
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  状态
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">服务重启</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">重启 HDFS 服务</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">今天 10:30</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                    成功
                  </span>
                </td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">新增节点</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">添加节点 node-12 到集群</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">今天 09:15</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                    成功
                  </span>
                </td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">配置修改</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">更新 Yarn 资源配置</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">昨天 16:42</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                    成功
                  </span>
                </td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">告警处理</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">处理节点 node-05 内存不足告警</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">昨天 14:23</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-amber-100 text-amber-800">
                    处理中
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default OverviewPage; 