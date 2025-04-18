<template>
  <div class="k8s-config-container">
    <!-- 顶部标题区域 -->
    <div class="page-header">
      <div class="header-icon-wrapper">
        <div class="kubernetes-logo"></div>
      </div>
      <div class="header-content">
        <h2 class="title">{{ serviceLabel || serviceName }} Kubernetes 仪表盘</h2>
        <p class="subtitle">查看和管理{{ serviceLabel || serviceName }}服务的Kubernetes资源</p>
      </div>
      <!-- 命名空间选择器 -->
      <div class="namespace-selector">
        <a-select
          v-model="selectedNamespace"
          placeholder="请选择命名空间"
          :loading="namespacesLoading"
          @change="handleNamespaceChange"
          style="width: 200px"
        >
          <a-select-option value="all">所有命名空间</a-select-option>
          <a-select-option v-for="ns in namespaces" :key="ns.name" :value="ns.name">
            {{ ns.name }}
          </a-select-option>
        </a-select>
      </div>
    </div>

    <div class="k8s-dashboard-layout">
      <!-- 左侧导航菜单 -->
      <div class="sidebar-menu">
        <!-- 工作负载分组 -->
        <div class="menu-group">
          <div class="group-title">工作负载</div>
          <div class="menu-item" :class="{ active: activeResource === 'cronjobs' }" @click="activeResource = 'cronjobs'">
            <span class="item-text">Cron Jobs</span>
            <span class="item-count">{{ cronJobs && cronJobs.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'daemonsets' }" @click="activeResource = 'daemonsets'">
            <span class="item-text">Daemon Sets</span>
            <span class="item-count">{{ daemonSets && daemonSets.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'deployments' }" @click="activeResource = 'deployments'">
            <span class="item-text">Deployments</span>
            <span class="item-count">{{ deployments && deployments.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'jobs' }" @click="activeResource = 'jobs'">
            <span class="item-text">Jobs</span>
            <span class="item-count">{{ jobs && jobs.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'pods' }" @click="activeResource = 'pods'">
            <span class="item-text">Pods</span>
            <span class="item-count">{{ pods && pods.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'replicasets' }" @click="activeResource = 'replicasets'">
            <span class="item-text">Replica Sets</span>
            <span class="item-count">{{ replicaSets && replicaSets.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'replicationcontrollers' }" @click="activeResource = 'replicationcontrollers'">
            <span class="item-text">Replication Controllers</span>
            <span class="item-count">{{ replicationControllers && replicationControllers.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'statefulsets' }" @click="activeResource = 'statefulsets'">
            <span class="item-text">Stateful Sets</span>
            <span class="item-count">{{ statefulSets && statefulSets.length || 0 }}</span>
          </div>
        </div>
        
        <div class="menu-group">
          <div class="group-title">服务</div>
          <div class="menu-item" :class="{ active: activeResource === 'service' }" @click="activeResource = 'service'">
            <span class="item-text">Services</span>
            <span class="item-count">{{ services && services.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'ingress' }" @click="activeResource = 'ingress'">
            <span class="item-text">Ingresses</span>
            <span class="item-count">{{ ingresses && ingresses.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'ingressclass' }" @click="activeResource = 'ingressclass'">
            <span class="item-text">Ingress Classes</span>
            <span class="item-count">{{ ingressClasses && ingressClasses.length || 0 }}</span>
          </div>
        </div>
        
        <div class="menu-group">
          <div class="group-title">配置和存储</div>
          <div class="menu-item" :class="{ active: activeResource === 'configmap' }" @click="activeResource = 'configmap'">
            <span class="item-text">Config Maps</span>
            <span class="item-count">{{ configMaps && configMaps.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'secret' }" @click="activeResource = 'secret'">
            <span class="item-text">Secrets</span>
            <span class="item-count">{{ secrets && secrets.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'pv' }" @click="activeResource = 'pv'">
            <span class="item-text">Persistent Volumes</span>
            <span class="item-count">{{ persistentVolumes && persistentVolumes.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'pvc' }" @click="activeResource = 'pvc'">
            <span class="item-text">Persistent Volume Claims</span>
            <span class="item-count">{{ pvcs && pvcs.length || 0 }}</span>
          </div>
          <div class="menu-item" :class="{ active: activeResource === 'storageclass' }" @click="activeResource = 'storageclass'">
            <span class="item-text">Storage Classes</span>
            <span class="item-count">{{ storageClasses && storageClasses.length || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div class="content-area">
        <!-- ConfigMap列表 -->
        <div v-if="activeResource === 'configmap'" class="resource-list">
          <div class="resource-header">
            <h3>Config Maps</h3>
          </div>
        <a-spin :spinning="loading">
          <a-table
            :columns="configMapColumns"
            :dataSource="configMaps"
            :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
          >
            <template #action="{ record }">
                <div class="action-buttons">
              <a @click="handleViewConfigMap(record)">查看</a>
              <a-divider type="vertical" />
              <a @click="handleEditConfigMap(record)">编辑</a>
                </div>
            </template>
            <template #labels="{ text }">
              <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                {{ key }}: {{ value }}
                  </a-tag>
              </div>
              <span v-else>-</span>
            </template>
            <template #time="{ text }">
              <span>{{ formatTime(text) }}</span>
            </template>
          </a-table>
        </a-spin>
        </div>

        <!-- Service列表 -->
        <div v-if="activeResource === 'service'" class="resource-list">
          <div class="resource-header">
            <h3>Services</h3>
          </div>
        <a-spin :spinning="loading">
          <a-table
            :columns="serviceColumns"
            :dataSource="services"
            :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
          >
            <template #action="{ record }">
                <div class="action-buttons">
              <a @click="handleViewService(record)">查看</a>
              <a-divider type="vertical" />
              <a @click="handleEditService(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
            </template>
          </a-table>
        </a-spin>
        </div>

        <!-- Ingress列表 -->
        <div v-if="activeResource === 'ingress'" class="resource-list">
          <div class="resource-header">
            <h3>Ingresses</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="ingressColumns"
              :dataSource="ingresses"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewIngress(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditIngress(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- IngressClass列表 -->
        <div v-if="activeResource === 'ingressclass'" class="resource-list">
          <div class="resource-header">
            <h3>Ingress Classes</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="ingressClassColumns"
              :dataSource="ingressClasses"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewIngressClass(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditIngressClass(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- Secret列表 -->
        <div v-if="activeResource === 'secret'" class="resource-list">
          <div class="resource-header">
            <h3>Secrets</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="secretColumns"
              :dataSource="secrets"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewSecret(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditSecret(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
              <template #type="{ text }">
                <a-tag color="green">{{ text }}</a-tag>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- PV列表 -->
        <div v-if="activeResource === 'pv'" class="resource-list">
          <div class="resource-header">
            <h3>Persistent Volumes</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="pvColumns"
              :dataSource="persistentVolumes"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewPv(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditPv(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
              <template #status="{ text }">
                <a-tag :color="getPvStatusColor(text)">{{ text }}</a-tag>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- PVC列表 -->
        <div v-if="activeResource === 'pvc'" class="resource-list">
          <div class="resource-header">
            <h3>Persistent Volume Claims</h3>
          </div>
        <a-spin :spinning="loading">
          <a-table
            :columns="pvcColumns"
            :dataSource="pvcs"
            :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
          >
            <template #action="{ record }">
                <div class="action-buttons">
              <a @click="handleViewPvc(record)">查看</a>
              <a-divider type="vertical" />
              <a @click="handleEditPvc(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
              <template #status="{ text }">
                <a-tag :color="getPvcStatusColor(text)">{{ text }}</a-tag>
            </template>
          </a-table>
        </a-spin>
        </div>

        <!-- Storage Class列表 -->
        <div v-if="activeResource === 'storageclass'" class="resource-list">
          <div class="resource-header">
            <h3>Storage Classes</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="storageClassColumns"
              :dataSource="storageClasses"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewStorageClass(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditStorageClass(record)">编辑</a>
                </div>
              </template>
              <template #provisioner="{ text }">
                <span>{{ text }}</span>
              </template>
              <template #default="{ text }">
                <a-tag v-if="text" color="orange">默认</a-tag>
                <span v-else>-</span>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- CronJob列表 -->
        <div v-if="activeResource === 'cronjobs'" class="resource-list">
          <div class="resource-header">
            <h3>Cron Jobs</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="cronJobColumns"
              :dataSource="cronJobs"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewCronJob(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditCronJob(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
              <template #schedule="{ text }">
                <span>{{ text }}</span>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- DaemonSet列表 -->
        <div v-if="activeResource === 'daemonsets'" class="resource-list">
          <div class="resource-header">
            <h3>Daemon Sets</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="daemonSetColumns"
              :dataSource="daemonSets"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewDaemonSet(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditDaemonSet(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- Deployment列表 -->
        <div v-if="activeResource === 'deployments'" class="resource-list">
          <!-- 顶部图表区域 -->
          <div class="charts-container">
            <div class="chart-card">
              <div class="chart-header">
                <h3>CPU Usage</h3>
                <a-icon type="fullscreen" />
              </div>
              <div class="chart-content">
                <div class="chart" ref="cpuChart"></div>
              </div>
            </div>
            
            <div class="chart-card">
              <div class="chart-header">
                <h3>Memory Usage</h3>
                <a-icon type="fullscreen" />
              </div>
              <div class="chart-content">
                <div class="chart" ref="memoryChart"></div>
              </div>
            </div>
          </div>
          
          <!-- Deployments列表区域 -->
          <div class="deployments-list-container">
            <div class="list-header">
              <h3>Deployments</h3>
              <div class="header-actions">
                <a-tooltip title="过滤">
                  <a-icon type="filter" class="action-icon" />
                </a-tooltip>
                <a-tooltip title="展开/收起">
                  <a-icon type="arrows-alt" class="action-icon" />
                </a-tooltip>
              </div>
            </div>
            <a-spin :spinning="loading">
              <a-table 
                :columns="deploymentColumns" 
                :dataSource="deployments" 
                :pagination="false"
                :rowKey="record => record.name"
                class="k8s-table"
              >
                <template #statusDot="{ record }">
                  <span class="status-dot" :class="{'status-running': record.readyReplicas === record.replicas, 'status-warning': record.readyReplicas !== record.replicas}"></span>
                </template>
                
                <template #name="{ record }">
                  <div class="name-cell">
                    <span class="name-text">{{ record.name }}</span>
                  </div>
                </template>
                
                <template #image="{ text }">
                  <div class="image-cell">
                    <a-tooltip :title="text">
                      <span class="image-text">{{ text }}</span>
                    </a-tooltip>
                  </div>
                </template>
                
                <template #labels="{ text }">
                  <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                    <a-tag v-for="(value, key) in text" :key="key" color="blue">
                      {{ key }}: {{ value }}
                    </a-tag>
                  </div>
                  <span v-else>-</span>
                </template>
                
                <template #pods="{ record }">
                  <span>{{ record.readyReplicas || 0 }} / {{ record.replicas || 0 }}</span>
                </template>
                
                <template #creationTime="{ text }">
                  <span>{{ formatTime(text) }}</span>
                </template>
                
                <template #action="{ record }">
                  <div class="action-buttons">
                    <a-dropdown :trigger="['click']">
                      <a-button type="link" size="small">
                        操作 <a-icon type="down" />
                      </a-button>
                      <a-menu slot="overlay">
                        <a-menu-item @click="handleViewDeployment(record)">查看详情</a-menu-item>
                        <a-menu-item @click="handleEditDeployment(record)">编辑</a-menu-item>
                        <a-menu-item @click="handleScaleDeployment(record)">伸缩</a-menu-item>
                      </a-menu>
                    </a-dropdown>
                  </div>
                </template>
              </a-table>
            </a-spin>
          </div>
        </div>

        <!-- Job列表 -->
        <div v-if="activeResource === 'jobs'" class="resource-list">
          <div class="resource-header">
            <h3>Jobs</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="jobColumns"
              :dataSource="jobs"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewJob(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditJob(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- Pod列表 -->
        <div v-if="activeResource === 'pods'" class="resource-list">
          <div class="resource-header">
            <h3>Pods</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="podColumns"
              :dataSource="pods"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewPod(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditPod(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
              <template #status="{ text }">
                <a-tag :color="getPodStatusColor(text)">{{ text }}</a-tag>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- ReplicaSet列表 -->
        <div v-if="activeResource === 'replicasets'" class="resource-list">
          <div class="resource-header">
            <h3>Replica Sets</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="replicaSetColumns"
              :dataSource="replicaSets"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewReplicaSet(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditReplicaSet(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- ReplicationController列表 -->
        <div v-if="activeResource === 'replicationcontrollers'" class="resource-list">
          <div class="resource-header">
            <h3>Replication Controllers</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="replicationControllerColumns"
              :dataSource="replicationControllers"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewReplicationController(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditReplicationController(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
            </a-table>
          </a-spin>
        </div>

        <!-- StatefulSet列表 -->
        <div v-if="activeResource === 'statefulsets'" class="resource-list">
          <div class="resource-header">
            <h3>Stateful Sets</h3>
          </div>
          <a-spin :spinning="loading">
            <a-table
              :columns="statefulSetColumns"
              :dataSource="statefulSets"
              :pagination="false"
              :rowKey="record => record.name"
              class="k8s-table"
            >
              <template #action="{ record }">
                <div class="action-buttons">
                  <a @click="handleViewStatefulSet(record)">查看</a>
                  <a-divider type="vertical" />
                  <a @click="handleEditStatefulSet(record)">编辑</a>
                </div>
              </template>
              <template #labels="{ text }">
                <div class="tag-list" v-if="text && Object.keys(text).length > 0">
                  <a-tag v-for="(value, key) in text" :key="key" color="blue">
                    {{ key }}: {{ value }}
                  </a-tag>
                </div>
                <span v-else>-</span>
              </template>
            </a-table>
          </a-spin>
        </div>
      </div>
    </div>

    <DeploymentView
      v-if="deploymentViewVisible"
      :namespace="currentDeployment.namespace"
      :deploymentName="currentDeployment.name"
      :visible="deploymentViewVisible"
      @update:visible="deploymentViewVisible = $event"
    />
  </div>
</template>

<script>
import { defineComponent, ref, reactive } from 'vue'
import DeploymentView from '../components/DeploymentView.vue'

export default defineComponent({
  name: 'KubernetesDashboard',
  components: {
    DeploymentView
  },
  props: {
    serviceId: {
      type: [Number, String],
      required: true
    },
    serviceName: {
      type: String,
      required: true,
      default: '未知服务'
    },
    serviceLabel: {
      type: String,
      default: ''
    },
    clusterId: {
      type: Number,
      required: true,
      default: 1
    }
  },
  data() {
    return {
      namespaces: [],
      selectedNamespace: 'all',
      namespacesLoading: false,
      activeResource: 'configmap', // 默认显示ConfigMap
      // 工作负载
      cronJobs: [],
      daemonSets: [],
      deployments: [],
      jobs: [],
      pods: [],
      replicaSets: [],
      replicationControllers: [],
      statefulSets: [],
      // 服务
      configMaps: [],
      services: [],
      pvcs: [],
      ingresses: [],
      ingressClasses: [],
      secrets: [],
      persistentVolumes: [],
      storageClasses: [],
      loading: false,
      // 工作负载表格列配置
      cronJobColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
          slots: { customRender: 'labels' }
        },
        {
          title: '计划',
          dataIndex: 'schedule',
          key: 'schedule',
          width: '15%',
          slots: { customRender: 'schedule' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      daemonSetColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '所需',
          dataIndex: 'desired',
          key: 'desired',
          width: '10%',
        },
        {
          title: '当前',
          dataIndex: 'current',
          key: 'current',
          width: '10%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      deploymentColumns: [
        {
          title: '',
          dataIndex: 'status',
          key: 'status',
          width: '20px',
          slots: { customRender: 'statusDot' }
        },
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
          slots: { customRender: 'name' }
        },
        {
          title: '镜像',
          dataIndex: 'image',
          key: 'image',
          width: '25%',
          slots: { customRender: 'image' }
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '20%',
          slots: { customRender: 'labels' }
        },
        {
          title: 'Pods',
          key: 'pods',
          width: '10%',
          slots: { customRender: 'pods' }
        },
        {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'creationTime',
          width: '15%',
          slots: { customRender: 'creationTime' },
          sorter: (a, b) => {
            return new Date(a.createTime) - new Date(b.createTime);
          }
        },
        {
          title: '操作',
          key: 'action',
          width: '10%',
          slots: { customRender: 'action' }
        }
      ],
      jobColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '完成度',
          dataIndex: 'completions',
          key: 'completions',
          width: '15%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      podColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '20%',
          slots: { customRender: 'labels' }
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '10%',
          slots: { customRender: 'status' }
        },
        {
          title: 'IP',
          dataIndex: 'ip',
          key: 'ip',
          width: '15%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      replicaSetColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '所需副本',
          dataIndex: 'desired',
          key: 'desired',
          width: '10%',
        },
        {
          title: '当前副本',
          dataIndex: 'current',
          key: 'current',
          width: '10%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      replicationControllerColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '所需副本',
          dataIndex: 'desired',
          key: 'desired',
          width: '10%',
        },
        {
          title: '当前副本',
          dataIndex: 'current',
          key: 'current',
          width: '10%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      statefulSetColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '15%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '副本',
          dataIndex: 'replicas',
          key: 'replicas',
          width: '10%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      // 现有的表格列配置
      configMapColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '35%',
          slots: { customRender: 'labels' }
        },
        {
          title: '创建时间',
          dataIndex: 'time',
          key: 'time',
          width: '25%',
          slots: { customRender: 'time' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      serviceColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
          slots: { customRender: 'labels' }
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: '15%',
        },
        {
          title: 'Cluster IP',
          dataIndex: 'clusterIP',
          key: 'clusterIP',
          width: '20%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      ingressColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '25%',
          slots: { customRender: 'labels' }
        },
        {
          title: '主机',
          dataIndex: 'hosts',
          key: 'hosts',
          width: '25%',
        },
        {
          title: '地址',
          dataIndex: 'address',
          key: 'address',
          width: '15%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      ingressClassColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '控制器',
          dataIndex: 'controller',
          key: 'controller',
          width: '30%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
          slots: { customRender: 'labels' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      secretColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: '15%',
          slots: { customRender: 'type' }
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
          slots: { customRender: 'labels' }
        },
        {
          title: '创建时间',
          dataIndex: 'time',
          key: 'time',
          width: '15%',
          slots: { customRender: 'time' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      pvColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
        },
        {
          title: '容量',
          dataIndex: 'capacity',
          key: 'capacity',
          width: '15%',
        },
        {
          title: '访问模式',
          dataIndex: 'accessModes',
          key: 'accessModes',
          width: '15%',
        },
        {
          title: '回收策略',
          dataIndex: 'reclaimPolicy',
          key: 'reclaimPolicy',
          width: '15%',
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '15%',
          slots: { customRender: 'status' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      pvcColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '20%',
        },
        {
          title: '标签',
          dataIndex: 'labels',
          key: 'labels',
          width: '30%',
          slots: { customRender: 'labels' }
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '15%',
          slots: { customRender: 'status' }
        },
        {
          title: '容量',
          dataIndex: 'capacity',
          key: 'capacity',
          width: '20%',
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      storageClassColumns: [
        {
          title: '名称',
          dataIndex: 'name',
          key: 'name',
          width: '25%',
        },
        {
          title: '供应商',
          dataIndex: 'provisioner',
          key: 'provisioner',
          width: '30%',
          slots: { customRender: 'provisioner' }
        },
        {
          title: '回收策略',
          dataIndex: 'reclaimPolicy',
          key: 'reclaimPolicy',
          width: '15%',
        },
        {
          title: '默认类',
          dataIndex: 'isDefault',
          key: 'isDefault',
          width: '15%',
          slots: { customRender: 'default' }
        },
        {
          title: '操作',
          key: 'action',
          width: '15%',
          slots: { customRender: 'action' },
        },
      ],
      deploymentViewVisible: false,
      currentDeployment: {
        namespace: '',
        name: ''
      },
      // 添加图表相关属性
      cpuChart: null,
      memoryChart: null,
    };
  },
  methods: {
    async fetchK8sResources() {
      this.loading = true;
      try {
        // 确保所有数组已初始化
        this.deployments = this.deployments || [];
        this.pods = this.pods || [];
        this.services = this.services || [];
        this.configMaps = this.configMaps || [];
        this.cronJobs = this.cronJobs || [];
        this.daemonSets = this.daemonSets || [];
        this.jobs = this.jobs || [];
        this.replicaSets = this.replicaSets || [];
        this.replicationControllers = this.replicationControllers || [];
        this.statefulSets = this.statefulSets || [];
        this.ingresses = this.ingresses || [];
        this.ingressClasses = this.ingressClasses || [];
        this.secrets = this.secrets || [];
        this.persistentVolumes = this.persistentVolumes || [];
        this.pvcs = this.pvcs || [];
        this.storageClasses = this.storageClasses || [];
    
        // 先获取命名空间列表
        if (this.namespaces.length === 0) {
          await this.fetchNamespaces();
        }
        
        // 根据当前显示的资源类型加载数据
        if (this.activeResource === 'deployments') {
          await this.fetchDeployments();
          // 初始化图表
          this.$nextTick(() => {
            this.initCharts();
          });
        } else if (this.activeResource === 'pods') {
          await this.fetchPods();
        } else if (this.activeResource === 'services') {
          await this.fetchServices();
        } else if (this.activeResource === 'configmap') {
          await this.fetchConfigMaps();
        } else if (this.activeResource === 'daemonsets') {
          await this.fetchDaemonSets();
        } else if (this.activeResource === 'statefulsets') {
          await this.fetchStatefulSets();
        } else if (this.activeResource === 'replicasets') {
          await this.fetchReplicaSets();
        } else if (this.activeResource === 'replicationcontrollers') {
          await this.fetchReplicationControllers();
        } else if (this.activeResource === 'jobs') {
          await this.fetchJobs();
        } else if (this.activeResource === 'cronjobs') {
          await this.fetchCronJobs();
        }
        
      } catch (error) {
        console.error('Error fetching K8s resources:', error);
        this.$message.error(`获取Kubernetes资源失败: ${error.message || '未知错误'}`);
      } finally {
        this.loading = false;
      }
    },
    // 获取命名空间
    async fetchNamespaces() {
      this.namespacesLoading = true;
      try {
        const res = await this.$axiosGet(global.API.getK8sNamespaces, {
          clusterId: this.clusterId,
          serviceName: this.serviceName ? this.serviceName.toUpperCase() : this.serviceName
        });
        if (res.code === 200) {
          this.namespaces = res.data || [];
        } else {
          console.error('Failed to fetch namespaces:', res.msg);
          this.namespaces = [];
        }
      } catch (error) {
        console.error('Error fetching namespaces:', error);
        this.namespaces = [];
      } finally {
        this.namespacesLoading = false;
      }
    },
    // 命名空间变化处理
    handleNamespaceChange(value) {
      this.selectedNamespace = value;
      this.fetchK8sResources(); // 重新加载资源
    },
    // 工作负载相关方法
    async fetchCronJobs() {
      try {
        const res = await this.$axiosGet(global.API.getK8sCronJobs, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          serviceName: this.serviceName ? this.serviceName.toUpperCase() : this.serviceName
        });
        if (res.code === 200) {
          this.cronJobs = res.data || [];
        } else {
          console.error('Failed to fetch cron jobs:', res.msg);
          this.cronJobs = [];
        }
      } catch (error) {
        console.error('Error fetching cron jobs:', error);
        this.cronJobs = [];
      }
    },
    async fetchDaemonSets() {
      try {
        const res = await this.$axiosGet(global.API.getK8sDaemonSets, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          serviceName: this.serviceName ? this.serviceName.toUpperCase() : this.serviceName
        });
        if (res.code === 200) {
          this.daemonSets = res.data || [];
        } else {
          console.error('Failed to fetch daemon sets:', res.msg);
          this.daemonSets = [];
        }
      } catch (error) {
        console.error('Error fetching daemon sets:', error);
        this.daemonSets = [];
      }
    },
    async fetchDeployments() {
      try {
        const res = await this.$axiosGet(global.API.getK8sDeployments, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          serviceName: this.serviceName ? this.serviceName.toUpperCase() : this.serviceName
        });
        if (res.code === 200) {
          this.deployments = res.data || [];
        } else {
          console.error('Failed to fetch deployments:', res.msg);
          this.deployments = [];
        }
      } catch (error) {
        console.error('Error fetching deployments:', error);
        this.deployments = [];
      }
    },
    async fetchJobs() {
      try {
        const res = await this.$axiosGet(global.API.getK8sJobs, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          serviceName: this.serviceName ? this.serviceName.toUpperCase() : this.serviceName
        });
        if (res.code === 200) {
          this.jobs = res.data || [];
        } else {
          console.error('Failed to fetch jobs:', res.msg);
          this.jobs = [];
        }
      } catch (error) {
        console.error('Error fetching jobs:', error);
        this.jobs = [];
      }
    },
    async fetchPods() {
      try {
        const res = await this.$axiosGet(global.API.getK8sPods, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace
        });
        if (res.code === 200) {
          this.pods = res.data || [];
        } else {
          console.error('Failed to fetch pods:', res.msg);
          this.pods = [];
        }
      } catch (error) {
        console.error('Error fetching pods:', error);
        this.pods = [];
      }
    },
    async fetchReplicaSets() {
      try {
        const res = await this.$axiosGet(global.API.getK8sReplicaSets, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          serviceName: this.serviceName ? this.serviceName.toUpperCase() : this.serviceName
        });
        if (res.code === 200) {
          this.replicaSets = res.data || [];
        } else {
          console.error('Failed to fetch replica sets:', res.msg);
          this.replicaSets = [];
        }
      } catch (error) {
        console.error('Error fetching replica sets:', error);
        this.replicaSets = [];
      }
    },
    async fetchReplicationControllers() {
      try {
        const res = await this.$axiosGet(global.API.getK8sReplicationControllers, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          serviceName: this.serviceName ? this.serviceName.toUpperCase() : this.serviceName
        });
        if (res.code === 200) {
          this.replicationControllers = res.data || [];
        } else {
          console.error('Failed to fetch replication controllers:', res.msg);
          this.replicationControllers = [];
        }
      } catch (error) {
        console.error('Error fetching replication controllers:', error);
        this.replicationControllers = [];
      }
    },
    async fetchStatefulSets() {
      try {
        const res = await this.$axiosGet(global.API.getK8sStatefulSets, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          serviceName: this.serviceName ? this.serviceName.toUpperCase() : this.serviceName
        });
        if (res.code === 200) {
          this.statefulSets = res.data || [];
        } else {
          console.error('Failed to fetch stateful sets:', res.msg);
          this.statefulSets = [];
        }
      } catch (error) {
        console.error('Error fetching stateful sets:', error);
        this.statefulSets = [];
      }
    },
    // 工作负载处理方法
    handleViewCronJob(record) {
      // TODO: 实现查看CronJob的逻辑
      this.$message.info(`查看CronJob ${record.name} 的功能正在开发中`);
    },
    handleEditCronJob(record) {
      // TODO: 实现编辑CronJob的逻辑
      this.$message.info(`编辑CronJob ${record.name} 的功能正在开发中`);
    },
    handleViewDaemonSet(record) {
      // TODO: 实现查看DaemonSet的逻辑
      this.$message.info(`查看DaemonSet ${record.name} 的功能正在开发中`);
    },
    handleEditDaemonSet(record) {
      // TODO: 实现编辑DaemonSet的逻辑
      this.$message.info(`编辑DaemonSet ${record.name} 的功能正在开发中`);
    },
    handleViewDeployment(record) {
      this.currentDeployment = {
        namespace: record.namespace,
        name: record.name
      }
      this.deploymentViewVisible = true
    },
    handleEditDeployment(record) {
      // TODO: 实现编辑Deployment的逻辑
      this.$message.info(`编辑Deployment ${record.name} 的功能正在开发中`);
    },
    handleViewJob(record) {
      // TODO: 实现查看Job的逻辑
      this.$message.info(`查看Job ${record.name} 的功能正在开发中`);
    },
    handleEditJob(record) {
      // TODO: 实现编辑Job的逻辑
      this.$message.info(`编辑Job ${record.name} 的功能正在开发中`);
    },
    handleViewPod(record) {
      // TODO: 实现查看Pod的逻辑
    },
    handleEditPod(record) {
      // TODO: 实现编辑Pod的逻辑
    },
    handleViewReplicaSet(record) {
      // TODO: 实现查看ReplicaSet的逻辑
      this.$message.info(`查看ReplicaSet ${record.name} 的功能正在开发中`);
    },
    handleEditReplicaSet(record) {
      // TODO: 实现编辑ReplicaSet的逻辑
      this.$message.info(`编辑ReplicaSet ${record.name} 的功能正在开发中`);
    },
    handleViewReplicationController(record) {
      // TODO: 实现查看ReplicationController的逻辑
      this.$message.info(`查看ReplicationController ${record.name} 的功能正在开发中`);
    },
    handleEditReplicationController(record) {
      // TODO: 实现编辑ReplicationController的逻辑
      this.$message.info(`编辑ReplicationController ${record.name} 的功能正在开发中`);
    },
    handleViewStatefulSet(record) {
      // TODO: 实现查看StatefulSet的逻辑
    },
    handleEditStatefulSet(record) {
      // TODO: 实现编辑StatefulSet的逻辑
    },
    // 配置和服务处理方法
    handleViewConfigMap(record) {
      // TODO: 实现查看ConfigMap的逻辑
      this.$message.info(`查看ConfigMap ${record.name} 的功能正在开发中`);
    },
    handleEditConfigMap(record) {
      // TODO: 实现编辑ConfigMap的逻辑
      this.$message.info(`编辑ConfigMap ${record.name} 的功能正在开发中`);
    },
    handleViewService(record) {
      // TODO: 实现查看Service的逻辑
      this.$message.info(`查看Service ${record.name} 的功能正在开发中`);
    },
    handleEditService(record) {
      // TODO: 实现编辑Service的逻辑
      this.$message.info(`编辑Service ${record.name} 的功能正在开发中`);
    },
    handleViewIngress(record) {
      // TODO: 实现查看Ingress的逻辑
      this.$message.info(`查看Ingress ${record.name} 的功能正在开发中`);
    },
    handleEditIngress(record) {
      // TODO: 实现编辑Ingress的逻辑
      this.$message.info(`编辑Ingress ${record.name} 的功能正在开发中`);
    },
    handleViewIngressClass(record) {
      // TODO: 实现查看IngressClass的逻辑
      this.$message.info(`查看IngressClass ${record.name} 的功能正在开发中`);
    },
    handleEditIngressClass(record) {
      // TODO: 实现编辑IngressClass的逻辑
      this.$message.info(`编辑IngressClass ${record.name} 的功能正在开发中`);
    },
    handleViewSecret(record) {
      // TODO: 实现查看Secret的逻辑
      this.$message.info(`查看Secret ${record.name} 的功能正在开发中`);
    },
    handleEditSecret(record) {
      // TODO: 实现编辑Secret的逻辑
      this.$message.info(`编辑Secret ${record.name} 的功能正在开发中`);
    },
    handleViewPv(record) {
      // TODO: 实现查看PersistentVolume的逻辑
      this.$message.info(`查看PersistentVolume ${record.name} 的功能正在开发中`);
    },
    handleEditPv(record) {
      // TODO: 实现编辑PersistentVolume的逻辑
      this.$message.info(`编辑PersistentVolume ${record.name} 的功能正在开发中`);
    },
    handleViewPvc(record) {
      // TODO: 实现查看PersistentVolumeClaim的逻辑
      this.$message.info(`查看PersistentVolumeClaim ${record.name} 的功能正在开发中`);
    },
    handleEditPvc(record) {
      // TODO: 实现编辑PersistentVolumeClaim的逻辑
      this.$message.info(`编辑PersistentVolumeClaim ${record.name} 的功能正在开发中`);
    },
    handleViewStorageClass(record) {
      // TODO: 实现查看StorageClass的逻辑
      this.$message.info(`查看StorageClass ${record.name} 的功能正在开发中`);
    },
    handleEditStorageClass(record) {
      // TODO: 实现编辑StorageClass的逻辑
      this.$message.info(`编辑StorageClass ${record.name} 的功能正在开发中`);
    },
    // Pod状态颜色
    getPodStatusColor(status) {
      switch (status) {
        case 'Running':
          return 'green';
        case 'Pending':
          return 'orange';
        case 'Succeeded':
          return 'blue';
        case 'Failed':
          return 'red';
        case 'Unknown':
          return 'gray';
        default:
          return 'default';
      }
    },
    // 处理伸缩Deployment
    handleScaleDeployment(record) {
      this.$message.info(`伸缩Deployment ${record.name} 的功能正在开发中`);
    },
    
    // 初始化图表
    initCharts() {
      // 由于页面可能未加载完成，延迟初始化
      this.$nextTick(() => {
        const echarts = require('echarts');
        
        // 初始化CPU使用率图表
        if (this.$refs.cpuChart) {
          this.cpuChart = echarts.init(this.$refs.cpuChart);
          const cpuOption = {
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              top: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: this.generateTimeAxis(),
              axisTick: {
                alignWithLabel: true
              }
            },
            yAxis: {
              type: 'value',
              name: 'CPU (cores)',
              min: 0
            },
            series: [{
              data: this.generateRandomData(0, 0.01),
              type: 'line',
              smooth: true,
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: 'rgba(128, 255, 165, 0.8)'
                  }, {
                    offset: 1, color: 'rgba(128, 255, 165, 0.1)'
                  }]
                }
              },
              itemStyle: {
                color: '#10b981'
              },
              lineStyle: {
                width: 2,
                color: '#10b981'
              }
            }]
          };
          this.cpuChart.setOption(cpuOption);
        }
        
        // 初始化内存使用率图表
        if (this.$refs.memoryChart) {
          this.memoryChart = echarts.init(this.$refs.memoryChart);
          const memoryOption = {
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              top: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: this.generateTimeAxis(),
              axisTick: {
                alignWithLabel: true
              }
            },
            yAxis: {
              type: 'value',
              name: 'Memory (bytes)',
              min: 0,
              max: 200,
              axisLabel: {
                formatter: '{value} Mi'
              }
            },
            series: [{
              data: this.generateRandomData(50, 100),
              type: 'line',
              smooth: true,
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: 'rgba(100, 149, 237, 0.8)'
                  }, {
                    offset: 1, color: 'rgba(100, 149, 237, 0.1)'
                  }]
                }
              },
              itemStyle: {
                color: '#3b82f6'
              },
              lineStyle: {
                width: 2,
                color: '#3b82f6'
              }
            }]
          };
          this.memoryChart.setOption(memoryOption);
        }
        
        // 添加窗口大小变化监听，调整图表大小
        window.addEventListener('resize', this.resizeCharts);
      });
    },
    
    // 生成时间轴数据
    generateTimeAxis() {
      const now = new Date();
      const times = [];
      for (let i = 12; i >= 0; i--) {
        const time = new Date(now.getTime() - i * 60000);
        times.push(time.getHours() + ':' + (time.getMinutes() < 10 ? '0' : '') + time.getMinutes());
      }
      return times;
    },
    
    // 生成随机数据用于图表展示
    generateRandomData(min, max) {
      const data = [];
      for (let i = 0; i < 13; i++) {
        data.push((Math.random() * (max - min) + min).toFixed(4));
      }
      return data;
    },
    
    // 更新图表
    updateCharts() {
      // 确保图表实例存在且未被销毁
      if (this.cpuChart && !this.cpuChart.isDisposed() && 
          this.memoryChart && !this.memoryChart.isDisposed()) {
        const times = this.generateTimeAxis();
        const cpuData = this.generateRandomData(0, 0.01);
        const memoryData = this.generateRandomData(50, 100);
        
        this.cpuChart.setOption({
          xAxis: {
            data: times
          },
          series: [{
            data: cpuData
          }]
        });
        
        this.memoryChart.setOption({
          xAxis: {
            data: times
          },
          series: [{
            data: memoryData
          }]
        });
      } else if (this.activeResource === 'deployments') {
        // 如果当前页面是deployments但图表已销毁，尝试重新初始化
        clearInterval(this.chartsInterval);
        this.$nextTick(() => {
          this.initCharts();
          // 重新设置定时器
          this.chartsInterval = setInterval(() => {
            this.updateCharts();
          }, 30000);
        });
      }
    },
    
    // 调整图表大小
    resizeCharts() {
      if (this.cpuChart && !this.cpuChart.isDisposed()) {
        this.cpuChart.resize();
      }
      if (this.memoryChart && !this.memoryChart.isDisposed()) {
        this.memoryChart.resize();
      }
    },
    
    async fetchPersistentVolumes() {
      // 这个方法暂未实现
      this.persistentVolumes = [];
    },
    
    async fetchServices() {
      try {
        const res = await this.$axiosGet(global.API.getK8sServices, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace
        });
        if (res.code === 200) {
          this.services = res.data || [];
        } else {
          console.error('Failed to fetch services:', res.msg);
          this.services = [];
        }
      } catch (error) {
        console.error('Error fetching services:', error);
        this.services = [];
      }
    },
    
    async fetchConfigMaps() {
      try {
        const res = await this.$axiosGet(global.API.getK8sConfigMaps, {
          clusterId: this.clusterId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace,
          serviceName: this.serviceName // 添加serviceName参数
        });
        if (res.code === 200) {
          this.configMaps = res.data || [];
        } else {
          console.error('Failed to fetch config maps:', res.msg);
          this.configMaps = [];
        }
      } catch (error) {
        console.error('Error fetching config maps:', error);
        this.configMaps = [];
      }
    },
  },
  mounted() {
    if (this.serviceId) {
      this.fetchK8sResources();
    } else {
      console.error('serviceId is required to fetch K8s resources');
    }
  },
  beforeDestroy() {
    // 清理定时器
    if (this.chartsInterval) {
      clearInterval(this.chartsInterval);
      this.chartsInterval = null;
    }
    
    // 清理图表资源
    if (this.cpuChart && !this.cpuChart.isDisposed()) {
      this.cpuChart.dispose();
      this.cpuChart = null;
    }
    if (this.memoryChart && !this.memoryChart.isDisposed()) {
      this.memoryChart.dispose();
      this.memoryChart = null;
    }
    
    // 移除窗口大小变化监听
    window.removeEventListener('resize', this.resizeCharts);
  },
  watch: {
    serviceId(newVal) {
      if (newVal) {
        this.fetchK8sResources();
      }
    },
    activeResource(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.fetchK8sResources();
        
        if (newVal === 'deployments') {
          // 当切换到deployments时初始化图表
          this.$nextTick(() => {
            // 设置定时刷新
            if (this.chartsInterval) {
              clearInterval(this.chartsInterval);
            }
            this.chartsInterval = setInterval(() => {
              this.updateCharts();
            }, 30000); // 每30秒更新一次
          });
        } else {
          // 切换到其他资源时清理定时器
          if (this.chartsInterval) {
            clearInterval(this.chartsInterval);
            this.chartsInterval = null;
          }
        }
      }
    },
    selectedNamespace(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.fetchK8sResources();
      }
    }
  }
});
</script>

<style scoped>
.k8s-config-container {
  padding: 24px;
  background-color: #f9fafc;
  min-height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e9ecef;
  background-color: transparent;
}

.header-icon-wrapper {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  background-color: transparent;
  border-radius: 0;
  box-shadow: none;
}

.kubernetes-logo {
  width: 42px;
  height: 42px;
  background-image: url("../../../../../assets/images/kubernetes-logo.svg");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.header-content {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.title {
  font-size: 22px;
  margin-bottom: 4px;
  color: #333333;
  font-weight: 500;
  letter-spacing: -0.3px;
}

.subtitle {
  font-size: 14px;
  color: #666666;
  font-weight: 400;
}

.namespace-selector {
  display: flex;
  align-items: center;
}

.namespace-selector :deep(.ant-select-selection) {
  background-color: #fff;
  border-radius: 6px;
  border: 1px solid #e1e4e8;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.namespace-selector :deep(.ant-select-selection:hover) {
  border-color: #4f7ff3;
}

.namespace-selector :deep(.ant-select-selection:focus) {
  border-color: #326CE5;
  box-shadow: 0 0 0 2px rgba(50, 108, 229, 0.2);
}

/* 新增的Dashboard布局样式 */
.k8s-dashboard-layout {
  display: flex;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  border: 1px solid #eaeaea;
}

/* 左侧导航菜单 */
.sidebar-menu {
  width: 250px;
  background-color: #f8f9fa;
  border-right: 1px solid #e9ecef;
  padding: 16px 0;
}

.menu-group {
  margin-bottom: 16px;
}

.group-title {
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #586069;
  text-transform: uppercase;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.menu-item:hover {
  background-color: #f1f3f5;
}

.menu-item.active {
  background-color: #e9ecef;
  color: #326CE5;
  font-weight: 500;
  border-left: 3px solid #326CE5;
}

.item-text {
  flex: 1;
  color: #24292e;
}

.item-count {
  font-size: 12px;
  padding: 2px 6px;
  background-color: #f1f3f5;
  border-radius: 10px;
  color: #586069;
}

/* 右侧内容区域 */
.content-area {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.resource-header {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e9ecef;
}

.resource-header h3 {
  font-size: 18px;
  color: #24292e;
  margin: 0;
}

/* 表格样式 */
.k8s-table {
  width: 100%;
  border-radius: 6px;
  overflow: hidden;
}

.k8s-table :deep(.ant-table-thead > tr > th) {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #586069;
  border-bottom: 1px solid #e9ecef;
}

.k8s-table :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f3f5;
}

.k8s-table :deep(.ant-table-tbody > tr:hover > td) {
  background-color: #f8f9fa;
}

.action-buttons {
  display: flex;
  align-items: center;
}

.action-buttons a {
  color: #326CE5;
}

.action-buttons a:hover {
  color: #4f7ff3;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* 图表区域样式 */
.charts-container {
  display: flex;
  width: 100%;
  margin-bottom: 24px;
  gap: 16px;
}

.chart-card {
  flex: 1;
  background-color: #ffffff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  overflow: hidden;
  border: 1px solid #eaeaea;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.chart-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.chart-content {
  padding: 16px;
  height: 200px;
}

.chart {
  width: 100%;
  height: 100%;
}

/* Deployments列表容器样式 */
.deployments-list-container {
  background-color: #ffffff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  overflow: hidden;
  border: 1px solid #eaeaea;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.list-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-icon {
  cursor: pointer;
  padding: 6px;
  color: #666;
}

.action-icon:hover {
  color: #1890ff;
}

/* 状态点样式 */
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #d9d9d9;
}

.status-running {
  background-color: #52c41a;
}

.status-warning {
  background-color: #faad14;
}

/* 名称单元格样式 */
.name-cell {
  display: flex;
  align-items: center;
}

.name-text {
  font-weight: 500;
  color: #1890ff;
  cursor: pointer;
}

.name-text:hover {
  text-decoration: underline;
}

/* 镜像单元格样式 */
.image-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>