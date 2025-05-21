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
            <div class="custom-nav" v-html="tocHtml"></div>
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
      tocVisible: false
    }
  },
  mounted() {
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
      
      // 调用API获取文档
      this.$axiosPost(services.getServiceDoc, {
        clusterId: clusterId,
        serviceId: this.serviceId,
        type: this.docType
      }).then(response => {
        if (response.code === 200 && response.data) {
          this.mdContent = response.data;
          
          // 添加TOC标记到文档开头
          const contentWithToc = '[[toc]]\n\n' + response.data;
          
          // 将Markdown转换为HTML
          this.htmlContent = md.render(contentWithToc);
          
          // 提取TOC部分
          const tocElement = document.createElement('div');
          tocElement.innerHTML = this.htmlContent;
          const tocContainer = tocElement.querySelector('.toc-container');
          
          if (tocContainer) {
            this.tocHtml = tocContainer.outerHTML;
            this.tocVisible = true;
            
            // 从内容中移除TOC容器
            this.htmlContent = this.htmlContent.replace(tocContainer.outerHTML, '');
          }
          
          // 添加锚点点击事件处理
          this.$nextTick(() => {
            this.setupTocLinkHandlers();
          });
        } else {
          this.mdContent = null;
          this.htmlContent = '';
          this.tocHtml = '';
          this.tocVisible = false;
        }
        this.loading = false;
      }).catch(error => {
        this.mdContent = null;
        this.htmlContent = '';
        this.tocHtml = '';
        this.tocVisible = false;
        this.loading = false;
      });
    },
    setupTocLinkHandlers() {
      const tocLinks = this.$el.querySelectorAll('.toc-link');
      
      tocLinks.forEach((link, index) => {
        // 移除已存在的事件监听器以防止重复
        const oldLink = link.cloneNode(true);
        link.parentNode.replaceChild(oldLink, link);
        
        oldLink.addEventListener('click', (e) => {
          e.preventDefault();
          
          const href = oldLink.getAttribute('href');
          
          if (href && href.startsWith('#')) {
            const id = href.substring(1);
            let targetElement = document.getElementById(id);
            
            // 尝试回退方案：如果没找到精确ID，尝试查找内容匹配的标题
            if (!targetElement && this.$refs.contentDiv) {
              const linkText = oldLink.textContent.trim();
              
              // 查找所有标题元素
              const headings = this.$refs.contentDiv.querySelectorAll('h1, h2, h3, h4, h5, h6');
              
              // 尝试使用内容匹配
              for (const heading of headings) {
                if (heading.textContent.trim() === linkText) {
                  targetElement = heading;
                  // 动态添加ID以便将来引用
                  if (!heading.id) {
                    heading.id = id;
                  }
                  break;
                }
              }
            }
            
            if (targetElement) {
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