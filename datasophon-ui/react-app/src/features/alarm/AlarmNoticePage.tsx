import React from 'react';

const AlarmNoticePage = () => {
  return (
    <div className="container mx-auto px-6 py-6">
      <h1 className="text-2xl font-semibold mb-6">通知组管理</h1>

      {/* 操作按钮区域 */}
      <div className="bg-white rounded-xl shadow-md p-4 mb-6">
        <div className="flex flex-wrap items-center justify-between">
          <div className="flex items-center space-x-4">
            <button className="bg-primary-500 text-white rounded-lg px-4 py-2 text-sm flex items-center">
              <div className="i-carbon-add mr-1"></div>
              <span>创建通知组</span>
            </button>
            <button className="border border-gray-300 text-gray-700 rounded-lg px-4 py-2 text-sm flex items-center">
              <div className="i-carbon-trash-can mr-1"></div>
              <span>批量删除</span>
            </button>
          </div>
          <div className="flex items-center">
            <div className="relative">
              <input 
                type="text" 
                placeholder="搜索通知组..." 
                className="border border-gray-200 rounded-lg text-sm py-2 pl-8 pr-3 w-64"
              />
              <div className="absolute left-2 top-1/2 transform -translate-y-1/2">
                <div className="i-carbon-search text-gray-400"></div>
              </div>
            </div>
            <button className="ml-2 border border-gray-300 rounded-lg p-2">
              <div className="i-carbon-filter"></div>
            </button>
          </div>
        </div>
      </div>

      {/* 通知组列表 */}
      <div className="bg-white rounded-xl shadow-md overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                <div className="flex items-center">
                  <input type="checkbox" className="mr-2 rounded border-gray-300 text-primary-500" />
                  通知组名称
                </div>
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                通知方式
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                联系人
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                创建时间
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                更新时间
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
                  <span className="text-sm font-medium text-gray-900">运维通知组</span>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 mr-1">
                    邮件
                  </span>
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                    短信
                  </span>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-900">张三, 李四, 王五</div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-500">2023-07-15 10:30</div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-500">2023-10-22 14:25</div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                  启用
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
                  <span className="text-sm font-medium text-gray-900">管理层通知组</span>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 mr-1">
                    邮件
                  </span>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-900">赵总, 钱总</div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-500">2023-06-22 09:15</div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-500">2023-06-22 09:15</div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                  启用
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
                  <span className="text-sm font-medium text-gray-900">开发通知组</span>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 mr-1">
                    邮件
                  </span>
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                    钉钉
                  </span>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-900">张工, 王工, 李工, 赵工</div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-500">2023-05-18 16:40</div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="text-sm text-gray-500">2023-11-03 11:22</div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800">
                  禁用
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

        {/* 分页 */}
        <div className="px-6 py-4 flex items-center justify-between">
          <div className="text-sm text-gray-500">
            显示 <span className="font-medium">1-3</span> 条，共 <span className="font-medium">3</span> 条
          </div>
          <div className="flex items-center space-x-2">
            <button className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm flex items-center disabled:opacity-50 disabled:cursor-not-allowed" disabled>
              <div className="i-carbon-chevron-left mr-1"></div>
              上一页
            </button>
            <div className="flex items-center space-x-1">
              <button className="w-8 h-8 flex items-center justify-center rounded-lg bg-primary-50 text-primary-600 font-medium text-sm">1</button>
            </div>
            <button className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm flex items-center disabled:opacity-50 disabled:cursor-not-allowed" disabled>
              下一页
              <div className="i-carbon-chevron-right ml-1"></div>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AlarmNoticePage; 