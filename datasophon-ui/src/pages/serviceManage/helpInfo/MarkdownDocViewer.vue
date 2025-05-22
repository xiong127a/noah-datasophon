<template>
  <div class="markdown-page">
    <div class="page-container">
      <div v-if="loading" class="loading-container">
        <a-spin tip="加载中..." />
      </div>
      <template v-else>
        <div v-if="mdContent" class="content-wrapper" ref="contentWrapper">
          <!-- 使用CSS定义的sticky侧边栏 -->
          <div class="sidebar" v-if="tocVisible" ref="tocNav">
            <div class="custom-nav" v-html="tocHtml" ref="tocContainerDiv"></div>
          </div>
          
          <!-- 右侧内容区 -->
          <div class="main-content">
            <!-- Markdown内容 -->
            <div class="markdown-content" v-html="htmlContent" ref="contentDiv"></div>
          </div>
        </div>
        <div class="no-data" v-else>
          <a-empty :description="emptyText" />
        </div>
      </template>
    </div>
    
    <!-- 直接固定显示的返回顶部按钮 - 居中显示 -->
    <div class="fixed-back-top" @click="scrollToTopHandler">
      <a-icon type="arrow-up" />
      <span>返回顶部</span>
    </div>
  </div>
</template>

<script>
import { md } from '@/utils/markdownConfig';
import services from '@/api/httpApi/services';
import paths from '@/api/baseUrl'
import './styles/markdown.less';
import './styles/markdownPage.less';

