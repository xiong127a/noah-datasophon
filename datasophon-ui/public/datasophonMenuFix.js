/**
 * 强制更新Datasophon总览菜单图标和顺序
 * 
 * 这个文件的目的是在页面加载时直接修改本地存储的菜单数据，
 * 更新Datasophon总览菜单的图标，并调整菜单顺序。
 */

(function() {
  // 添加到窗口加载完成后执行
  window.addEventListener('load', function() {
    try {
      console.log('强制更新Datasophon总览菜单...');
      
      setTimeout(function() {
        try {
          // 从本地存储获取菜单数据
          const menuDataStr = localStorage.getItem('menuData');
          if (!menuDataStr) {
            console.warn('未找到菜单数据');
            return;
          }
          
          const menuData = JSON.parse(menuDataStr);
          let updated = false;
          
          // 找到Datasophon总览和集群总览菜单项
          let datasophonOverviewItem = null;
          let clusterOverviewItem = null;
          let datasophonIndex = -1;
          let clusterIndex = -1;
          
          menuData.forEach((item, index) => {
            if (item.path === 'datasophon-overview') {
              datasophonOverviewItem = item;
              datasophonIndex = index;
              
              // 确保meta对象存在
              if (!item.meta) {
                item.meta = {};
              }
              
              // 更新图标
              item.meta.icon = 'datasophon-overview';
              item.name = 'Datasophon总览';
              updated = true;
              console.log('已更新Datasophon总览菜单图标');
            }
            
            if (item.path === 'overview') {
              clusterOverviewItem = item;
              clusterIndex = index;
            }
          });
          
          // 调整菜单顺序，确保Datasophon总览在第一位
          if (datasophonIndex > 0 && clusterIndex >= 0 && datasophonIndex > clusterIndex) {
            // 移除Datasophon总览
            menuData.splice(datasophonIndex, 1);
            // 在集群总览前插入
            menuData.splice(clusterIndex, 0, datasophonOverviewItem);
            updated = true;
            console.log('已调整菜单顺序，Datasophon总览现在在第一位');
          }
          
          if (updated) {
            // 更新本地存储
            localStorage.setItem('menuData', JSON.stringify(menuData));
            console.log('成功更新菜单数据');
            
            // 如果当前在相关页面，刷新页面
            if (window.location.pathname.includes('/datasophon-overview') || 
                window.location.pathname.includes('/overview')) {
              console.log('正在总览页面，刷新页面应用新菜单...');
              setTimeout(() => {
                window.location.reload();
              }, 500);
            }
          } else {
            console.warn('未找到需要更新的菜单项');
          }
        } catch (error) {
          console.error('强制更新菜单失败:', error);
        }
      }, 500); // 延迟0.5秒执行，确保菜单数据已加载
    } catch (error) {
      console.error('启动菜单修复脚本失败:', error);
    }
  });
})(); 