import React from 'react';

const HomePage = () => {
  return (
    <div className="container mx-auto px-6 py-6">
      <h1 className="text-2xl font-semibold mb-6">主页</h1>
      
      <div className="bg-white rounded-xl shadow-md p-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div>
            <h2 className="text-xl font-medium mb-4">Noah大数据平台</h2>
            <p className="text-gray-600 leading-relaxed mb-6">
              欢迎使用Noah大数据基础平台，这是一个高效、可靠的大数据管理系统。
              通过本平台，您可以轻松管理集群资源、监控系统状态、配置服务参数以及处理告警信息。
            </p>
            <div className="space-y-4">
              <div className="flex items-start">
                <div className="w-10 h-10 rounded-lg bg-primary-50 flex-center mr-4 mt-1">
                  <div className="i-carbon-dashboard text-primary-500"></div>
                </div>
                <div>
                  <h3 className="text-lg font-medium">实时监控</h3>
                  <p className="text-gray-500 mt-1">
                    提供全面的集群状态和资源使用监控，让您随时了解系统运行情况。
                  </p>
                </div>
              </div>
              
              <div className="flex items-start">
                <div className="w-10 h-10 rounded-lg bg-primary-50 flex-center mr-4 mt-1">
                  <div className="i-carbon-settings text-primary-500"></div>
                </div>
                <div>
                  <h3 className="text-lg font-medium">简易管理</h3>
                  <p className="text-gray-500 mt-1">
                    直观的界面设计，让集群配置和服务管理变得简单高效。
                  </p>
                </div>
              </div>
              
              <div className="flex items-start">
                <div className="w-10 h-10 rounded-lg bg-primary-50 flex-center mr-4 mt-1">
                  <div className="i-carbon-notification text-primary-500"></div>
                </div>
                <div>
                  <h3 className="text-lg font-medium">智能告警</h3>
                  <p className="text-gray-500 mt-1">
                    及时发现并通知系统异常，预防潜在问题，确保平台稳定运行。
                  </p>
                </div>
              </div>
            </div>
          </div>
          
          <div className="flex-center">
            <div className="w-full max-w-md">
              <img 
                src="/product.png" 
                alt="Noah大数据平台" 
                className="w-full object-contain"
              />
            </div>
          </div>
        </div>
        
        <div className="mt-12">
          <h2 className="text-xl font-medium mb-6">快捷操作</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            <button className="p-6 bg-gray-50 rounded-xl hover:bg-gray-100 transition flex flex-col items-center">
              <div className="w-12 h-12 rounded-full bg-primary-100 flex-center mb-4">
                <div className="i-carbon-cloud text-primary-500 text-xl"></div>
              </div>
              <span className="text-gray-800 font-medium">集群管理</span>
            </button>
            
            <button className="p-6 bg-gray-50 rounded-xl hover:bg-gray-100 transition flex flex-col items-center">
              <div className="w-12 h-12 rounded-full bg-primary-100 flex-center mb-4">
                <div className="i-carbon-bare-metal-server text-primary-500 text-xl"></div>
              </div>
              <span className="text-gray-800 font-medium">主机管理</span>
            </button>
            
            <button className="p-6 bg-gray-50 rounded-xl hover:bg-gray-100 transition flex flex-col items-center">
              <div className="w-12 h-12 rounded-full bg-primary-100 flex-center mb-4">
                <div className="i-carbon-user-admin text-primary-500 text-xl"></div>
              </div>
              <span className="text-gray-800 font-medium">用户管理</span>
            </button>
            
            <button className="p-6 bg-gray-50 rounded-xl hover:bg-gray-100 transition flex flex-col items-center">
              <div className="w-12 h-12 rounded-full bg-primary-100 flex-center mb-4">
                <div className="i-carbon-help text-primary-500 text-xl"></div>
              </div>
              <span className="text-gray-800 font-medium">帮助文档</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage; 