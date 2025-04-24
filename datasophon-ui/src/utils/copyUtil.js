/**
 * 复制工具函数
 * 提供通用的复制文本到剪贴板功能
 */

// 防止频繁调用的标志
let copyInProgress = false;

/**
 * 复制文本到剪贴板
 * @param {String} text 要复制的文本内容
 * @param {String} [label] 成功提示中显示的标签名
 * @param {Object} [vueInstance] Vue实例，用于显示消息提示
 * @returns {Promise<boolean>} 是否复制成功
 */
export function copyText(text, label, vueInstance) {
  // 文本为空时不执行操作
  if (!text) return Promise.resolve(false);
  
  // 防止频繁点击，如果正在复制中则不执行
  if (copyInProgress) return Promise.resolve(false);
  copyInProgress = true;
  
  // 显示成功提示
  const showSuccess = () => {
    if (vueInstance && vueInstance.$message) {
      vueInstance.$message.success(`已复制${label || '文本'}`);
    } else if (window.$message) {
      window.$message.success(`已复制${label || '文本'}`);
    } else {
      console.log(`已复制${label || '文本'}`);
    }
  };
  
  // 显示错误提示
  const showError = (err) => {
    console.error('复制失败:', err);
    if (vueInstance && vueInstance.$message) {
      vueInstance.$message.error('复制失败，请手动复制');
    } else if (window.$message) {
      window.$message.error('复制失败，请手动复制');
    } else {
      console.error('复制失败，请手动复制');
    }
  };
  
  return new Promise((resolve) => {
    // 尝试使用现代的Clipboard API
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text)
        .then(() => {
          showSuccess();
          resolve(true);
        })
        .catch((err) => {
          console.warn('Clipboard API 失败，尝试备用方法:', err);
          // 备用方法：使用 document.execCommand
          try {
            // 创建隔离容器
            const container = document.createElement('div');
            container.style.cssText = `
              position: absolute;
              left: -9999px;
              top: -9999px;
              width: 1px;
              height: 1px;
              opacity: 0;
              overflow: hidden;
              z-index: -9999;
              pointer-events: none;
            `;
            
            // 创建textarea
            const textarea = document.createElement('textarea');
            textarea.value = text;
            textarea.style.position = 'relative';
            textarea.style.opacity = '0';
            
            // 确定容器挂载位置
            const mountTarget = vueInstance?.$el || document.documentElement;
            mountTarget.appendChild(container);
            container.appendChild(textarea);
            
            textarea.focus();
            textarea.select();
            
            const success = document.execCommand('copy');
            if (success) {
              showSuccess();
              resolve(true);
            } else {
              showError(new Error('execCommand返回失败'));
              resolve(false);
            }
            
            // 清理DOM
            if (mountTarget.contains(container)) {
              mountTarget.removeChild(container);
            }
          } catch (execErr) {
            showError(execErr);
            resolve(false);
          }
        })
        .finally(() => {
          // 延迟重置复制标志，防止快速点击
          setTimeout(() => {
            copyInProgress = false;
          }, 300);
        });
    } else {
      // 浏览器不支持Clipboard API，直接使用备用方法
      try {
        // 创建隔离容器
        const container = document.createElement('div');
        container.style.cssText = `
          position: absolute;
          left: -9999px;
          top: -9999px;
          width: 1px;
          height: 1px;
          opacity: 0;
          overflow: hidden;
          z-index: -9999;
          pointer-events: none;
        `;
        
        // 创建textarea
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'relative';
        textarea.style.opacity = '0';
        
        // 确定容器挂载位置
        const mountTarget = vueInstance?.$el || document.documentElement;
        mountTarget.appendChild(container);
        container.appendChild(textarea);
        
        textarea.focus();
        textarea.select();
        
        const success = document.execCommand('copy');
        if (success) {
          showSuccess();
          resolve(true);
        } else {
          showError(new Error('execCommand返回失败'));
          resolve(false);
        }
        
        // 清理DOM
        if (mountTarget.contains(container)) {
          mountTarget.removeChild(container);
        }
      } catch (err) {
        showError(err);
        resolve(false);
      } finally {
        // 延迟重置复制标志，防止快速点击
        setTimeout(() => {
          copyInProgress = false;
        }, 300);
      }
    }
  });
}

/**
 * 安全地下载文件，避免直接使用document.body
 * @param {String} url 下载链接
 * @param {String} fileName 文件名
 * @param {Object} [vueInstance] Vue实例，用于显示消息和挂载DOM
 * @returns {Promise<boolean>} 是否下载成功
 */
export function downloadFile(url, fileName, vueInstance) {
  return new Promise((resolve) => {
    try {
      // 创建隔离容器
      const container = document.createElement('div');
      container.style.cssText = `
        position: absolute;
        left: -9999px;
        top: -9999px;
        width: 1px;
        height: 1px;
        opacity: 0;
        overflow: hidden;
        z-index: -9999;
        pointer-events: none;
      `;
      
      // 创建下载链接
      const link = document.createElement('a');
      link.href = url;
      if (fileName) {
        link.setAttribute('download', fileName);
      }
      link.style.display = 'none';
      
      // 确定容器挂载位置
      const mountTarget = vueInstance?.$el || document.documentElement;
      mountTarget.appendChild(container);
      container.appendChild(link);
      
      // 触发点击
      link.click();
      
      // 显示成功消息
      if (vueInstance && vueInstance.$message) {
        vueInstance.$message.success(`正在下载 ${fileName || '文件'}`);
      } else if (window.$message) {
        window.$message.success(`正在下载 ${fileName || '文件'}`);
      }
      
      // 异步清理DOM
      setTimeout(() => {
        // 清理DOM
        if (mountTarget.contains(container)) {
          mountTarget.removeChild(container);
        }
        resolve(true);
      }, 100);
    } catch (error) {
      console.error('下载文件失败:', error);
      if (vueInstance && vueInstance.$message) {
        vueInstance.$message.error('下载文件失败');
      } else if (window.$message) {
        window.$message.error('下载文件失败');
      }
      resolve(false);
    }
  });
}

export default {
  copyText,
  downloadFile
}; 