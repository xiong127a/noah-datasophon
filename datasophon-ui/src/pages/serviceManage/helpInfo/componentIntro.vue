<template>
  <div class="component-intro">
    <a-card class="card-shadow">
      <MarkdownDocViewer
        :service-id="serviceId"
        doc-type="component"
        empty-text="暂无组件介绍信息"
      />
    </a-card>
  </div>
</template>

<script>
import MarkdownDocViewer from './MarkdownDocViewer.vue';

export default {
  name: 'ComponentIntro',
  components: {
    MarkdownDocViewer
  },
  props: {
    serviceId: {
      type: String,
      required: true
    }
  },
  mounted() {
    // 添加调试代码，检查DOM结构
    setTimeout(() => {
      console.log('ComponentIntro mounted, checking DOM structure...');
      const sidebar = document.querySelector('.component-intro .sidebar');
      if (sidebar) {
        console.log('Sidebar found:', sidebar);
        
        // 检查父元素和定位计算结果
        let parent = sidebar.parentElement;
        let parentStyle;
        while (parent) {
          parentStyle = window.getComputedStyle(parent);
          console.log('Parent element:', parent.tagName, parent.className);
          console.log('Parent style - overflow:', parentStyle.overflow);
          console.log('Parent style - position:', parentStyle.position);
          
          if (parent.classList.contains('component-intro')) break;
          parent = parent.parentElement;
        }
        
        // 检查sidebar自身的样式
        const sidebarStyle = window.getComputedStyle(sidebar);
        console.log('Sidebar position:', sidebarStyle.position);
        console.log('Sidebar top:', sidebarStyle.top);
      } else {
        console.log('Sidebar element not found!');
      }
    }, 2000);
  }
}
</script>

<style lang="less">
.component-intro {
  width: 100%;
  
  // 覆盖目录侧边栏样式
  :deep(.content-wrapper .sidebar) {
    // 确保侧边栏固定
    position: sticky !important;
    top: 84px !important;
    align-self: flex-start !important;
    z-index: 5 !important;
    background-color: #fff;
    max-height: calc(100vh - 120px) !important;
  }
  
  :deep(.sidebar .custom-nav) {
    // 添加滚动条样式
    overflow-y: auto;
    max-height: calc(100vh - 200px);
    padding-right: 10px;
    
    // 隐藏默认滚动条
    &::-webkit-scrollbar {
      width: 8px;
    }
    
    // 滚动条轨道
    &::-webkit-scrollbar-track {
      background: transparent;
    }
    
    // 滚动条滑块
    &::-webkit-scrollbar-thumb {
      background-color: rgba(0, 0, 0, 0.2);
      border-radius: 4px;
      
      // 悬停时的样式
      &:hover {
        background-color: rgba(0, 0, 0, 0.4);
      }
    }
    
    // 仅在滚动时显示滚动条
    &:not(:hover)::-webkit-scrollbar-thumb {
      background-color: transparent;
    }
    
    // Firefox滚动条支持
    scrollbar-width: thin;
    scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
    
    // 目录项样式优化
    ul.table-of-contents {
      padding-left: 15px;
      
      li {
        margin-bottom: 6px;
        
        a {
          display: block;
          padding: 4px 0;
          color: rgba(0, 0, 0, 0.65);
          text-decoration: none;
          transition: all 0.3s;
          
          &:hover, &.active {
            color: #1890ff;
          }
        }
      }
    }
  }
}
</style> 