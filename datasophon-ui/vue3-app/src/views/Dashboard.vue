<template>
  <div class="dashboard-container">
    <h1 class="page-title">主页</h1>
    <div class="dashboard-content">
      <div class="welcome-card">
        <h2>欢迎使用Noah大数据基础平台</h2>
        <p>从这里开始管理您的集群和服务</p>
      </div>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">
            <svg-icon icon-class="host" />
          </div>
          <div class="stat-content">
            <div class="stat-value">12</div>
            <div class="stat-label">主机数量</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon">
            <svg-icon icon-class="cluster" />
          </div>
          <div class="stat-content">
            <div class="stat-value">3</div>
            <div class="stat-label">集群数量</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon">
            <svg-icon icon-class="service" />
          </div>
          <div class="stat-content">
            <div class="stat-value">24</div>
            <div class="stat-label">服务数量</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon">
            <svg-icon icon-class="alarm" />
          </div>
          <div class="stat-content">
            <div class="stat-value">2</div>
            <div class="stat-label">告警数量</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Dashboard'
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
  color: #333;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-card {
  background-color: #fff;
  border-radius: 4px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  text-align: center;
}

.welcome-card h2 {
  font-size: 20px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.welcome-card p {
  font-size: 14px;
  color: #666;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(24, 144, 255, 0.1);
  border-radius: 12px;
  margin-right: 16px;
  color: #1890ff;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style> 