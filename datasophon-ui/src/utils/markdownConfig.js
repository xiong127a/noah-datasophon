import MarkdownIt from 'markdown-it';
import MarkdownItAnchor from 'markdown-it-anchor';
import MarkdownItToc from 'markdown-it-toc-done-right';

// 创建markdown-it实例
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function(str, lang) {
    return `<pre class="language-${lang}"><code>${md.utils.escapeHtml(str)}</code></pre>`;
  }
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

// 添加目录插件
md.use(MarkdownItToc, {
  containerClass: 'toc-container',
  listType: 'ul',
  listClass: 'toc-list',
  itemClass: 'toc-item',
  linkClass: 'toc-link'
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

export { md }; 