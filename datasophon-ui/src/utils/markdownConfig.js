import MarkdownIt from 'markdown-it';
import MarkdownItAnchor from 'markdown-it-anchor';
import MarkdownItToc from 'markdown-it-toc-done-right';

// 创建markdown-it实例，开启缓存以提高性能
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function(str, lang) {
    return `<pre class="language-${lang}"><code>${md.utils.escapeHtml(str)}</code></pre>`;
  },
  // 启用缓存，提高重复解析性能
  _performanceCache: Object.create(null)
});

// 添加锚点插件 - 为标题添加ID
md.use(MarkdownItAnchor, {
  permalink: false,
  slugify: s => {
    const slug = s.toLowerCase()
      .replace(/\s+/g, '-')       // 空格替换为-
      .replace(/[^\w\u4e00-\u9fa5-]/g, '') // 保留字母、数字、中文和连字符
      .replace(/-+/g, '-');      // 将多个连字符合并为一个
    
    return slug;
  }
});

// 添加目录插件，配置更好的性能选项
md.use(MarkdownItToc, {
  containerClass: 'toc-container',
  listType: 'ul',
  listClass: 'toc-list',
  itemClass: 'toc-item',
  linkClass: 'toc-link',
  // 优化大型文档的目录生成
  maxDepth: 4, // 限制目录深度
  format: (content) => content // 简化格式化，提高性能
});

// 修改链接处理，添加target=_blank使外部链接在新窗口打开
const defaultRender = md.renderer.rules.link_open || function(tokens, idx, options, env, self) {
  return self.renderToken(tokens, idx, options);
};

md.renderer.rules.link_open = function(tokens, idx, options, env, self) {
  const token = tokens[idx];
  const href = token.attrGet('href');
  
  if (href && /^https?:\/\//.test(href)) {
    token.attrSet('target', '_blank');
    token.attrSet('rel', 'noopener noreferrer');
  }
  
  return defaultRender(tokens, idx, options, env, self);
};

// 优化图片渲染，添加懒加载属性
const defaultImageRender = md.renderer.rules.image || function(tokens, idx, options, env, self) {
  return self.renderToken(tokens, idx, options);
};

md.renderer.rules.image = function(tokens, idx, options, env, self) {
  const token = tokens[idx];
  
  // 添加懒加载属性
  token.attrSet('loading', 'lazy');
  token.attrSet('class', 'lazy-image');
  
  return defaultImageRender(tokens, idx, options, env, self);
};

