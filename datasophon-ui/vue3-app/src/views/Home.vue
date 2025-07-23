<template>
  <div class="home-container">
    <div class="welcome-section">
      <div class="logo-container">
        <img src="@/assets/img/logo.png" alt="Datasophon Logo" class="logo" />
      </div>
      <h1 class="welcome-title">欢迎使用 Datasophon</h1>
      <p class="welcome-subtitle">一站式大数据平台部署与管理解决方案</p>
    </div>
    
    <div class="main-actions">
      <div class="action-card" @click="navigateToCluster">
        <div class="card-icon">
          <svg-icon icon-class="colony" class="icon" />
        </div>
        <div class="card-content">
          <h3>集群管理</h3>
          <p>创建和管理您的大数据集群</p>
        </div>
      </div>
      
      <div class="action-card" @click="navigateToService">
        <div class="card-icon">
          <svg-icon icon-class="service" class="icon" />
        </div>
        <div class="card-content">
          <h3>服务管理</h3>
          <p>管理和监控您的服务组件</p>
        </div>
      </div>
      
      <div class="action-card" @click="navigateToHost">
        <div class="card-icon">
          <svg-icon icon-class="host" class="icon" />
        </div>
        <div class="card-content">
          <h3>主机管理</h3>
          <p>管理集群中的主机资源</p>
        </div>
      </div>
    </div>
    
    <div class="info-section">
      <div class="info-card">
        <h3>平台特性</h3>
        <ul class="feature-list">
          <li>一键部署 Hadoop 生态系统各组件</li>
          <li>图形化管理和监控各组件状态</li>
          <li>智能化配置管理和版本控制</li>
          <li>丰富的告警规则和通知方式</li>
          <li>细粒度的权限管理和租户隔离</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'

const router = useRouter()

const navigateToCluster = () => {
  router.push('/colony-manage/list')
}

const navigateToService = () => {
  router.push('/service-manage')
}

const navigateToHost = () => {
  router.push('/host')
}
</script>

<style scoped lang="scss">
$primary-color: #0066cc;
$primary-light: #4096ff;
$primary-dark: #0055aa;
$card-background: #ffffff;
$text-color: #1d1d1f;
$text-color-secondary: #86868b;
$border-radius: 16px;
$shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
$animation-easing: cubic-bezier(0.25, 0.1, 0.25, 1.0);

.home-container {
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

.welcome-section {
  text-align: center;
  margin-bottom: 40px;
  
  .logo-container {
    margin-bottom: 24px;
    
    .logo {
      height: 80px;
      object-fit: contain;
    }
  }
  
  .welcome-title {
    font-size: 36px;
    font-weight: 600;
    color: $text-color;
    margin-bottom: 16px;
    letter-spacing: -0.025em;
    line-height: 1.2;
  }
  
  .welcome-subtitle {
    font-size: 20px;
    font-weight: 400;
    color: $text-color-secondary;
    max-width: 600px;
    margin: 0 auto;
    line-height: 1.5;
  }
}

.main-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 48px;
  
  .action-card {
    background-color: $card-background;
    border-radius: $border-radius;
    padding: 32px;
    box-shadow: $shadow;
    transition: transform 0.3s $animation-easing, box-shadow 0.3s $animation-easing;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    
    &:hover {
      transform: translateY(-8px) scale(1.02);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
      
      .card-icon {
        transform: scale(1.1);
        background-color: rgba($primary-color, 0.1);
      }
    }
    
    .card-icon {
      width: 80px;
      height: 80px;
      background-color: rgba($primary-color, 0.05);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 24px;
      transition: all 0.3s $animation-easing;
      
      .icon {
        font-size: 36px;
        color: $primary-color;
      }
    }
    
    .card-content {
      h3 {
        font-size: 20px;
        font-weight: 600;
        color: $text-color;
        margin-bottom: 12px;
      }
      
      p {
        font-size: 16px;
        color: $text-color-secondary;
        line-height: 1.5;
      }
    }
  }
}

.info-section {
  .info-card {
    background-color: $card-background;
    border-radius: $border-radius;
    padding: 32px;
    box-shadow: $shadow;
    
    h3 {
      font-size: 24px;
      font-weight: 600;
      color: $text-color;
      margin-bottom: 24px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      padding-bottom: 16px;
    }
    
    .feature-list {
      list-style-type: none;
      padding: 0;
      margin: 0;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 16px;
      
      li {
        position: relative;
        padding-left: 28px;
        font-size: 16px;
        color: $text-color;
        line-height: 1.6;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 8px;
          width: 18px;
          height: 18px;
          background-color: rgba($primary-color, 0.1);
          border-radius: 50%;
        }
        
        &::after {
          content: '';
          position: absolute;
          left: 7px;
          top: 13px;
          width: 5px;
          height: 9px;
          border-right: 2px solid $primary-color;
          border-bottom: 2px solid $primary-color;
          transform: rotate(45deg);
        }
      }
    }
  }
}

// 响应式布局
@media (max-width: 768px) {
  .home-container {
    padding: 24px;
  }
  
  .welcome-section {
    margin-bottom: 36px;
    
    .welcome-title {
      font-size: 28px;
    }
    
    .welcome-subtitle {
      font-size: 16px;
    }
  }
  
  .main-actions {
    grid-template-columns: 1fr;
  }
  
  .info-section {
    .info-card {
      .feature-list {
        grid-template-columns: 1fr;
      }
    }
  }
}
</style> 