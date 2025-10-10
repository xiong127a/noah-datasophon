/**
 * 强制添加告警管理帮助菜单
 * 
 * 这个文件的目的是在页面加载时直接修改本地存储的菜单数据，
 * 添加告警管理帮助菜单项，解决菜单不显示的问题。
 */

(function() {
  // 添加到窗口加载完成后执行
  window.addEventListener('load', function() {
    try {
      console.log('强制添加告警管理帮助菜单...');
      
      // 从本地存储获取菜单数据
      const menuData = JSON.parse(localStorage.getItem('menuData') || '[]');
      
      // 找到告警管理模块
      const alarmManageModule = menuData.find(item => item.path === 'alarm-manage');
      
      if (!alarmManageModule) {
        console.warn('未找到告警管理模块');
        return;
      }
      
      // 检查是否已存在帮助菜单项
      const hasHelpMenu = alarmManageModule.children.some(item => item.path === 'help');
      
      if (hasHelpMenu) {
        console.log('告警管理帮助菜单已存在');
        return;
      }
      
      // 在数组开头添加帮助菜单项
      alarmManageModule.children.unshift({
        path: 'help',
        name: '使用帮助',
        label: '使用帮助',
        fullPath: '/alarm-manage/help',
        meta: {
          notAlive: false,
          invisible: false
        }
      });
      
      // 更新本地存储
      localStorage.setItem('menuData', JSON.stringify(menuData));
      console.log('成功添加告警管理帮助菜单');
      
      // 刷新页面以应用新菜单
      if (window.location.pathname.includes('/alarm-manage')) {
        console.log('正在告警管理页面，刷新页面应用新菜单...');
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      }
    } catch (error) {
      console.error('强制添加告警管理帮助菜单失败:', error);
    }
  });
})(); 