// 添加性能优化的渲染方法
md.optimizedRender = function(src) {
  // 加强空值检查
  if (src === undefined || src === null) {
    console.error('[Markdown] 无效的内容传递给optimizedRender: undefined或null');
    return '<div class="markdown-error">无法渲染空内容</div>';
  }
  
  if (typeof src !== 'string') {
    console.error('[Markdown] 无效的内容类型传递给optimizedRender:', typeof src);
    return '<div class="markdown-error">内容类型错误</div>';
  }
  
  // 检查字符串是否为空或只包含空白
  if (src.trim() === '') {
    console.warn('[Markdown] 传递给optimizedRender的内容为空字符串');
    return '<div class="markdown-error">文档内容为空</div>';
  }
  
  try {
    // 标准化换行符
    src = src.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
    
    // 修复Markdown标题格式问题 - 确保#号和文本之间有空格
    src = src.replace(/^(#{1,6})([^#\s])/gm, '$1 $2');
    
    // 修复可能导致渲染问题的特殊字符
    src = src.replace(/\u00A0/g, ' '); // 替换非断空格为普通空格
    
    // 修复表格渲染问题，确保表格行前后有空行
    src = src.replace(/([^\n])\n(\|[^\n]+\|)/g, '$1\n\n$2');
    src = src.replace(/(\|[^\n]+\|)\n([^\n|])/g, '$1\n\n$2');
    
    // 安全处理文档内容 - 去除可能导致问题的控制字符
    let safeContent = '';
    // 遍历字符串字符，跳过控制字符
    for (let i = 0; i < src.length; i++) {
      const charCode = src.charCodeAt(i);
      // 跳过ASCII控制字符和其他可能导致问题的字符
      if (!((charCode >= 0 && charCode <= 31 && charCode !== 10) || (charCode >= 127 && charCode <= 159) || charCode === 0xFEFF)) {
        safeContent += src[i];
      }
    }
    
    // 使用缓存提高性能
    const cache = md._performanceCache || Object.create(null);
    
    // 安全地创建缓存键 - 只使用内容的一小部分作为键
    const cacheKey = (safeContent.substring(0, 30) + '_' + safeContent.length).replace(/[^\w]/g, '_');
    
    if (cache[cacheKey]) {
      console.log('[Markdown] Cache hit for content');
      return cache[cacheKey];
    }
    
    // 进行最终检查，确保内容解析正常
    if (safeContent.includes('###') && !safeContent.includes('## ')) {
      console.warn('[Markdown] 检测到潜在格式问题，尝试修复');
      safeContent = safeContent.replace(/###([^#\n])/g, '### $1');
      safeContent = safeContent.replace(/##([^#\n])/g, '## $1');
      safeContent = safeContent.replace(/#([^#\n])/g, '# $1');
    }
    
    console.log('[Markdown] 渲染内容前100个字符:', safeContent.substring(0, 100));
    
    // 使用完整的markdown-it渲染
    const result = `<div class="markdown-content">${md.render(safeContent)}</div>`;
    
    // 保存到缓存
    if (Object.keys(cache).length > 100) {
      // 简单的缓存管理 - 超过100个条目时清空缓存
      for (const key in cache) {
        delete cache[key];
      }
    }
    cache[cacheKey] = result;
    
    return result;
  } catch (error) {
    console.error('[Markdown] Error in optimizedRender:', error);
    console.error('[Markdown] 错误内容前100个字符:', 
      typeof src === 'string' ? src.substring(0, 100) : '非字符串内容');
    
    // 尝试使用安全的基本渲染方法
    try {
      return `<div class="markdown-content">${md.render(src || '')}</div>`;
    } catch (fallbackError) {
      console.error('[Markdown] Fallback render failed:', fallbackError);
      
      // 极简降级渲染 - 至少保留基本格式
      try {
        let htmlContent = '<div class="markdown-content">';
        
        if (typeof src === 'string') {
          // 简单分割行并转义
          const lines = src.split('\n');
          for (let i = 0; i < lines.length; i++) {
            const line = lines[i] || '';
            const trimmed = line.trim();
            
            if (trimmed.startsWith('#')) {
              // 处理标题
              let level = 1;
              while (level <= 6 && trimmed[level-1] === '#') level++;
              level = Math.min(level-1, 6);
              
              const text = trimmed.substring(level).trim();
              htmlContent += `<h${level}>${text}</h${level}>`;
            } else if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
              // 处理无序列表
              htmlContent += '<ul><li>' + trimmed.substring(2) + '</li></ul>';
            } else if (trimmed.startsWith('1. ') || /^\d+\./.test(trimmed)) {
              // 处理有序列表
              htmlContent += '<ol><li>' + trimmed.replace(/^\d+\.\s*/, '') + '</li></ol>';
            } else if (trimmed.startsWith('>')) {
              // 处理引用
              htmlContent += '<blockquote><p>' + trimmed.substring(1).trim() + '</p></blockquote>';
            } else if (trimmed.startsWith('```')) {
              // 处理代码块
              htmlContent += '<pre><code>' + (i+1 < lines.length ? lines[++i] : '') + '</code></pre>';
              // 跳过代码块内容，直到结束标记
              while (i+1 < lines.length && !lines[i+1].trim().startsWith('```')) i++;
              if (i+1 < lines.length) i++; // 跳过结束标记
            } else if (trimmed !== '') {
              // 普通段落
              htmlContent += '<p>' + trimmed + '</p>';
            } else {
              // 空行
              htmlContent += '<br>';
            }
          }
        } else {
          htmlContent += '<p>无法显示内容</p>';
        }
        
        htmlContent += '</div>';
        return htmlContent;
      } catch (e) {
        // 最终兜底方案
        return '<div class="markdown-error"><h3>文档渲染失败</h3><p>请尝试刷新页面或联系管理员。</p></div>';
      }
    }
  }
};

export { md }; 