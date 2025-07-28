import React from 'react';

const HostManagePage = () => {
  return (
    <div className="container mx-auto px-6 py-6">
      <h1 className="text-2xl font-semibold mb-6">主机管理</h1>
      
      {/* 筛选器和操作按钮 */}
      <div className="bg-white rounded-xl shadow-md p-4 mb-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-wrap gap-4">
            <div className="flex items-center">
              <span className="text-sm text-gray-500 mr-2">状态:</span>
              <select className="border border-gray-200 rounded-lg text-sm py-1.5 px-3 bg-white">
                <option value="">全部</option>
                <option value="running">正常</option>
                <option value="warning">警告</option>
                <option value="error">异常</option>
              </select>
            </div>
            <div className="flex items-center">
              <span className="text-sm text-gray-500 mr-2">集群:</span>
              <select className="border border-gray-200 rounded-lg text-sm py-1.5 px-3 bg-white">
                <option value="">全部</option>
                <option value="cluster1">测试集群</option>
                <option value="cluster2">生产集群</option>
              </select>
            </div>
            <div className="flex items-center">
              <span className="text-sm text-gray-500 mr-2">标签:</span>
              <select className="border border-gray-200 rounded-lg text-sm py-1.5 px-3 bg-white">
                <option value="">全部</option>
                <option value="master">Master</option>
                <option value="worker">Worker</option>
                <option value="edge">Edge</option>
              </select>
            </div>
          </div>
          <div className="flex items-center">
            <div className="relative">
              <input 
                type="text" 
                placeholder="搜索主机名/IP..." 
                className="border border-gray-200 rounded-lg text-sm py-1.5 pl-8 pr-3 w-64 bg-white"
              />
              <div className="absolute left-2 top-1/2 transform -translate-y-1/2">
                <div className="i-carbon-search text-gray-400"></div>
              </div>
            </div>
            <button className="ml-4 bg-primary-500 text-white rounded-lg px-4 py-1.5 text-sm flex items-center">
              <div className="i-carbon-add mr-1"></div>
              <span>添加主机</span>
            </button>
          </div>
        </div>
      </div>
      
      {/* 主机列表 */}
      <div className="bg-white rounded-xl shadow-md overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  <div className="flex items-center">
                    <input type="checkbox" className="mr-2 rounded border-gray-300 text-primary-500" />
                    主机名
                  </div>
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  IP地址
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  集群
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  角色
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  CPU使用率
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  内存使用率
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  磁盘使用率
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  状态
                </th>
                <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  操作
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              <tr>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <input type="checkbox" className="mr-2 rounded border-gray-300 text-primary-500" />
                    <span className="text-sm text-gray-900">node-01</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  192.168.1.101
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  测试集群
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
                    Master
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-24 bg-gray-200 rounded-full h-2 mr-2">
                      <div className="bg-green-500 h-2 rounded-full" style={{width: '15%'}}></div>
                    </div>
                    <span className="text-sm text-gray-500">15%</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-24 bg-gray-200 rounded-full h-2 mr-2">
                      <div className="bg-green-500 h-2 rounded-full" style={{width: '30%'}}></div>
                    </div>
                    <span className="text-sm text-gray-500">30%</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-24 bg-gray-200 rounded-full h-2 mr-2">
                      <div className="bg-green-500 h-2 rounded-full" style={{width: '25%'}}></div>
                    </div>
                    <span className="text-sm text-gray-500">25%</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                    正常
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-right">
                  <div className="flex justify-end space-x-3">
                    <button className="text-primary-500 hover:text-primary-600">
                      <div className="i-carbon-view"></div>
                    </button>
                    <button className="text-amber-500 hover:text-amber-600">
                      <div className="i-carbon-edit"></div>
                    </button>
                    <button className="text-red-500 hover:text-red-600">
                      <div className="i-carbon-trash-can"></div>
                    </button>
                  </div>
                </td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <input type="checkbox" className="mr-2 rounded border-gray-300 text-primary-500" />
                    <span className="text-sm text-gray-900">node-02</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  192.168.1.102
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  测试集群
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-indigo-100 text-indigo-800">
                    Worker
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-24 bg-gray-200 rounded-full h-2 mr-2">
                      <div className="bg-amber-500 h-2 rounded-full" style={{width: '65%'}}></div>
                    </div>
                    <span className="text-sm text-gray-500">65%</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-24 bg-gray-200 rounded-full h-2 mr-2">
                      <div className="bg-amber-500 h-2 rounded-full" style={{width: '70%'}}></div>
                    </div>
                    <span className="text-sm text-gray-500">70%</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-24 bg-gray-200 rounded-full h-2 mr-2">
                      <div className="bg-red-500 h-2 rounded-full" style={{width: '92%'}}></div>
                    </div>
                    <span className="text-sm text-gray-500">92%</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-amber-100 text-amber-800">
                    警告
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-right">
                  <div className="flex justify-end space-x-3">
                    <button className="text-primary-500 hover:text-primary-600">
                      <div className="i-carbon-view"></div>
                    </button>
                    <button className="text-amber-500 hover:text-amber-600">
                      <div className="i-carbon-edit"></div>
                    </button>
                    <button className="text-red-500 hover:text-red-600">
                      <div className="i-carbon-trash-can"></div>
                    </button>
                  </div>
                </td>
              </tr>
              <tr>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <input type="checkbox" className="mr-2 rounded border-gray-300 text-primary-500" />
                    <span className="text-sm text-gray-900">node-03</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  192.168.1.103
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  测试集群
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800">
                    Edge
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-24 bg-gray-200 rounded-full h-2 mr-2">
                      <div className="bg-green-500 h-2 rounded-full" style={{width: '25%'}}></div>
                    </div>
                    <span className="text-sm text-gray-500">25%</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-24 bg-gray-200 rounded-full h-2 mr-2">
                      <div className="bg-green-500 h-2 rounded-full" style={{width: '40%'}}></div>
                    </div>
                    <span className="text-sm text-gray-500">40%</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-24 bg-gray-200 rounded-full h-2 mr-2">
                      <div className="bg-green-500 h-2 rounded-full" style={{width: '35%'}}></div>
                    </div>
                    <span className="text-sm text-gray-500">35%</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                    正常
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-right">
                  <div className="flex justify-end space-x-3">
                    <button className="text-primary-500 hover:text-primary-600">
                      <div className="i-carbon-view"></div>
                    </button>
                    <button className="text-amber-500 hover:text-amber-600">
                      <div className="i-carbon-edit"></div>
                    </button>
                    <button className="text-red-500 hover:text-red-600">
                      <div className="i-carbon-trash-can"></div>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        
        {/* 分页 */}
        <div className="px-6 py-4 flex items-center justify-between">
          <div className="text-sm text-gray-500">
            显示 <span className="font-medium">1-3</span> 条，共 <span className="font-medium">24</span> 条
          </div>
          <div className="flex items-center space-x-2">
            <button className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm flex items-center disabled:opacity-50 disabled:cursor-not-allowed">
              <div className="i-carbon-chevron-left mr-1"></div>
              上一页
            </button>
            <div className="flex items-center space-x-1">
              <button className="w-8 h-8 flex items-center justify-center rounded-lg bg-primary-50 text-primary-600 font-medium text-sm">1</button>
              <button className="w-8 h-8 flex items-center justify-center rounded-lg text-gray-500 hover:bg-gray-100 text-sm">2</button>
              <button className="w-8 h-8 flex items-center justify-center rounded-lg text-gray-500 hover:bg-gray-100 text-sm">3</button>
              <span className="w-8 h-8 flex items-center justify-center text-gray-500 text-sm">...</span>
              <button className="w-8 h-8 flex items-center justify-center rounded-lg text-gray-500 hover:bg-gray-100 text-sm">8</button>
            </div>
            <button className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm flex items-center">
              下一页
              <div className="i-carbon-chevron-right ml-1"></div>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HostManagePage; 