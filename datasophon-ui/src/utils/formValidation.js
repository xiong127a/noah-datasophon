/**
 * 全局表单验证工具
 * 用于为所有表单提供统一的验证体验，包括输入成功状态显示
 */

// 防抖动函数
const debounce = (fn, delay = 300) => {
  let timer = null;
  return function(...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
};

// 标记已处理的元素，防止重复处理
const processedElements = new WeakSet();

// 标记有效状态，确保立即反馈
const markValidState = (fieldName, isValid) => {
  // 直接找到对应的表单项
  const formItems = document.querySelectorAll(`.ant-form-item[data-field="${fieldName}"]`);
  if (formItems && formItems.length > 0) {
    formItems.forEach(formItem => {
      if (isValid) {
        formItem.classList.remove('ant-form-item-has-error');
        formItem.classList.add('ant-form-item-has-success');
      } else {
        formItem.classList.remove('ant-form-item-has-success');
      }
    });
  }
};

// 更新所有输入控件的状态处理函数
const updateInputStates = () => {
  // 获取所有表单项
  const formItems = document.querySelectorAll('.ant-form-item');
  formItems.forEach(item => {
    // 跳过已处理的元素
    if (processedElements.has(item)) return;
    
    // 找到控件
    const input = item.querySelector('input');
    const select = item.querySelector('.ant-select-selection');
    const textarea = item.querySelector('textarea');
    
    // 获取字段名称
    const field = item.getAttribute('data-field');
    
    if (field) {
      // 针对不同类型控件设置监听
      if (input && !input.hasAttribute('data-monitored')) {
        input.setAttribute('data-monitored', 'true');
        
        input.addEventListener('input', () => {
          if (input.value && input.value.trim() !== '') {
            item.classList.add('ant-form-item-has-success');
          } else {
            item.classList.remove('ant-form-item-has-success');
          }
        });
        
        // 初始状态检查
        if (input.value && input.value.trim() !== '') {
          item.classList.add('ant-form-item-has-success');
        }
      }
      
      // 下拉选择框
      if (select && !select.hasAttribute('data-monitored')) {
        select.setAttribute('data-monitored', 'true');
        
        // 初始状态检查
        if (select.querySelector('.ant-select-selection-selected-value')) {
          item.classList.add('ant-form-item-has-success');
        }
      }
      
      // 文本域
      if (textarea && !textarea.hasAttribute('data-monitored')) {
        textarea.setAttribute('data-monitored', 'true');
        
        textarea.addEventListener('input', () => {
          if (textarea.value && textarea.value.trim() !== '') {
            item.classList.add('ant-form-item-has-success');
          } else {
            item.classList.remove('ant-form-item-has-success');
          }
        });
        
        // 初始状态检查
        if (textarea.value && textarea.value.trim() !== '') {
          item.classList.add('ant-form-item-has-success');
        }
      }
      
      // 标记此元素已处理
      processedElements.add(item);
    }
  });
};

// 处理表单字段的验证状态
const handleFormFieldValidation = () => {
  // 找到页面中所有待处理的表单字段
  const unprocessedFormItems = Array.from(document.querySelectorAll('.ant-form-item')).filter(
    item => !processedElements.has(item)
  );
  
  if (unprocessedFormItems.length === 0) return;
  
  // 处理新的表单项
  unprocessedFormItems.forEach(item => {
    const control = item.querySelector('.ant-form-item-control');
    if (control) {
      const field = control.getAttribute('id');
      if (field) {
        const fieldName = field.replace('_field', '');
        item.setAttribute('data-field', fieldName);
        
        // 标记为已处理
        processedElements.add(item);
      }
    }
  });
  
  // 更新输入状态
  updateInputStates();
};

// 主要的表单验证方法
export const validateField = (rule, value, callback) => {
  if (value !== undefined && value !== null && String(value).trim() !== '') {
    // 立即应用样式，不使用延时
    markValidState(rule.field, true);
    callback();
  } else {
    markValidState(rule.field, false);
    callback('该字段为必填项');
  }
};

// 初始化表单项，添加data-field属性
export const initFormItemAttributes = () => {
  const formItems = document.querySelectorAll('.ant-form-item');
  formItems.forEach(item => {
    // 跳过已处理的元素
    if (processedElements.has(item)) return;
    
    // 获取控件
    const control = item.querySelector('.ant-form-item-control');
    if (control) {
      const field = control.getAttribute('id');
      if (field) {
        const fieldName = field.replace('_field', '');
        item.setAttribute('data-field', fieldName);
        
        // 找到表单字段的输入控件
        const input = item.querySelector('input');
        const select = item.querySelector('.ant-select');
        
        // 对于已有值的控件，直接应用成功样式
        if (input && input.value && input.value.trim() !== '') {
          item.classList.add('ant-form-item-has-success');
        }
        
        if (select && select.querySelector('.ant-select-selection-selected-value')) {
          item.classList.add('ant-form-item-has-success');
        }
        
        // 标记为已处理
        processedElements.add(item);
      }
    }
  });
};

// 防抖的表单处理函数
const debouncedHandleFormValidation = debounce(handleFormFieldValidation, 300);

// 表单验证Mixin
export const formValidationMixin = {
  mounted() {
    // 初始化表单项属性
    initFormItemAttributes();
    
    // 添加监听
    setTimeout(() => {
      updateInputStates();
    }, 200);

    // 使用防抖动的MutationObserver
    let isProcessing = false;
    const observer = new MutationObserver((mutations) => {
      // 防止在处理过程中再次触发
      if (isProcessing) return;
      
      // 检查是否有表单相关的变动
      const hasFormChanges = mutations.some(mutation => {
        return Array.from(mutation.addedNodes).some(node => {
          if (!(node instanceof HTMLElement)) return false;
          return (
            node.classList?.contains('ant-form-item') ||
            node.querySelector?.('.ant-form-item') ||
            node.classList?.contains('ant-modal') ||
            node.querySelector?.('.ant-modal')
          );
        });
      });
      
      if (!hasFormChanges) return;
      
      isProcessing = true;
      setTimeout(() => {
        debouncedHandleFormValidation();
        isProcessing = false;
      }, 100);
    });
    
    // 只观察表单相关区域，减少不必要的触发
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: false,
      characterData: false
    });
    
    // 组件销毁时断开观察器
    this.$once('hook:beforeDestroy', () => {
      observer.disconnect();
    });
  }
}; 