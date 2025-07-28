/**
 * 通知提示工具
 * 使用苹果风格的通知提示
 */

// 成功提示
export const showSuccess = (message: string, title = '成功') => {
  // 创建通知元素
  const notification = document.createElement('div');
  notification.className = 'apple-notification success';
  
  // 设置内容
  notification.innerHTML = `
    <div class="notification-icon">
      <div class="i-carbon-checkmark-filled text-lg"></div>
    </div>
    <div class="notification-content">
      <div class="notification-title">${title}</div>
      <div class="notification-message">${message}</div>
    </div>
    <div class="notification-close">
      <div class="i-carbon-close text-sm"></div>
    </div>
  `;
  
  // 添加到body
  document.body.appendChild(notification);
  
  // 添加关闭事件
  const closeBtn = notification.querySelector('.notification-close');
  closeBtn?.addEventListener('click', () => {
    notification.classList.add('notification-hide');
    setTimeout(() => {
      document.body.removeChild(notification);
    }, 300);
  });
  
  // 显示动画
  setTimeout(() => {
    notification.classList.add('notification-show');
  }, 10);
  
  // 3秒后自动关闭
  setTimeout(() => {
    notification.classList.add('notification-hide');
    setTimeout(() => {
      if (document.body.contains(notification)) {
        document.body.removeChild(notification);
      }
    }, 300);
  }, 3000);
};

// 错误提示
export const showError = (message: string, title = '错误') => {
  // 创建通知元素
  const notification = document.createElement('div');
  notification.className = 'apple-notification error';
  
  // 设置内容
  notification.innerHTML = `
    <div class="notification-icon">
      <div class="i-carbon-error-filled text-lg"></div>
    </div>
    <div class="notification-content">
      <div class="notification-title">${title}</div>
      <div class="notification-message">${message}</div>
    </div>
    <div class="notification-close">
      <div class="i-carbon-close text-sm"></div>
    </div>
  `;
  
  // 添加到body
  document.body.appendChild(notification);
  
  // 添加关闭事件
  const closeBtn = notification.querySelector('.notification-close');
  closeBtn?.addEventListener('click', () => {
    notification.classList.add('notification-hide');
    setTimeout(() => {
      document.body.removeChild(notification);
    }, 300);
  });
  
  // 显示动画
  setTimeout(() => {
    notification.classList.add('notification-show');
  }, 10);
  
  // 3秒后自动关闭
  setTimeout(() => {
    notification.classList.add('notification-hide');
    setTimeout(() => {
      if (document.body.contains(notification)) {
        document.body.removeChild(notification);
      }
    }, 300);
  }, 3000);
};

// 警告提示
export const showWarning = (message: string, title = '警告') => {
  // 创建通知元素
  const notification = document.createElement('div');
  notification.className = 'apple-notification warning';
  
  // 设置内容
  notification.innerHTML = `
    <div class="notification-icon">
      <div class="i-carbon-warning-filled text-lg"></div>
    </div>
    <div class="notification-content">
      <div class="notification-title">${title}</div>
      <div class="notification-message">${message}</div>
    </div>
    <div class="notification-close">
      <div class="i-carbon-close text-sm"></div>
    </div>
  `;
  
  // 添加到body
  document.body.appendChild(notification);
  
  // 添加关闭事件
  const closeBtn = notification.querySelector('.notification-close');
  closeBtn?.addEventListener('click', () => {
    notification.classList.add('notification-hide');
    setTimeout(() => {
      document.body.removeChild(notification);
    }, 300);
  });
  
  // 显示动画
  setTimeout(() => {
    notification.classList.add('notification-show');
  }, 10);
  
  // 3秒后自动关闭
  setTimeout(() => {
    notification.classList.add('notification-hide');
    setTimeout(() => {
      if (document.body.contains(notification));
      document.body.removeChild(notification);
    }, 300);
  }, 3000);
}; 