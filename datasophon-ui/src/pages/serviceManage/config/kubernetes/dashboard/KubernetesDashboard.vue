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
      <div class="namespace-selector" style="display: none;"><!-- 隐藏命名空间选择器 -->
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              <!-- 添加创建时间自定义渲染函数 -->
              <template #creationTime="{ record }">
                <span class="format-time-cell">
                  {{ formatTime(record.objectMeta?.creationTimestamp) }}
                </span>
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace || ''}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace || ''}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
          <div class="k8s-dashboard-charts">
            <!-- CPU Usage Chart -->
            <div class="k8s-chart-card">
              <div class="k8s-chart-header">
                <div class="k8s-chart-title">CPU Usage</div>
                <div class="k8s-chart-actions">
                  <a-icon type="fullscreen" class="k8s-action-icon" />
              </div>
              </div>
              <div class="k8s-chart-content">
                <div class="k8s-chart-y-label">CPU (cores)</div>
                <div ref="cpuChart" class="chart"></div>
              </div>
            </div>
            
            <!-- Memory Usage Chart -->
            <div class="k8s-chart-card">
              <div class="k8s-chart-header">
                <div class="k8s-chart-title">Memory Usage</div>
                <div class="k8s-chart-actions">
                  <a-icon type="fullscreen" class="k8s-action-icon" />
              </div>
              </div>
              <div class="k8s-chart-content">
                <div class="k8s-chart-y-label">Memory (bytes)</div>
                <div ref="memoryChart" class="chart"></div>
              </div>
            </div>
          </div>
          
          <!-- Deployments列表区域 -->
          <div class="k8s-dashboard-card k8s-resource-card">
            <div class="k8s-card-header">
              <span class="k8s-card-title">Deployments</span>
              <div class="k8s-card-actions">
                <a-icon type="bars" class="k8s-action-icon" />
                <a class="k8s-card-collapse-icon">
                  <a-icon type="minus" />
                </a>
              </div>
            </div>
            <div class="k8s-card-content">
            <a-spin :spinning="loading">
              <a-table 
                :columns="deploymentColumns" 
                :dataSource="deployments" 
                :pagination="false"
                  :rowKey="record => `${record?.objectMeta?.namespace || 'unknown'}-${record?.objectMeta?.name || 'unknown'}`"
                class="k8s-table"
              >
              </a-table>
            </a-spin>
            </div>
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
              :rowKey="record => `${record.namespace}-${record.name}`"
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
import dayjs from 'dayjs'

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
      selectedNamespace: 'datasophon', // 固定使用datasophon命名空间
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
          customRender: (text, record) => {
            const classNames = ['status-dot'];
            if (record?.pods?.running > 0) classNames.push('status-running');
            if (record?.pods?.pending > 0) classNames.push('status-warning');
            if (record?.pods?.failed > 0) classNames.push('status-danger');
            if (!record?.pods || (!record?.pods.running && !record?.pods.pending && !record?.pods.failed)) 
              classNames.push('status-unknown');
            return <span class={classNames.join(' ')}></span>;
          }
        },
        {
          title: '名称',
          dataIndex: 'objectMeta.name',
          key: 'name',
          width: '20%',
          customRender: (_, record) => {
            return <div class="name-cell">
              <span class="name-text">{record?.objectMeta?.name || '未知'}</span>
            </div>
          }
        },
        {
          title: '命名空间',
          dataIndex: 'namespace',
          key: 'namespace',
          width: '12%'
        },
        {
          title: '镜像',
          dataIndex: 'containerImages',
          key: 'image',
          width: '25%',
          customRender: (_, record) => {
            return <div class="image-cell">
              <a-tooltip title={record?.containerImages ? record.containerImages.join(', ') : ''}>
                <span class="image-text">{record?.containerImages ? record.containerImages.join(', ') : '-'}</span>
              </a-tooltip>
            </div>
          }
        },
        {
          title: '标签',
          key: 'labels',
          width: '20%',
          customRender: (text, record) => {
            if (!record.objectMeta?.labels || Object.keys(record.objectMeta.labels).length === 0) {
              return '-';
            }
            
            // 使用a-tag组件来模拟原始K8s Dashboard中的mat-chip组件
            return h('div', { class: 'labels-container' }, 
              Object.entries(record.objectMeta.labels).map(([key, value]) => {
                return h('a-tag', { 
                  props: { color: 'blue' },
                  class: 'label-chip',
                  key: key
                }, `${key}: ${value}`);
              })
            );
          }
        },
        {
          title: 'Pods',
          dataIndex: 'pods',
          key: 'pods',
          width: '10%',
          customRender: (_, record) => {
            return <div class="pods-display">
              <span>{record?.pods && record.pods.running !== undefined ? record.pods.running : 0} / {record?.pods && record.pods.desired !== undefined ? record.pods.desired : 0}</span>
            </div>
          }
        },
        {
          title: '内部 Endpoints',
          key: 'internalEndpoints',
          width: '15%',
          customRender: (text, record) => {
            // 显示内部端点
            if (!record.internalEndpoint || !record.internalEndpoint.ports || record.internalEndpoint.ports.length === 0) {
              return '-';
            }
            
            const endpoints = [];
            
            // 完全按照Kubernetes Dashboard的方式实现内部端点显示
            record.internalEndpoint.ports.forEach(port => {
              // 显示内部端口
              endpoints.push(h('div', { class: 'internal-endpoint' }, `${record.internalEndpoint.host}:${port.port} ${port.protocol}`));
              
              // 如果存在nodePort，则显示nodePort端口
              if (port.nodePort) {
                endpoints.push(h('div', { class: 'internal-endpoint' }, `${record.internalEndpoint.host}:${port.nodePort} ${port.protocol}`));
              }
            });
            
            return h('div', {}, endpoints);
          }
        },
        {
          title: '外部 Endpoints',
          key: 'externalEndpoints',
          width: '15%',
          customRender: (text, record) => {
            // 检查externalEndpoints是否为空数组
            const hasExternalEndpoints = record.externalEndpoints && record.externalEndpoints.length > 0;
            
            // 如果externalEndpoints不为空，显示外部端点
            if (hasExternalEndpoints) {
              const endpoints = [];
              
              record.externalEndpoints.forEach(endpoint => {
                if (endpoint.ports && endpoint.ports.length > 0) {
                  endpoint.ports.forEach(port => {
                    if (port.port) {
                      endpoints.push(h('div', {}, [
                        h('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.port}`,
                            target: '_blank',
                            rel: 'noopener noreferrer'
                          },
                          class: 'external-endpoint'
                        }, [
                          `${endpoint.host}:${port.port}`,
                          h('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                    
                    if (!port.port && port.nodePort) {
                      endpoints.push(h('div', {}, [
                        h('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.nodePort}`,
                            target: '_blank',
                            rel: 'noopener noreferrer'
                          },
                          class: 'external-endpoint'
                        }, [
                          `${endpoint.host}:${port.nodePort}`,
                          h('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                  });
                } else {
                  endpoints.push(h('div', {}, [
                    h('a', { 
                      attrs: { 
                        href: `http://${endpoint.host}`,
                        target: '_blank',
                        rel: 'noopener noreferrer'
                      },
                      class: 'external-endpoint'
                    }, [
                      endpoint.host,
                      h('i', { class: 'anticon anticon-link external-icon' })
                    ])
                  ]));
                }
              });
              
              return h('div', {}, endpoints);
            }
            
            return '-';
          }
        },
        {
          title: '创建时间',
          key: 'creationTime',
          width: '10%',
          sorter: true,
          className: 'normal-column-header', // 添加自定义类名
          customRender: (text, record) => {
            // 获取创建时间
            const timestamp = record.objectMeta?.creationTimestamp;
            if (!timestamp) return '-';
            
            // 格式化为 "x天前" 的形式
            const days = this.getDaysAgo(timestamp);
            return h('span', { style: 'white-space: nowrap;' }, `${days}天前`);
          }
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
          key: 'creationTime',
          width: '10%',
          sorter: true,
          className: 'normal-column-header', // 添加自定义类名
          customRender: (text, record) => {
            // 获取创建时间
            const timestamp = record.objectMeta?.creationTimestamp;
            if (!timestamp) return '-';
            
            // 格式化为 "x天前" 的形式
            const days = this.getDaysAgo(timestamp);
            return h('span', { style: 'white-space: nowrap;' }, `${days}天前`);
          }
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
          dataIndex: ['objectMeta', 'name'],
          key: 'name',
          width: '15%',
          customRender: (text, record) => {
            // 绿色状态点和名称一起显示
            return h('div', { style: { display: 'flex', alignItems: 'center' } }, [
              h('span', { 
                class: ['status-dot'], 
                style: { 
                  backgroundColor: '#4caf50', 
                  width: '8px', 
                  height: '8px', 
                  borderRadius: '50%', 
                  display: 'inline-block',
                  marginRight: '8px'
                } 
              }),
              h('span', { class: 'cell-content', title: text || '未知' }, text || '未知')
            ]);
          }
        },
        {
          title: '命名空间',
          dataIndex: ['objectMeta', 'namespace'],
          key: 'namespace',
          width: '10%',
          customRender: (text) => {
            return h('span', { class: 'cell-content', title: text || 'datasophon' }, text || 'datasophon');
          }
        },
        {
          title: '标签',
          key: 'labels',
          width: '15%',
          customRender: (text, record) => {
            if (!record.objectMeta?.labels || Object.keys(record.objectMeta.labels).length === 0) {
              return '-';
            }
            
            // 使用a-tag组件来模拟原始K8s Dashboard中的mat-chip组件
            return h('div', { class: 'labels-container' }, 
              Object.entries(record.objectMeta.labels).map(([key, value]) => {
                return h('a-tag', { 
                  props: { color: 'blue' },
                  class: 'label-chip',
                  key: key
                }, `${key}: ${value}`);
              })
            );
          }
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: '10%',
          customRender: (text) => {
            return h('span', { class: 'cell-content', title: text || 'NodePort' }, text || 'NodePort');
          }
        },
        {
          title: '集群 IP',
          dataIndex: 'clusterIP',
          key: 'clusterIP',
          width: '10%',
          customRender: (text) => {
            return h('span', { class: 'cell-content', title: text || '-' }, text || '-');
          }
        },
        {
          title: '内部 Endpoints',
          key: 'internalEndpoints',
          width: '15%',
          customRender: (text, record) => {
            // 显示内部端点
            if (!record.internalEndpoint || !record.internalEndpoint.ports || record.internalEndpoint.ports.length === 0) {
              return '-';
            }
            
            const endpoints = [];
            
            // 完全按照Kubernetes Dashboard的方式实现内部端点显示
            record.internalEndpoint.ports.forEach(port => {
              // 创建内部端口文本
              const internalPortText = `${record.internalEndpoint.host}:${port.port} ${port.protocol}`;
              endpoints.push(h('div', { 
                class: 'internal-endpoint',
                title: internalPortText
              }, internalPortText));
              
              // 如果存在nodePort，则显示nodePort端口
              if (port.nodePort) {
                const nodePortText = `${record.internalEndpoint.host}:${port.nodePort} ${port.protocol}`;
                endpoints.push(h('div', { 
                  class: 'internal-endpoint',
                  title: nodePortText
                }, nodePortText));
              }
            });
            
            return h('div', { style: { maxWidth: '100%', overflow: 'hidden' } }, endpoints);
          }
        },
        {
          title: '外部 Endpoints',
          key: 'externalEndpoints',
          width: '10%',
          customRender: (text, record) => {
            // 检查externalEndpoints是否为空数组
            const hasExternalEndpoints = record.externalEndpoints && record.externalEndpoints.length > 0;
            
            // 如果externalEndpoints不为空，显示外部端点
            if (hasExternalEndpoints) {
              const endpoints = [];
              
              record.externalEndpoints.forEach(endpoint => {
                if (endpoint.ports && endpoint.ports.length > 0) {
                  endpoint.ports.forEach(port => {
                    if (port.port) {
                      const portText = `${endpoint.host}:${port.port}`;
                      endpoints.push(h('div', {}, [
                        h('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.port}`,
                            target: '_blank',
                            rel: 'noopener noreferrer',
                            title: portText
                          },
                          class: 'external-endpoint'
                        }, [
                          h('span', { class: 'cell-content' }, portText),
                          h('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                    
                    if (!port.port && port.nodePort) {
                      const nodePortText = `${endpoint.host}:${port.nodePort}`;
                      endpoints.push(h('div', {}, [
                        h('a', { 
                          attrs: { 
                            href: `http://${endpoint.host}:${port.nodePort}`,
                            target: '_blank',
                            rel: 'noopener noreferrer',
                            title: nodePortText
                          },
                          class: 'external-endpoint'
                        }, [
                          h('span', { class: 'cell-content' }, nodePortText),
                          h('i', { class: 'anticon anticon-link external-icon' })
                        ])
                      ]));
                    }
                  });
                } else {
                  endpoints.push(h('div', {}, [
                    h('a', { 
                      attrs: { 
                        href: `http://${endpoint.host}`,
                        target: '_blank',
                        rel: 'noopener noreferrer',
                        title: endpoint.host
                      },
                      class: 'external-endpoint'
                    }, [
                      h('span', { class: 'cell-content' }, endpoint.host),
                      h('i', { class: 'anticon anticon-link external-icon' })
                    ])
                  ]));
                }
              });
              
              return h('div', { style: { maxWidth: '100%', overflow: 'hidden' } }, endpoints);
            }
            
            return '-';
          }
        },
        {
          title: '创建时间',
          key: 'createTime',
          dataIndex: ['objectMeta', 'creationTimestamp'],
          width: '15%',
          customRender: (text, record) => {
            // 获取创建时间
            const timestamp = record.objectMeta?.creationTimestamp;
            if (!timestamp) return '-';
            
            // 格式化为 "x天前" 的形式
            const days = this.getDaysAgo(timestamp);
            
            // 返回包含title属性的span，鼠标悬停时显示精确日期
            return h('span', { 
              class: 'format-time-cell', 
              style: 'white-space: nowrap;',
              title: this.formatTime(timestamp)
            }, `${days}天前`);
          }
        }
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
          key: 'creationTime',
          width: '10%',
          sorter: true,
          className: 'normal-column-header', // 添加自定义类名
          customRender: (text, record) => {
            // 获取创建时间
            const timestamp = record.objectMeta?.creationTimestamp;
            if (!timestamp) return '-';
            
            // 格式化为 "x天前" 的形式
            const days = this.getDaysAgo(timestamp);
            return h('span', { style: 'white-space: nowrap;' }, `${days}天前`);
          }
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
      metricsData: [], // 保存从API获取的指标数据
      serviceLoading: false,
      serviceErrors: [],
      serviceTotalItems: 0,
    };
  },
  methods: {
    async fetchK8sResources() {
      // 获取命名空间列表
      this.fetchNamespaces();
        
      // 根据当前选中的资源类型加载数据
      this.updateResourceData();
      
          // 初始化图表
          this.$nextTick(() => {
            this.initCharts();
          });
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
          // 确保获取命名空间列表数组
          this.namespaces = res.data && Array.isArray(res.data) ? res.data : (res.data && res.data.namespaces ? res.data.namespaces : []);
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
        // 使用serviceInstanceId作为serviceId参数
        const serviceId = this.serviceInstanceId || 40; // 使用默认值40
        
        const res = await this.$axiosGet(global.API.getK8sDeployments, {
          clusterId: this.clusterId,
          serviceId: serviceId,
          namespace: this.selectedNamespace === 'all' ? null : this.selectedNamespace
        });
        if (res.code === 200) {
          // 确保获取部署列表数组，并处理数据，确保每个部署对象都有必要的属性
          let deployList = res.data && res.data.deployments ? res.data.deployments : [];
          
          // 处理deployments数据，确保每个项都有必要的属性
          this.deployments = deployList.map(deploy => {
            // 如果deploy为null或undefined，返回一个空对象
            if (!deploy) return { objectMeta: {}, pods: {} };
            
            // 确保objectMeta存在
            if (!deploy.objectMeta) deploy.objectMeta = {};
            
            // 确保pods存在
            if (!deploy.pods) deploy.pods = {};
            
            return deploy;
          });
          
          console.log("处理后的deployments数据:", this.deployments);
          
          // 保存获取到的指标数据
          this.metricsData = res.data && res.data.cumulativeMetrics ? res.data.cumulativeMetrics : [];
          console.log("获取到的指标数据:", this.metricsData);
          
          // 初始化图表
          this.$nextTick(() => {
            this.initCharts();
          });
        } else {
          console.error('Failed to fetch deployments:', res.msg);
          this.deployments = [];
          this.metricsData = [];
        }
      } catch (error) {
        console.error('Error fetching deployments:', error);
        this.deployments = [];
        this.metricsData = [];
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
      if (!record || !record.objectMeta) {
        this.$message.warning('部署信息不完整，无法查看详情');
        return;
      }
      
      this.currentDeployment = {
        namespace: record.objectMeta.namespace || '',
        name: record.objectMeta.name || ''
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
        
        // 获取CPU和内存指标数据
        let cpuMetric = this.metricsData && this.metricsData.length > 0 
          ? this.metricsData.find(metric => metric.metricName === 'cpu/usage_rate') 
          : null;
        
        let memoryMetric = this.metricsData && this.metricsData.length > 0 
          ? this.metricsData.find(metric => metric.metricName === 'memory/usage') 
          : null;
        
        // 提取时间轴和数据点
        let xAxisData = [];
        let cpuData = [];
        let memoryData = [];
        
        // 处理CPU指标数据
        if (cpuMetric && cpuMetric.dataPoints && cpuMetric.dataPoints.length > 0) {
          // 排序数据点，确保时间顺序正确
          const sortedDataPoints = [...cpuMetric.dataPoints].sort((a, b) => a.x - b.x);
          
          // 提取x轴数据
          sortedDataPoints.forEach(point => {
            const date = new Date(point.x * 1000);
            const timeStr = `${date.getHours()}:${date.getMinutes() < 10 ? '0' : ''}${date.getMinutes()}`;
            xAxisData.push(timeStr);
            cpuData.push(point.y);
          });
        } else {
          // 如果没有数据，使用默认时间轴
          xAxisData = this.generateTimeAxis();
          cpuData = new Array(xAxisData.length).fill(0);
        }
        
        // 处理内存指标数据
        if (memoryMetric && memoryMetric.dataPoints && memoryMetric.dataPoints.length > 0) {
          const sortedDataPoints = [...memoryMetric.dataPoints].sort((a, b) => a.x - b.x);
          memoryData = sortedDataPoints.map(point => point.y);
        } else {
          memoryData = new Array(xAxisData.length).fill(0);
        }
        
        // 初始化CPU使用率图表
        if (this.$refs.cpuChart) {
          this.cpuChart = echarts.init(this.$refs.cpuChart, null, {
            renderer: 'canvas',
            useDirtyRect: true,
            devicePixelRatio: window.devicePixelRatio
          });
          const cpuOption = {
            title: {
              show: false
            },
            grid: {
              left: '8%',
              right: '2%',
              bottom: '10%',
              top: '5%',
              containLabel: false
            },
            tooltip: {
              trigger: 'axis',
              formatter: '{b}<br/>{a}: {c} cores'
            },
            xAxis: {
              type: 'category',
              data: xAxisData,
              axisLine: {
                lineStyle: {
                  color: '#E0E0E0'
                }
              },
              axisTick: {
                alignWithLabel: true,
                lineStyle: {
                  color: '#E0E0E0'
                }
              },
              axisLabel: {
                color: '#666',
                fontSize: 10
              },
              splitLine: {
                show: true,
                lineStyle: {
                  color: ['#f0f0f0'],
                  type: 'dashed'
                }
              }
            },
            yAxis: {
              type: 'value',
              name: '',
              nameLocation: 'end',
              nameGap: 15,
              nameTextStyle: {
                color: '#666',
                fontSize: 10,
                padding: [0, 0, 0, 10]
              },
              min: 0,
              max: 0.01, // 与官方一致，Y轴最大值固定为0.01
              axisLine: {
                show: false
              },
              axisTick: {
                show: false
              },
              axisLabel: {
                color: '#666',
                fontSize: 10,
                formatter: '{value}'
              },
              splitLine: {
                show: true,
                lineStyle: {
                  color: ['#f0f0f0'],
                  type: 'dashed'
                }
              }
            },
            series: [{
              name: 'CPU Usage',
              data: cpuData,
              type: 'line',
              smooth: true,
              symbol: 'none',
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: 'rgba(83, 231, 139, 0.8)' // 更接近官方的绿色
                  }, {
                    offset: 1, color: 'rgba(83, 231, 139, 0.1)'
                  }]
                }
              },
              itemStyle: {
                color: '#53e78b' // 更接近官方的绿色
              },
              lineStyle: {
                width: 2,
                color: '#53e78b' // 更接近官方的绿色
              }
            }]
          };
          this.cpuChart.setOption(cpuOption);
        }
        
        // 初始化内存使用率图表
        if (this.$refs.memoryChart) {
          this.memoryChart = echarts.init(this.$refs.memoryChart, null, {
            renderer: 'canvas',
            useDirtyRect: true,
            devicePixelRatio: window.devicePixelRatio
          });
          
          // 计算内存单位和转换
          const maxMemory = Math.max(...memoryData);
          const memoryInMi = maxMemory / (1024 * 1024);
          const yAxisMax = 20; // 固定为20 Mi，与官方一致
          
          const memoryOption = {
            title: {
              show: false
            },
            grid: {
              left: '8%',
              right: '2%',
              bottom: '10%',
              top: '5%',
              containLabel: false
            },
            tooltip: {
              trigger: 'axis',
              formatter: function(params) {
                const value = params[0].value / (1024 * 1024); // 转换为Mi
                return params[0].axisValue + '<br/>' + params[0].seriesName + ': ' + value.toFixed(2) + ' Mi';
              }
            },
            xAxis: {
              type: 'category',
              data: xAxisData,
              axisLine: {
                lineStyle: {
                  color: '#E0E0E0'
                }
              },
              axisTick: {
                alignWithLabel: true,
                lineStyle: {
                  color: '#E0E0E0'
                }
              },
              axisLabel: {
                color: '#666',
                fontSize: 10
              },
              splitLine: {
                show: true,
                lineStyle: {
                  color: ['#f0f0f0'],
                  type: 'dashed'
                }
              }
            },
            yAxis: {
              type: 'value',
              name: '',
              nameLocation: 'end',
              nameGap: 15,
              nameTextStyle: {
                color: '#666',
                fontSize: 10,
                padding: [0, 0, 0, 10]
              },
              min: 0,
              max: yAxisMax,
              axisLine: {
                show: false
              },
              axisTick: {
                show: false
              },
              axisLabel: {
                color: '#666',
                fontSize: 10,
                formatter: '{value} Mi'
              },
              splitLine: {
                show: true,
                lineStyle: {
                  color: ['#f0f0f0'],
                  type: 'dashed'
                }
              }
            },
            series: [{
              name: 'Memory Usage',
              data: memoryData.map(value => value), // 原始字节值
              type: 'line',
              smooth: true,
              symbol: 'none',
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: 'rgba(66, 133, 244, 0.9)' // 使用更亮的蓝色，增加不透明度
                  }, {
                    offset: 1, color: 'rgba(66, 133, 244, 0.2)'
                  }]
                }
              },
              itemStyle: {
                color: '#4285f4' // Google蓝
              },
              lineStyle: {
                width: 2,
                color: '#4285f4' // Google蓝
              }
            }]
          };
          this.memoryChart.setOption(memoryOption);
        }
        
        // 添加窗口大小变化监听，调整图表大小
        window.addEventListener('resize', this.resizeCharts, { passive: true });
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
    
    // 更新图表
    updateCharts() {
      // 如果没有图表数据，重新获取部署数据
      if (!this.metricsData || this.metricsData.length === 0) {
        this.fetchDeployments();
        return;
      }
      
      // 确保图表实例存在且未被销毁
      if (this.cpuChart && !this.cpuChart.isDisposed() && 
          this.memoryChart && !this.memoryChart.isDisposed()) {
            
        // 重新获取部署数据和指标数据
        this.fetchDeployments();
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
        this.serviceLoading = true;
        const res = await this.$axiosGet(global.API.getK8sServices, {
          clusterId: this.clusterId,
          namespace: 'datasophon' // 固定使用datasophon命名空间
        });
        
        if (res.code === 200 && res.data) {
          // 添加调试日志
          console.log('服务数据结构:', JSON.stringify(res.data.services[0]));
          console.log('创建时间字段:', res.data.services[0].objectMeta?.creationTimestamp);
          
          // 处理服务数据
          this.services = res.data.services || [];
          this.serviceTotalItems = res.data.listMeta?.totalItems || 0;
          
          // 返回的数据不需要特殊处理，直接使用
          // 如果有必要，可以在这里添加额外的数据处理逻辑
        } else {
          this.services = [];
          this.serviceTotalItems = 0;
          console.error('获取服务列表失败:', res.msg);
        }
      } catch (error) {
        console.error('获取服务列表失败:', error);
        this.$message.error('获取服务列表失败');
        this.services = [];
        this.serviceTotalItems = 0;
      } finally {
        this.serviceLoading = false;
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
    
    // 添加formatTime方法
    formatTime(time) {
      if (!time) return '-';
      return new Date(time).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    },
    
    // 添加相对时间格式化方法
    formatRelativeTime(time) {
      if (!time) return '-';
      
      const now = new Date();
      const date = new Date(time);
      const diff = Math.floor((now - date) / 1000); // 转换为秒
      
      if (diff < 60) {
        return '刚刚';
      } else if (diff < 3600) {
        return Math.floor(diff / 60) + '分钟前';
      } else if (diff < 86400) {
        return Math.floor(diff / 3600) + '小时前';
      } else if (diff < 2592000) {
        return Math.floor(diff / 86400) + '天前';
      } else if (diff < 31536000) {
        return Math.floor(diff / 2592000) + '个月前';
      } else {
        return Math.floor(diff / 31536000) + '年前';
      }
    },
    updateResourceData() {
      // 清除之前的数据
      this.loading = true;
      
      // 根据当前选中的资源类型加载数据
      switch (this.activeResource) {
        case 'deployment':
          this.fetchDeployments();
          break;
        case 'pod':
          this.fetchPods();
          break;
        case 'replicaset':
          this.fetchReplicaSets();
          break;
        case 'replicationcontroller':
          this.fetchReplicationControllers();
          break;
        case 'statefulset':
          this.fetchStatefulSets();
          break;
        case 'daemonset':
          this.fetchDaemonSets();
          break;
        case 'job':
          this.fetchJobs();
          break;
        case 'cronjob':
          this.fetchCronJobs();
          break;
        case 'service':
          this.fetchServices();
          break;
        case 'ingress':
          this.fetchIngresses();
          break;
        case 'configmap':
          this.fetchConfigMaps();
          break;
        case 'secret':
          this.fetchSecrets();
          break;
        case 'pv':
          this.fetchPersistentVolumes();
          break;
        case 'pvc':
          this.fetchPVCs();
          break;
        case 'storageclass':
          this.fetchStorageClasses();
          break;
        default:
          // 默认加载Deployments
          this.fetchDeployments();
      }
      
      this.loading = false;
    },
    getDaysAgo(timestamp) {
      if (!timestamp) return '-';
      
      const date = new Date(timestamp);
      const now = new Date();
      
      // 计算时间差（毫秒）
      const timeDiff = Math.abs(now - date);
      
      // 转换为天数
      const days = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
      
      return days;
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
        this.updateResourceData();
      }
    },
    selectedNamespace(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.updateResourceData();
      }
    }
  }
});
</script>

<style lang="less" scoped>
.k8s-config-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  
  // 页面头部样式
.page-header {
  display: flex;
  align-items: center;
    padding: 16px 24px;
    background-color: #fff;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
    margin-bottom: 16px;

.header-icon-wrapper {
  margin-right: 16px;

.kubernetes-logo {
        width: 40px;
        height: 40px;
        background-image: url('~@/assets/images/kubernetes-logo.svg');
        background-size: contain;
        background-repeat: no-repeat;
      }
}

.header-content {
  flex: 1;

.title {
        margin: 0;
        padding: 0;
        font-size: 18px;
  font-weight: 500;
        color: #333;
        line-height: 1.4;
}

.subtitle {
        margin: 4px 0 0;
        padding: 0;
        font-size: 13px;
        color: #666;
        line-height: 1.4;
      }
}

.namespace-selector {
      margin-left: 16px;
    }
  }
  
  // 整体仪表盘布局
.k8s-dashboard-layout {
  display: flex;
    flex: 1;
    min-height: calc(100vh - 185px);
    padding: 0 24px 16px;
    
    // 左侧导航样式
.sidebar-menu {
      width: 280px;
      min-width: 280px;
      margin-right: 16px;
      background-color: #fff;
      border-radius: 4px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);

.menu-group {
        padding: 12px 0;
        border-bottom: 1px solid #f0f0f0;
        
        &:last-child {
          border-bottom: none;
}

.group-title {
  padding: 8px 16px;
          color: #999;
          font-size: 12px;
          font-weight: 500;
  text-transform: uppercase;
          letter-spacing: 0.5px;
}

.menu-item {
  display: flex;
          justify-content: space-between;
  align-items: center;
          padding: 10px 16px;
          color: #333;
  font-size: 14px;
  cursor: pointer;
          transition: all 0.2s;
          
          &:hover {
            background-color: #f5f7fa;
}

          &.active {
            background-color: #e6f7ff;
            color: #1890ff;
            border-right: 3px solid #1890ff;
            
            .item-count {
              background-color: #1890ff;
              color: #fff;
            }
}

.item-text {
  flex: 1;
}

.item-count {
            display: inline-block;
            min-width: 24px;
            height: 24px;
            line-height: 24px;
            text-align: center;
            padding: 0 6px;
            border-radius: 12px;
            background-color: #f0f0f0;
            color: #666;
  font-size: 12px;
          }
        }
      }
}

    // 右侧内容区域样式
.content-area {
  flex: 1;
      
      .resource-list {
        height: 100%;

.resource-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 16px 0;
  margin-bottom: 16px;

          h3 {
  margin: 0;
            font-size: 18px;
            font-weight: 500;
            color: #333;
}
        }
      }
    }
  }
  
  // 仪表板卡片样式
  .k8s-dashboard-card {
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    margin-bottom: 16px;
    overflow: hidden;
    
    &.k8s-resource-card {
      .k8s-card-header {
  display: flex;
        justify-content: space-between;
  align-items: center;
        height: 48px;
        padding: 0 16px;
        background-color: #f7f7f7;
        border-bottom: 1px solid #eee;
        
        .k8s-card-title {
          font-size: 16px;
          font-weight: 500;
          color: #333;
}

        .k8s-card-actions {
  display: flex;
          gap: 12px;
          
          .k8s-action-icon {
            font-size: 16px;
            color: #999;
            cursor: pointer;
            
            &:hover {
              color: #1890ff;
            }
          }
        }
      }
      
      .k8s-card-content {
        padding: 0;
      }
    }
  }
}

// 图表相关样式
.k8s-dashboard-charts {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;

  .k8s-chart-card {
  flex: 1;
    min-width: 400px;
    height: 250px;
  border-radius: 4px;
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
    position: relative;

    .k8s-chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
      height: 48px;
      padding: 0 16px;
      background-color: #f7f7f7;
      border-bottom: 1px solid #e8e8e8;
      
      .k8s-chart-title {
  font-size: 14px;
  font-weight: 500;
        color: #333;
}

      .k8s-chart-actions {
        .k8s-action-icon {
          cursor: pointer;
          color: #999;
          transition: color 0.3s;
          
          &:hover {
            color: #1890ff;
          }
        }
      }
    }
    
    .k8s-chart-content {
      position: relative;
      height: calc(100% - 48px);
      padding: 10px 5px 10px 15px;
      
      .k8s-chart-y-label {
        position: absolute;
        left: -25px;
        top: 50%;
        transform: rotate(-90deg);
        transform-origin: center;
        font-size: 12px;
        color: #666;
        white-space: nowrap;
        z-index: 2;
        width: 80px;
        text-align: center;
}

.chart {
  width: 100%;
  height: 100%;
}
    }
  }
}

/* 响应式布局 */
@media screen and (max-width: 1200px) {
  .k8s-dashboard-container {
    .k8s-dashboard-charts {
      .k8s-chart-card {
        min-width: 300px;
      }
    }
  }
}

@media screen and (max-width: 768px) {
  .k8s-dashboard-container {
    .k8s-dashboard-charts {
      flex-direction: column;
      
      .k8s-chart-card {
        width: 100%;
      }
    }
  }
}

/* 表格通用样式 */
.k8s-table {
  .status-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: 8px;
    
    &.status-running {
      background-color: #52c41a;
    }
    
    &.status-warning {
      background-color: #faad14;
    }
    
    &.status-danger {
      background-color: #f5222d;
    }
    
    &.status-unknown {
      background-color: #d9d9d9;
    }
  }
  
  .name-cell, .image-cell, .pods-display {
    white-space: nowrap;
  overflow: hidden;
    text-overflow: ellipsis;
}

  .tag-list {
  display: flex;
    flex-wrap: wrap;
    
    .label-tag {
      margin: 2px;
    }
  }
  
  .action-buttons {
    white-space: nowrap;
    
    a {
      color: #1890ff;
      
      &:hover {
        color: #40a9ff;
      }
    }
  }
}

// 额外的表格样式修复
:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 500;
  color: #333;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background-color: #e6f7ff;
}

:deep(.ant-tag) {
  margin-right: 4px;
  margin-bottom: 4px;
  border-radius: 2px;
}

:deep(.ant-spin-container) {
  height: 100%;
}

:deep(.ant-empty-image) {
  margin-top: 32px;
}

:deep(.ant-table-placeholder) {
  height: 100%;
}

.k8s-dashboard {
  &-container {
    min-height: 600px;
    background-color: #fff;
    border-radius: 4px;
    padding: 16px;

    .tabs-container {
      margin-top: 16px;
}

    .service-table {
      margin-top: 16px;
      
      // 状态点样式
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
        margin-right: 8px;

        &.status-running {
  background-color: #52c41a;
}

        &.status-warning {
  background-color: #faad14;
}

        &.status-unknown {
          background-color: #d9d9d9;
        }
      }
      
      // 标签样式
      .labels-container {
  display: flex;
        flex-wrap: wrap;
        gap: 4px;
        
        .label-chip {
          margin-right: 4px;
          margin-bottom: 4px;
          max-width: 100%;
          height: auto;
          line-height: 1.5;
          white-space: normal;
          word-break: break-word;
        }
      }
      
      // 表格头部样式
      .ant-table-thead > tr > th {
        background-color: #f5f7fa;
  font-weight: 500;
        color: #262626;
}

      // 表格单元格样式
      .ant-table-tbody > tr > td {
        padding: 10px 16px;
        word-break: break-word;
      }
      
      // 创建时间列样式
      .ant-table-row td:last-child {
  white-space: nowrap;
      }
    }
  }
}

