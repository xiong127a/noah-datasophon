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
            const textarea = document.createElement('textarea');
            textarea.value = text;
            textarea.style.position = 'fixed';
            textarea.style.left = '-9999px';
            textarea.style.top = '-9999px';
            textarea.style.opacity = '0';
            document.body.appendChild(textarea);
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
            document.body.removeChild(textarea);
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
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.left = '-9999px';
        textarea.style.top = '-9999px';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
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
        document.body.removeChild(textarea);
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

export default {
  copyText
}; 