export default {
  name: 'MarkdownDocViewer',
  props: {
    serviceId: {
      type: String,
      required: true
    },
    docType: {
      type: String,
      required: true,
      validator: value => ['component', 'guide'].includes(value)
    },
    emptyText: {
      type: String,
      default: '暂无文档信息'
    }
  },
  data() {
    return {
      loading: true,
      serviceName: '',
      mdContent: null,
      htmlContent: '',
      tocHtml: '',
      tocVisible: false,
      backendBaseUrl: process.env.VUE_APP_API_BASE_URL || '' // 获取后端API基础URL
    }
  },
  created() {
    // 配置图片和iframe的URL转换处理
    this.configureImageRenderer();
    this.configureIframeRenderer();
  },
  mounted() {
    console.log('[TOC Mounted] Component mounted. Calling getServiceName and fetchDocData.');
    this.getServiceName();
    this.fetchDocData();
    
    // 添加全局返回顶部事件，确保按钮功能正常
    this.setupGlobalScrollToTop();
  },
  beforeDestroy() {
    // 移除全局事件
    document.removeEventListener('global-scroll-top', this.handleGlobalScrollTop);
  },
  methods: {
    // 配置图片URL转换
    configureImageRenderer() {
      // 保存默认的图片渲染器
      const defaultRender = md.renderer.rules.image || function(tokens, idx, options, env, self) {
        return self.renderToken(tokens, idx, options);
      };
      
      // 获取API基础URL
      const apiBaseUrl = paths.path();
      
      // 重写图片渲染器
      md.renderer.rules.image = (tokens, idx, options, env, self) => {
        const token = tokens[idx];
        
        try {
          // 检查是否是图片token
          if (token.type === 'image') {
            // 从token中获取src属性
            const srcIndex = token.attrIndex('src');
            if (srcIndex >= 0) {
              const src = token.attrs[srcIndex][1];
              console.log('Markdown图片标签解析 - 原始路径:', src);
              
              // 对路径进行编码
              const encodedPath = encodeURIComponent(src);
              
              // 使用services中定义的API路径
              const newSrc = `${apiBaseUrl}${services.getDocImage}?imagePath=${encodedPath}`;
              
              // 替换原始路径
              token.attrs[srcIndex][1] = newSrc;
              console.log('Markdown图片标签解析 - 转换后路径:', newSrc);
            }
          }
        } catch (error) {
          console.error('Markdown图片标签解析 - 处理出错:', error);
        }
        
        // 调用默认渲染器处理其他属性
        return defaultRender(tokens, idx, options, env, self);
      };
    },

    // 配置iframe URL转换
    configureIframeRenderer() {
      // 获取API基础URL
      const apiBaseUrl = paths.path();
      
      // 添加自定义的iframe渲染规则
      md.renderer.rules.html_block = function(tokens, idx) {
        const content = tokens[idx].content;
        
        // 检查是否包含iframe标签
        if (content.includes('<iframe')) {
          try {
            // 创建临时DOM元素来解析HTML
            const div = document.createElement('div');
            div.innerHTML = content;
            const iframe = div.querySelector('iframe');
            
            if (iframe && iframe.hasAttribute('src')) {
              const originalSrc = iframe.getAttribute('src');
              console.log('Markdown iframe标签解析 - 原始路径:', originalSrc);
              
              // 对路径进行编码
              const encodedPath = encodeURIComponent(originalSrc);
              
              // 使用services中定义的API路径
              const newSrc = `${apiBaseUrl}${services.getDocImage}?imagePath=${encodedPath}`;
              
              // 替换src属性
              iframe.setAttribute('src', newSrc);
              console.log('Markdown iframe标签解析 - 转换后路径:', newSrc);
              
              // 返回修改后的HTML
              return div.innerHTML;
            }
          } catch (error) {
            console.error('Markdown iframe标签解析 - 处理出错:', error);
          }
        }
        
        // 如果不是iframe或处理失败，返回原始内容
        return content;
      };
    },
    
    // 设置全局返回顶部事件
    setupGlobalScrollToTop() {
      // 创建一个自定义事件
      this.handleGlobalScrollTop = () => {
        console.log('触发全局返回顶部事件');
        this.smoothScrollToTop();
      };
      
      // 添加事件监听
      document.addEventListener('global-scroll-top', this.handleGlobalScrollTop);
    },
    
    // 强制滚动到顶部的方法
    forceScrollToTop() {
      // 使用平滑滚动效果
      this.smoothScrollToTop();
      
      // 设置备用方案，如果平滑滚动失败，2秒后强制滚动
      setTimeout(() => {
        // 检查当前滚动位置
        const currentPos = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop;
        if (currentPos > 50) {
          // 如果仍然不在顶部，使用硬性滚动作为备用
          this.instantScrollToTop();
        }
      }, 2000);
    },
    
    // 平滑滚动到顶部
    smoothScrollToTop() {
      try {
        // 主要平滑滚动方法
        window.scrollTo({
          top: 0,
          behavior: 'smooth'
        });
        
        // 尝试在主要滚动容器上使用平滑滚动
        const scrollContainers = [
          document.documentElement,
          document.body,
          document.querySelector('.markdown-page'),
          document.querySelector('.page-container'),
          document.querySelector('.content-wrapper'),
          document.querySelector('.main-content'),
          this.$el.closest('.ant-layout-content'),
          document.querySelector('.ant-layout-content')
        ];
        
        // 对所有可能的容器应用平滑滚动
        scrollContainers.forEach(container => {
          if (container) {
            // 设置CSS滚动行为
            container.style.scrollBehavior = 'smooth';
            
            // 尝试平滑滚动
            try {
              container.scrollTo({
                top: 0,
                behavior: 'smooth'
              });
            } catch (e) {
              // 有些旧浏览器可能不支持带选项的scrollTo
              container.scrollTop = 0;
            }
          }
        });
        
        // 尝试使用scrollIntoView平滑滚动
        const topElement = document.querySelector('.markdown-page') || document.body.firstElementChild;
        if (topElement) {
          topElement.scrollIntoView({ 
            behavior: 'smooth', 
            block: 'start' 
          });
        }
      } catch (err) {
        console.error('平滑滚动失败:', err);
        this.instantScrollToTop();
      }
    },
    
    // 立即滚动到顶部（备用方法）
    instantScrollToTop() {
      // 强制重置所有可能的滚动容器
      window.scrollTo(0, 0);
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
      
      // 处理所有可能的滚动容器
      const scrollContainers = document.querySelectorAll('*');
      scrollContainers.forEach(el => {
        if (el.scrollTop > 0) {
          el.scrollTop = 0;
        }
      });
    },
    
    // 平滑滚动到顶部的处理函数
    scrollToTopHandler(e) {
      // 防止事件冒泡和默认行为
      e.preventDefault();
      e.stopPropagation();
      
      console.log('返回顶部按钮被点击');
      
      // 触发自定义事件
      document.dispatchEvent(new Event('global-scroll-top'));
      
      // 使用平滑滚动
      this.smoothScrollToTop();
      
      return false;
    },
    
    getServiceName() {
      if (this.$route && this.$route.params && this.$route.params.serviceId) {
        const serviceId = this.$route.params.serviceId || ''
        const menuData = JSON.parse(localStorage.getItem('menuData')) || []
        const arr = menuData.filter(item => item.path === 'service-manage')
        if (arr.length > 0) {
          arr[0].children.map(item => {
            if (item.meta.params.serviceId == serviceId) {
              this.serviceName = item.name
            }
          })
        }
      }
    },
    fetchDocData() {
      // 获取当前路由中的集群ID
      const clusterId = this.$route.params.clusterId || localStorage.getItem('clusterId');
      console.log('[TOC Data] fetchDocData called. ClusterId:', clusterId, 'ServiceId:', this.serviceId, 'DocType:', this.docType);
      
      // 调用API获取文档
      this.$axiosPost(services.getServiceDoc, {
        clusterId: clusterId,
        serviceId: this.serviceId,
        type: this.docType
      }).then(response => {
        console.log('[TOC Data] fetchDocData - .then() callback. Response received:', response);
        if (response.code === 200 && response.data) {
          this.mdContent = response.data;
          console.log('[TOC Data] fetchDocData - Got data. mdContent length:', response.data.length);
          
          // Always attempt to generate TOC by adding the marker
          const contentWithTocMarker = '[[toc]]\n\n' + response.data;
          
          // Render Markdown to full HTML (including the TOC part if generated)
          const fullHtml = md.render(contentWithTocMarker);
          
          // Use a temporary DOM element to parse the full HTML
          const tempDiv = document.createElement('div');
          tempDiv.innerHTML = fullHtml;
          
          // Find the TOC container (adjust selector if needed based on your markdown-it-table-of-contents plugin output)
          const tocContainerNode = tempDiv.querySelector('.toc-container'); // Or whatever class/ID your TOC plugin uses
          
          if (tocContainerNode) {
            this.tocHtml = tocContainerNode.outerHTML;
            this.tocVisible = true;
            
            // Remove the TOC container from the temporary DOM element
            tocContainerNode.parentNode.removeChild(tocContainerNode);
            
            // The remaining HTML is the main content
            this.htmlContent = tempDiv.innerHTML;
            console.log('[TOC Data] TOC extracted to sidebar. Main content updated.');
          } else {
            // No TOC was generated by [[toc]] or it couldn't be found
            this.htmlContent = fullHtml; // Use the full HTML as is
            this.tocHtml = '';
            this.tocVisible = false;
            console.log('[TOC Data] No TOC container found. Main content will include everything.');
          }
          
          // 添加锚点点击事件处理
          setTimeout(() => {
            console.log('[TOC Data] fetchDocData - setTimeout: Attempting to call setupTocLinkHandlers.');
            this.setupTocLinkHandlers();
          }, 0);
        } else {
          console.warn('[TOC Data] fetchDocData - No data or error in response. Code:', response.code, 'Data:', response.data);
          this.mdContent = null;
          this.htmlContent = '';
          this.tocHtml = '';
          this.tocVisible = false;
        }
        this.loading = false;
      }).catch(error => {
        console.error('[TOC Data] fetchDocData - .catch() error:', error);
        this.mdContent = null;
        this.htmlContent = '';
        this.tocHtml = '';
        this.tocVisible = false;
        this.loading = false;
      });
    },
    setupTocLinkHandlers() {
      console.log('[TOC Setup] setupTocLinkHandlers function CALLED.');
      if (!this.$refs.tocContainerDiv) {
        console.warn('[TOC Setup] this.$refs.tocContainerDiv is not available. Cannot find tocLinks.');
        return;
      }
      const tocLinks = this.$refs.tocContainerDiv.querySelectorAll('.toc-link');
      console.log('[TOC Setup] Found tocLinks elements count (from ref):', tocLinks.length, 'Elements:', tocLinks);
      
      tocLinks.forEach((link, index) => {
        console.log('[TOC Setup] Loop', index, '- Setting up listener for link:', link);
        // 移除已存在的事件监听器以防止重复
        const oldLink = link.cloneNode(true);
        link.parentNode.replaceChild(oldLink, link);
        
        oldLink.addEventListener('click', (e) => {
          e.preventDefault();
          
          const href = oldLink.getAttribute('href');
          console.log('[TOC Click] Original href:', href);
          
          if (href && href.startsWith('#')) {
            const idFromHref = decodeURIComponent(href.substring(1)); // 使用 decode
            console.log('[TOC Click] ID from href (decoded):', idFromHref);

            let targetElement = document.getElementById(idFromHref);
            console.log('[TOC Click] Target from getElementById with decoded ID:', targetElement);
            
            // 尝试回退方案：如果没找到精确ID，尝试查找内容匹配的标题
            if (!targetElement && this.$refs.contentDiv) {
              console.log('[TOC Click] Fallback: Searching by text content because getElementById failed.');
              const linkText = oldLink.textContent.trim();
              console.log('[TOC Click] Fallback: Link text:', linkText);
              
              // 查找所有标题元素
              const headings = this.$refs.contentDiv.querySelectorAll('h1, h2, h3, h4, h5, h6');
              
              // 尝试使用内容匹配
              for (const heading of headings) {
                const headingText = heading.textContent.trim();
                const headingId = heading.id;
                console.log(`[TOC Click] Fallback: Checking heading: Text="${headingText}", Existing ID="${headingId}"`);

                if (headingText === linkText) {
                  targetElement = heading;
                  console.log('[TOC Click] Fallback: Matched heading by text:', targetElement);
                  // 动态添加ID以便将来引用
                  if (!heading.id) {
                    console.log('[TOC Click] Fallback: Setting ID on matched heading (was empty):', idFromHref);
                    heading.id = idFromHref;
                  } else if (heading.id !== idFromHref) {
                    // 如果ID存在但不匹配从href解码得到的ID，这可能指示slugify逻辑与TOC生成href的逻辑存在不一致
                    // 或者页面中存在重复的文本标题但对应不同的slug。
                    // 在这种情况下，我们仍然信任从href派生的ID，因为它与用户点击的链接直接相关。
                    console.warn(`[TOC Click] Fallback: Matched heading's existing ID ("${heading.id}") does not match ID from href ("${idFromHref}"). Overwriting ID on heading to ensure navigation to the correct link's target.`);
                    heading.id = idFromHref; 
                  }
                  break;
                }
              }
            }
            
            if (targetElement) {
              console.log('[TOC Click] Scrolling to target:', targetElement, 'with ID:', targetElement.id);
              // 滚动到目标元素
              targetElement.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
              });
              
              // 添加高亮效果
              targetElement.classList.add('highlighted');
              setTimeout(() => {
                targetElement.classList.remove('highlighted');
              }, 2000);
            } else {
              console.warn('[TOC Click] Target element not found for href:', href, 'and decoded ID:', idFromHref);
            }
          }
          
          // 设置当前活跃项
          tocLinks.forEach(tl => tl.classList.remove('active'));
          oldLink.classList.add('active');
        });
      });
    }
  }
}
</script>

<style lang="less" scoped>
/* 全局添加平滑滚动效果 */
:deep(.markdown-page),
:deep(.page-container),
:deep(.content-wrapper),
:deep(.main-content),
:deep(.markdown-content) {
  scroll-behavior: smooth;
}

/* 固定显示的返回顶部按钮 - 居中显示 */
.fixed-back-top {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 50px;
  width: 120px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #1890ff;
  color: white;
  border-radius: 20px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 10000;
  font-size: 14px;
  font-weight: bold;
  transition: all 0.3s;
  
  .anticon {
    margin-right: 4px;
    font-size: 18px;
  }
  
  &:hover {
    background-color: #40a9ff;
    transform: translateX(-50%) translateY(-3px);
  }
  
  &:active {
    background-color: #096dd9;
    transform: translateX(-50%) translateY(0);
  }
}
</style> 