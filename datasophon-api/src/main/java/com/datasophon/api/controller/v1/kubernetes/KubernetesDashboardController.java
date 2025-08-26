/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.controller.v1.kubernetes;

import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.converter.K8sResourceConverter;
import com.datasophon.api.converter.KubernetesResourceVOConverter;
import com.datasophon.api.service.KubernetesDashboardService;
import com.datasophon.common.dto.K8sNamespaceDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.dto.KubernetesResourceDTO;
import com.datasophon.common.dto.K8sResourceStatsDTO;
import com.datasophon.common.vo.K8sNamespaceVO;
import com.datasophon.common.vo.K8sResourceStatsVO;
import com.datasophon.common.vo.KubernetesResourceVO;
import com.datasophon.common.vo.PageVO;
import com.datasophon.api.dto.Result;
import java.util.Map;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Kubernetes仪表盘控制器
 * 提供Kubernetes资源查询接口
 */
@ApiVersion(path = "kubernetes/dashboard")
@Slf4j
public class KubernetesDashboardController {

    @Autowired
    private KubernetesDashboardService kubernetesDashboardService;

    private final K8sResourceConverter k8sResourceConverter = K8sResourceConverter.INSTANCE;

    @Autowired
    private KubernetesResourceVOConverter kubernetesResourceVOConverter;