.service-detail-dialog {
  width: 80%;
  max-width: 900px;
}

// 端点样式
.internal-endpoint, .external-endpoint {
  padding: 2px 0;
  word-break: break-all;
}

.external-endpoint {
  display: flex;
  align-items: center;
  
  .external-icon {
    margin-left: 4px;
    font-size: 12px;
  }
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

// 表格样式优化
.ant-table {
  table-layout: fixed;
  
  .ant-table-tbody > tr > td {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    word-break: keep-all;
  }
  
  // 确保创建时间列正确显示
  .ant-table-row td:last-child {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 为单元格内容添加工具提示
.cell-content {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 100%;
}

// 修复表格在Safari和Firefox中的显示问题
.k8s-config-container {
  .ant-table-wrapper {
    overflow-x: auto;
    
    .ant-table {
      min-width: 1000px; // 确保表格内容有足够的显示空间
      
      // 修复表头标题竖向显示问题
      .ant-table-thead > tr > th {
        white-space: nowrap !important;
        text-align: left !important;
        min-width: 100px; // 确保每列有足够宽度显示标题
      }
      
      // 特别处理创建时间列的表头
      .ant-table-thead > tr > th:last-child {
        min-width: 100px;
        white-space: nowrap !important;
        text-align: left !important;
        writing-mode: horizontal-tb !important; // 强制水平文本
        transform: none !important; // 防止任何旋转
      }
    }
    
    .ant-table-thead > tr > th,
    .ant-table-tbody > tr > td {
      padding: 10px 8px;
      vertical-align: middle;
    }
    
    .ant-table-column-title {
      word-break: keep-all;
      white-space: nowrap;
      text-align: left !important;
      writing-mode: horizontal-tb !important; // 强制水平文本
      transform: none !important; // 防止任何旋转
      display: inline-block !important; // 确保标题按照预期显示
    }
  }
}

// 内部端点样式
.internal-endpoint {
  padding: 2px 0;
  word-break: keep-all;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

// 外部端点样式
.external-endpoint {
  display: flex;
  align-items: center;
  padding: 2px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  
  .external-icon {
    margin-left: 4px;
    font-size: 12px;
    flex-shrink: 0;
  }
}

.normal-column-header {
  .ant-table-column-title {
    writing-mode: horizontal-tb !important;
    transform: none !important;
    white-space: nowrap !important;
    min-width: 90px !important;
    display: inline-block !important;
  }
}

// 修改全局表头样式
.k8s-config-container {
  .ant-table-column-has-sorters {
    .ant-table-column-title {
      writing-mode: horizontal-tb !important;
      transform: none !important;
      white-space: nowrap !important;
    }
  }
}

/* 确保所有表头正常水平显示，不会竖直旋转 */
:deep(.normal-column-header) {
  .ant-table-column-title {
    display: inline-block !important;
    white-space: nowrap !important;
    overflow: visible !important;
    writing-mode: horizontal-tb !important;
    min-width: 90px !important;
    transform: none !important;
  }
}

/* 修复表格内容样式 */
:deep(.ant-table-tbody) {
  td {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

/* 全局修复表头标题垂直显示问题 */
:deep(.ant-table-thead > tr > th) {
  white-space: nowrap !important;
  text-align: left !important;
  
  .ant-table-column-title {
    display: inline-block !important;
    white-space: nowrap !important;
    writing-mode: horizontal-tb !important;
    transform: none !important;
    word-break: keep-all !important;
    min-width: auto !important;
    width: auto !important;
    max-width: 100% !important;
  }
}

:deep(.ant-table-column-has-sorters) {
  .ant-table-column-sorter {
    display: inline-flex !important;
    align-items: center !important;
    vertical-align: middle !important;
    margin-left: 4px !important;
  }
}

/* 特别针对"创建时间"列的表头 */
:deep(.ant-table-thead > tr > th.normal-column-header) {
  .ant-table-column-title {
    display: inline-block !important;
    white-space: nowrap !important;
    writing-mode: horizontal-tb !important;
    transform: none !important;
    word-break: keep-all !important;
  }
}

/* 修复表格内容溢出问题 */
:deep(.ant-table-body) {
  overflow-x: auto !important;
}

:deep(.ant-table-tbody > tr > td) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: keep-all;
}
</style>