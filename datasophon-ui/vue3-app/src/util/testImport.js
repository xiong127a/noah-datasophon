// 测试关键组件的导入路径
// 这个文件只是用于测试，不会在实际应用中使用

try {
  // 尝试导入集群列表组件
  import('../views/cluster/ClusterList.vue')
    .then(() => console.log('成功导入 ClusterList 组件'))
    .catch(err => console.error('导入 ClusterList 失败:', err));

  // 尝试导入存储库管理组件
  import('../views/cluster/ParcelList.vue')
    .then(() => console.log('成功导入 ParcelList (集群目录) 组件'))
    .catch(err => console.error('导入 ParcelList (集群目录) 失败:', err));

  // 尝试导入备用路径的存储库管理组件
  import('../views/repository/ParcelList.vue')
    .then(() => console.log('成功导入 ParcelList (存储库目录) 组件'))
    .catch(err => console.error('导入 ParcelList (存储库目录) 失败:', err));

  // 尝试导入集群框架组件
  import('../views/cluster/FrameworkManage.vue')
    .then(() => console.log('成功导入 FrameworkManage 组件'))
    .catch(err => console.error('导入 FrameworkManage 失败:', err));
} catch (error) {
  console.error('测试导入时发生错误:', error);
}

// 这个文件的目的是验证组件路径是否存在，只要没有报错就说明路径是正确的
console.log('组件路径验证完成'); 