    /**
     * 获取Kubernetes命名空间列表
     */
    @GetMapping("/namespaces")
    public Result<List<K8sNamespaceVO>> getNamespaces(@ClusterId Long clusterId) {
        try {
            if (clusterId == null) {
                return Result.error("集群ID不能为空");
            }

            List<K8sNamespaceDTO> namespaceDTOs = kubernetesDashboardService.getNamespaces(clusterId);
            List<K8sNamespaceVO> namespaceVOs = k8sResourceConverter.namespaceListToVoList(namespaceDTOs);
            return Result.success(namespaceVOs);
        } catch (Exception e) {
            log.error("获取Kubernetes命名空间列表失败: {}", e.getMessage(), e);
            return Result.error("获取命名空间列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取Deployments列表
     */
    @RequestMapping("/deployments")
    public Result<PageVO<KubernetesResourceVO>> getDeployments(
            @ClusterId Long clusterId,
            @RequestParam(value = "serviceId", required = false) Long serviceId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        log.info("获取Deployments列表请求：clusterId={}, serviceId={}, namespace={}, pageNum={}, pageSize={}",
                clusterId, serviceId, namespace, pageNum, pageSize);

        try {
            PageResult<KubernetesResourceDTO> deploymentDTOs = kubernetesDashboardService.getDeployments(clusterId,
                    serviceId, namespace, pageNum, pageSize);

            // DTO转换为VO
            List<KubernetesResourceVO> deploymentVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(deploymentDTOs.getRecords());

            // 封装为分页结果
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(deploymentDTOs, deploymentVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取Deployments列表失败: {}", e.getMessage(), e);
            return Result.error("获取Deployments列表失败: " + e.getMessage());
        }

    }

    /**
     * 获取Pods列表
     */
    @GetMapping("/pods")
    public Result<PageVO<KubernetesResourceVO>> getPodsInfo(@ClusterId Long clusterId,
            @RequestParam(name = "serviceId", required = false) Long serviceId,
            @RequestParam(name = "namespace", required = false) String namespace,
            @RequestParam(name = "searchTerm", required = false) String searchTerm,
            @RequestParam(name = "statusFilter", required = false) String statusFilter,
            @RequestParam(name = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            log.info("获取Pods列表请求：clusterId={}, serviceId={}, namespace={}, searchTerm={}, statusFilter={}, pageNum={}, pageSize={}",
                    clusterId, serviceId, namespace, searchTerm, statusFilter, pageNum, pageSize);
            
            PageResult<KubernetesResourceDTO> podDTOs = kubernetesDashboardService.getPods(clusterId, serviceId,
                    namespace, searchTerm, statusFilter, pageNum, pageSize);

            // DTO转换为VO
            List<KubernetesResourceVO> podVOs = kubernetesResourceVOConverter.dtoListToVoList(podDTOs.getRecords());

            // 封装为分页结果
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(podDTOs, podVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取Pods列表失败", e);
            return Result.error("获取Pods列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取Services列表
     */
    @RequestMapping("/services")
    public Result<PageVO<KubernetesResourceVO>> getServices(
            @ClusterId Long clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> serviceDTOs = kubernetesDashboardService.getServices(clusterId, namespace,
                    pageNum, pageSize);
            List<KubernetesResourceVO> serviceVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(serviceDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(serviceDTOs, serviceVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取Services列表失败: {}", e.getMessage(), e);
            return Result.error("获取Services列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取ConfigMaps列表
     */
    @RequestMapping("/configmaps")
    public Result<PageVO<KubernetesResourceVO>> getConfigMaps(
            @ClusterId Long clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> configMapDTOs = kubernetesDashboardService.getConfigMaps(clusterId,
                    namespace, pageNum, pageSize);
            List<KubernetesResourceVO> configMapVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(configMapDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(configMapDTOs, configMapVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取ConfigMaps列表失败: {}", e.getMessage(), e);
            return Result.error("获取ConfigMaps列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取Secrets列表
     */
    @RequestMapping("/secrets")
    public Result<PageVO<KubernetesResourceVO>> getSecrets(
            @ClusterId Long clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> secretDTOs = kubernetesDashboardService.getSecrets(clusterId, namespace,
                    pageNum, pageSize);
            List<KubernetesResourceVO> secretVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(secretDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(secretDTOs, secretVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取Secrets列表失败: {}", e.getMessage(), e);
            return Result.error("获取Secrets列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取PersistentVolumes列表
     */
    @RequestMapping("/persistentvolumes")
    public Result<PageVO<KubernetesResourceVO>> getPersistentVolumes(
            @ClusterId Long clusterId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        try {
            PageResult<KubernetesResourceDTO> pvDTOs = kubernetesDashboardService.getPersistentVolumes(clusterId,
                    pageNum, pageSize);
            List<KubernetesResourceVO> pvVOs = kubernetesResourceVOConverter.dtoListToVoList(pvDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(pvDTOs, pvVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取PersistentVolumes列表失败: {}", e.getMessage(), e);
            return Result.error("获取PersistentVolumes列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取PersistentVolumeClaims列表
     */
    @RequestMapping("/pvcs")
    public Result<PageVO<KubernetesResourceVO>> getPersistentVolumeClaims(
            @ClusterId Long clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        // 直接调用支持分页的服务方法
        try {
            PageResult<KubernetesResourceDTO> pvcDTOs = kubernetesDashboardService.getPersistentVolumeClaims(clusterId,
                    namespace, pageNum, pageSize);
            List<KubernetesResourceVO> pvcVOs = kubernetesResourceVOConverter.dtoListToVoList(pvcDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(pvcDTOs, pvcVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取PersistentVolumeClaims列表失败: {}", e.getMessage(), e);
            return Result.error("获取PersistentVolumeClaims列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取StorageClasses列表
     */
    @RequestMapping("/storageclasses")
    public Result<PageVO<KubernetesResourceVO>> getStorageClasses(
            @ClusterId Long clusterId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        log.info("获取StorageClasses列表请求：clusterId={}, pageNum={}, pageSize={}",
                clusterId, pageNum, pageSize);

        // 直接调用支持分页的服务方法
        try {
            PageResult<KubernetesResourceDTO> storageDTOs = kubernetesDashboardService.getStorageClasses(clusterId,
                    pageNum, pageSize);
            List<KubernetesResourceVO> storageVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(storageDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(storageDTOs, storageVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取StorageClasses列表失败: {}", e.getMessage(), e);
            return Result.error("获取StorageClasses列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取Ingresses列表
     */
    @RequestMapping("/ingresses")
    public Result<PageVO<KubernetesResourceVO>> getIngresses(
            @ClusterId Long clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> ingressDTOs = kubernetesDashboardService.getIngresses(clusterId,
                    namespace, pageNum, pageSize);
            List<KubernetesResourceVO> ingressVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(ingressDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(ingressDTOs, ingressVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取Ingresses列表失败: {}", e.getMessage(), e);
            return Result.error("获取Ingresses列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取IngressClasses列表
     */
    @RequestMapping("/ingressclasses")
    public Result<PageVO<KubernetesResourceVO>> getIngressClasses(
            @ClusterId Long clusterId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> ingressClassDTOs = kubernetesDashboardService.getIngressClasses(clusterId,
                    pageNum, pageSize);
            List<KubernetesResourceVO> ingressClassVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(ingressClassDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(ingressClassDTOs, ingressClassVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取IngressClasses列表失败: {}", e.getMessage(), e);
            return Result.error("获取IngressClasses列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取DaemonSets列表
     */
    @RequestMapping("/daemonsets")
    public Result<PageVO<KubernetesResourceVO>> getDaemonSets(
            @ClusterId Long clusterId,
            @RequestParam(value = "serviceId", required = false) Long serviceId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> daemonSetDTOs = kubernetesDashboardService.getDaemonSets(clusterId,
                    serviceId, namespace, pageNum, pageSize);
            List<KubernetesResourceVO> daemonSetVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(daemonSetDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(daemonSetDTOs, daemonSetVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取DaemonSets列表失败: {}", e.getMessage(), e);
            return Result.error("获取DaemonSets列表失败: " + e.getMessage());
        }

    }

    /**
     * 获取StatefulSets列表
     */
    @RequestMapping("/statefulsets")
    public Result<PageVO<KubernetesResourceVO>> getStatefulSets(
            @ClusterId Long clusterId,
            @RequestParam(value = "serviceId", required = false) Long serviceId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> statefulSetDTOs = kubernetesDashboardService.getStatefulSets(clusterId,
                    serviceId, namespace, pageNum, pageSize);
            List<KubernetesResourceVO> statefulSetVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(statefulSetDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(statefulSetDTOs, statefulSetVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取StatefulSets列表失败: {}", e.getMessage(), e);
            return Result.error("获取StatefulSets列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取ReplicaSets列表
     */
    @RequestMapping("/replicasets")
    public Result<PageVO<KubernetesResourceVO>> getReplicaSets(
            @ClusterId Long clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> replicaSetDTOs = kubernetesDashboardService.getReplicaSets(clusterId,
                    namespace, pageNum, pageSize);
            List<KubernetesResourceVO> replicaSetVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(replicaSetDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(replicaSetDTOs, replicaSetVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取ReplicaSets列表失败: {}", e.getMessage(), e);
            return Result.error("获取ReplicaSets列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取ReplicationControllers列表（带分页）
     */
    @RequestMapping("/replicationcontrollers")
    public Result<PageVO<KubernetesResourceVO>> getReplicationControllers(
            @ClusterId Long clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> rcDTOs = kubernetesDashboardService.getReplicationControllers(clusterId,
                    namespace, pageNum, pageSize);
            List<KubernetesResourceVO> rcVOs = kubernetesResourceVOConverter.dtoListToVoList(rcDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(rcDTOs, rcVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取ReplicationControllers列表失败: {}", e.getMessage(), e);
            return Result.error("获取ReplicationControllers列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取Jobs列表
     */
    @RequestMapping("/jobs")
    public Result<PageVO<KubernetesResourceVO>> getJobs(
            @ClusterId Long clusterId,
            @RequestParam(value = "serviceId", required = false) Long serviceId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> jobDTOs = kubernetesDashboardService.getJobs(clusterId, namespace,
                    pageNum, pageSize);
            List<KubernetesResourceVO> jobVOs = kubernetesResourceVOConverter.dtoListToVoList(jobDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(jobDTOs, jobVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取Jobs列表失败: {}", e.getMessage(), e);
            return Result.error("获取Jobs列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取CronJobs列表
     */
    @RequestMapping("/cronjobs")
    public Result<PageVO<KubernetesResourceVO>> getCronJobs(
            @ClusterId Long clusterId,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            PageResult<KubernetesResourceDTO> cronJobDTOs = kubernetesDashboardService.getCronJobs(clusterId, namespace,
                    pageNum, pageSize);
            List<KubernetesResourceVO> cronJobVOs = kubernetesResourceVOConverter
                    .dtoListToVoList(cronJobDTOs.getRecords());
            PageVO<KubernetesResourceVO> pageVO = PageVO.from(cronJobDTOs, cronJobVOs);
            return Result.success(pageVO);
        } catch (Exception e) {
            log.error("获取CronJobs列表失败: {}", e.getMessage(), e);
            return Result.error("获取CronJobs列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取Deployment详情
     */
    @RequestMapping("/deployment/detail")
    public Result<KubernetesResourceVO> getDeploymentDetail(
            @ClusterId Long clusterId,
            @RequestParam("namespace") String namespace,
            @RequestParam("name") String name) {
        try {
            KubernetesResourceDTO deploymentDTO = kubernetesDashboardService.getDeploymentDetail(clusterId, namespace,
                    name);
            KubernetesResourceVO deploymentVO = kubernetesResourceVOConverter.dtoToVo(deploymentDTO);
            return Result.success(deploymentVO);
        } catch (Exception e) {
            log.error("获取Deployment详情失败: {}", e.getMessage(), e);
            return Result.error("获取Deployment详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取Deployment相关事件
     */
    @RequestMapping("/deployment/events")
    public Result<List<Map<String, Object>>> getDeploymentEvents(
            @ClusterId Long clusterId,
            @RequestParam("namespace") String namespace,
            @RequestParam("kind") String kind,
            @RequestParam("name") String name) {
        try {
            List<Map<String, Object>> events = kubernetesDashboardService.getResourceEvents(clusterId, namespace, kind,
                    name);
            return Result.success(events);
        } catch (Exception e) {
            log.error("获取资源事件失败: {}", e.getMessage(), e);
            return Result.error("获取资源事件失败: " + e.getMessage());
        }
    }

    /**
     * 获取资源相关事件
     */
    @RequestMapping(value = "/resource/events", method = RequestMethod.GET)
    public Result<List<Map<String, Object>>> getResourceEvents(@ClusterId Long clusterId,
            @RequestParam String namespace,
            @RequestParam String kind,
            @RequestParam String name) {
        try {
            List<Map<String, Object>> events = kubernetesDashboardService.getResourceEvents(clusterId, namespace, kind,
                    name);
            return Result.success(events);
        } catch (Exception e) {
            log.error("获取资源事件失败: {}", e.getMessage(), e);
            return Result.error("获取资源事件失败: " + e.getMessage());
        }
    }

    /**
     * 获取资源统计数据
     * 只统计各类资源的数量，不返回资源列表以提高性能
     */
    @RequestMapping("/resource-stats")
    public Result<K8sResourceStatsVO> getResourceStats(
            @ClusterId Long clusterId,
            @RequestParam(value = "serviceId", required = false) Long serviceId,
            @RequestParam(value = "namespace", required = false) String namespace) {
        try {
            // 添加调试日志
            log.info("收到resource-stats请求：clusterId={}, serviceId={}, namespace={}", clusterId, serviceId, namespace);

            // 确保clusterId不为空
            if (clusterId == null) {
                log.warn("收到resource-stats请求但缺少clusterId参数");
                return Result.error("缺少clusterId参数");
            }

            // 调用Service方法，获取统计数据
            K8sResourceStatsDTO statsDTO = kubernetesDashboardService.getResourceStats(clusterId, serviceId, namespace);
            
            // DTO转换为VO
            K8sResourceStatsVO statsVO = k8sResourceConverter.statsToVo(statsDTO);

            // 日志记录返回结果
            log.info("resource-stats接口返回统计数据: pods={}, services={}, deployments={}", 
                statsVO.getPodCount(), statsVO.getServiceCount(), statsVO.getDeploymentCount());

            return Result.success(statsVO);
        } catch (Exception e) {
            log.error("获取Kubernetes资源统计失败", e);
            return Result.error("获取Kubernetes资源统计失败: " + e.getMessage());
        }
    }

}
