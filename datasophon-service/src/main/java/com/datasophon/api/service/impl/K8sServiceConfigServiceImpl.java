package com.datasophon.api.service.impl;

import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.K8sServiceConfigService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class K8sServiceConfigServiceImpl implements K8sServiceConfigService {

    @Autowired
    private ClusterInfoServiceImpl clusterInfoService;

    @Autowired
    private FrameServiceRoleService frameServiceRoleService;

    private static final String LABEL_SELECTOR_FORMAT = "app=%s-%s";
    private static final String NO_ROLES_MSG = "No roles found for service: ";
    private static final String INVALID_PARAMS_MSG = "参数错误：必须提供集群ID和服务名称";
    private static final String NO_KUBECONFIG_MSG = "集群配置信息不存在";
    private static final String NO_CONFIGMAPS_MSG = "未找到匹配的配置项";

    @Override
    public Result getK8sConfigMaps(Integer clusterId, String serviceName) {
        // 参数校验
        if (!validateParams(clusterId, serviceName)) {
            return Result.error(INVALID_PARAMS_MSG);
        }

        // 获取kubeconfig
        String kubeConfig = getKubeConfig(clusterId);
        if (kubeConfig == null) {
            return Result.error(NO_KUBECONFIG_MSG);
        }

        // 获取服务角色
        Result serviceRoleResult = getServiceRoles(clusterId, serviceName);
        if (!serviceRoleResult.isSuccess()) {
            return serviceRoleResult;
        }

        // 转换角色列表
        List<FrameServiceRoleEntity> roleList = convertToRoleList(serviceRoleResult.getData());
        if (roleList.isEmpty()) {
            return Result.error(NO_ROLES_MSG + serviceName);
        }

        // 查询ConfigMap
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            List<Map<String, Object>> configMapDataList = queryConfigMapsForRoles(client, roleList, serviceName);
            return Result.success(configMapDataList);
        } catch (Exception e) {
            log.error("K8s连接异常", e);
            return Result.error("集群连接异常");
        }
    }


    public Result getK8sServices(Integer clusterId, String serviceName) {
        // 参数校验
        if (!validateParams(clusterId, serviceName)) {
            return Result.error(INVALID_PARAMS_MSG);
        }

        // 获取kubeconfig
        String kubeConfig = getKubeConfig(clusterId);
        if (kubeConfig == null) {
            return Result.error(NO_KUBECONFIG_MSG);
        }

        // 获取服务角色
        Result serviceRoleResult = getServiceRoles(clusterId, serviceName);
        if (!serviceRoleResult.isSuccess()) {
            return serviceRoleResult;
        }

        // 转换角色列表
        List<FrameServiceRoleEntity> roleList = convertToRoleList(serviceRoleResult.getData());
        if (roleList.isEmpty()) {
            return Result.error(NO_ROLES_MSG + serviceName);
        }

        // 查询Service
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            List<Map<String, Object>> serviceDataList = queryServicesForRoles(client, roleList, serviceName);
            return Result.success(serviceDataList);
        } catch (Exception e) {
            log.error("K8s连接异常", e);
            return Result.error("集群连接异常");
        }
    }

    private boolean validateParams(Integer clusterId, String serviceName) {
        return clusterId != null && StringUtils.isNotBlank(serviceName);
    }

    private String getKubeConfig(Integer clusterId) {
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(clusterId);
        if (StringUtils.isBlank(kubeConfig)) {
            log.error("无法获取集群配置，clusterId: {}", clusterId);
            return null;
        }
        return kubeConfig;
    }

    private Result getServiceRoles(Integer clusterId, String serviceName) {
        try {
            return frameServiceRoleService.getServiceRoleByServiceName(clusterId, serviceName);
        } catch (Exception e) {
            log.error("查询服务角色失败，clusterId: {}, service: {}", clusterId, serviceName, e);
            return Result.error("服务角色查询异常");
        }
    }

    private List<FrameServiceRoleEntity> convertToRoleList(Object data) {
        if (!(data instanceof List)) {
            return Collections.emptyList();
        }
        return ((List<?>) data).stream()
                .filter(FrameServiceRoleEntity.class::isInstance)
                .map(FrameServiceRoleEntity.class::cast)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> queryConfigMapsForRoles(KubernetesClient client, List<FrameServiceRoleEntity> roles, String serviceName) {
        List<Map<String, Object>> configMapDataList = new ArrayList<>();
        for (FrameServiceRoleEntity role : roles) {
            try {
                String selector = buildLabelSelector(serviceName, role.getServiceRoleName());
                List<Map<String, Object>> items = queryConfigMaps(client, selector);
                if (!items.isEmpty()) {
                    configMapDataList.addAll(items);
                }
            } catch (KubernetesClientException e) {
                log.error("查询ConfigMap失败，role: {}", role.getServiceRoleName(), e);
            }
        }
        return configMapDataList;
    }

    private String buildLabelSelector(String serviceName, String roleName) {
        return String.format(LABEL_SELECTOR_FORMAT,
                serviceName.toLowerCase(),
                roleName.toLowerCase().replaceAll("[^a-z0-9-]", ""));
    }

    private List<Map<String, Object>> queryConfigMaps(KubernetesClient client, String labelSelector) {
        List<ConfigMap> items = client.configMaps()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withLabelSelector(labelSelector)
                .list()
                .getItems();

        List<Map<String, Object>>  collect = items.stream()
                .map(configMap -> {
                    Map<String, Object> info = new HashMap<>(4);
                    info.put("name", configMap.getMetadata().getName());
                    // 将 labels 转换为 key : value 的格式
                    Map<String, String> labels = configMap.getMetadata().getLabels();
                    String formattedLabels = labels.entrySet().stream()
                            .map(entry -> entry.getKey() + " : " + entry.getValue())
                            .collect(Collectors.joining("  "));
                    info.put("labels", formattedLabels.isEmpty() ? "-" : formattedLabels);
                    info.put("data", configMap.getData());
                    info.put("time", configMap.getMetadata().getCreationTimestamp());
                    return info;
                })
                .collect(Collectors.toList());
        return collect;

    }

    private List<Map<String, Object>> queryServicesForRoles(KubernetesClient client, List<FrameServiceRoleEntity> roles, String serviceName) {
        List<Map<String, Object>> serviceDataList = new ArrayList<>();
        for (FrameServiceRoleEntity role : roles) {
            try {
                String selector = buildLabelSelector(serviceName, role.getServiceRoleName());
                List<Map<String, Object>> items = queryServices(client, selector);
                if (!items.isEmpty()) {
                    serviceDataList.addAll(items);
                }
            } catch (KubernetesClientException e) {
                log.error("查询Service失败，role: {}", role.getServiceRoleName(), e);
            }
        }
        return serviceDataList;
    }

    private List<Map<String, Object>> queryServices(KubernetesClient client, String labelSelector) {
        List<io.fabric8.kubernetes.api.model.Service> items = client.services()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withLabelSelector(labelSelector)
                .list()
                .getItems();

        return items.stream()
                .map(service -> {
                    Map<String, Object> info = new HashMap<>(4);
                    info.put("name", service.getMetadata().getName());
                    // 将 labels 转换为 key : value 的格式
                    Map<String, String> labels = service.getMetadata().getLabels();
                    String formattedLabels = labels.entrySet().stream()
                            .map(entry -> entry.getKey() + " : " + entry.getValue())
                            .collect(Collectors.joining("  "));
                    info.put("labels", formattedLabels.isEmpty() ? "-" : formattedLabels);
                    info.put("type", service.getSpec().getType());
                    info.put("clusterIP", service.getSpec().getClusterIP());
                    info.put("time", service.getMetadata().getCreationTimestamp());
                    return info;
                })
                .collect(Collectors.toList());
    }


    @Override
    public Result getK8sConfigMapDetail(Integer clusterId,String name) {
        // TODO: 实现获取ConfigMap详情的逻辑
        return Result.success();
    }

    @Override
    public Result updateK8sConfigMap(Integer clusterId,String name, String content) {
        // TODO: 实现更新ConfigMap的逻辑
        return Result.success();
    }


    @Override
    public Result getK8sServiceDetail(Integer clusterId,String name) {
        // TODO: 实现获取Service详情的逻辑
        return Result.success();
    }

    @Override
    public Result updateK8sService(Integer clusterId,String name, String content) {
        // TODO: 实现更新Service的逻辑
        return Result.success();
    }


    @Override
    public Result getK8sPvcs(Integer clusterId, String serviceName) {
        // 参数校验
        if (!validateParams(clusterId, serviceName)) {
            return Result.error(INVALID_PARAMS_MSG);
        }

        // 获取kubeconfig
        String kubeConfig = getKubeConfig(clusterId);
        if (kubeConfig == null) {
            return Result.error(NO_KUBECONFIG_MSG);
        }

        // 获取服务角色
        Result serviceRoleResult = getServiceRoles(clusterId, serviceName);
        if (!serviceRoleResult.isSuccess()) {
            return serviceRoleResult;
        }

        // 转换角色列表
        List<FrameServiceRoleEntity> roleList = convertToRoleList(serviceRoleResult.getData());
        if (roleList.isEmpty()) {
            return Result.error(NO_ROLES_MSG + serviceName);
        }

        // 查询PVC
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            List<Map<String, Object>> pvcDataList = queryPvcsForRoles(client, roleList, serviceName);
            return Result.success(pvcDataList);
        } catch (Exception e) {
            log.error("K8s连接异常", e);
            return Result.error("集群连接异常");
        }
    }

    private List<Map<String, Object>> queryPvcsForRoles(KubernetesClient client, List<FrameServiceRoleEntity> roles, String serviceName) {
        List<Map<String, Object>> pvcDataList = new ArrayList<>();
        for (FrameServiceRoleEntity role : roles) {
            try {
                String selector = buildLabelSelector(serviceName, role.getServiceRoleName());
                List<Map<String, Object>> items = queryPvcs(client, selector);
                if (!items.isEmpty()) {
                    pvcDataList.addAll(items);
                }
            } catch (KubernetesClientException e) {
                log.error("查询PVC失败，role: {}", role.getServiceRoleName(), e);
            }
        }
        return pvcDataList;
    }

    private List<Map<String, Object>> queryPvcs(KubernetesClient client, String labelSelector) {
        List<PersistentVolumeClaim> items = client.persistentVolumeClaims()
                .inNamespace(Constant.K8S_NAMESPACE)
                .withLabelSelector(labelSelector)
                .list()
                .getItems();

        return items.stream()
                .map(pvc -> {
                    Map<String, Object> info = new HashMap<>(6);
                    info.put("name", pvc.getMetadata().getName());
                    Map<String, String> labels = pvc.getMetadata().getLabels();
                    String formattedLabels = labels.entrySet().stream()
                            .map(entry -> entry.getKey() + " : " + entry.getValue())
                            .collect(Collectors.joining("  "));
                    info.put("labels", formattedLabels.isEmpty() ? "-" : formattedLabels);
                    info.put("status", pvc.getStatus().getPhase());
                    info.put("storageClass", pvc.getSpec().getStorageClassName());
                    Map<String, Quantity> capacity = pvc.getStatus().getCapacity();
                    // 提取容量值并格式化
                    String capacityValue = capacity != null && capacity.containsKey("storage") 
                            ? capacity.get("storage").getAmount() + capacity.get("storage").getFormat() 
                            : "-";
                    info.put("capacity", capacityValue);
                    info.put("time", pvc.getMetadata().getCreationTimestamp());
                    return info;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result getK8sPvcDetail(Integer clusterId, String name) {
        // 参数校验
        if (clusterId == null || StringUtils.isBlank(name)) {
            return Result.error("参数错误：必须提供集群ID和PVC名称");
        }

        // 获取kubeconfig
        String kubeConfig = getKubeConfig(clusterId);
        if (kubeConfig == null) {
            return Result.error(NO_KUBECONFIG_MSG);
        }

        // 查询PVC详情
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            PersistentVolumeClaim pvc = client.persistentVolumeClaims()
                    .inNamespace(Constant.K8S_NAMESPACE)
                    .withName(name)
                    .get();

            if (pvc == null) {
                return Result.error("未找到匹配的PVC");
            }

            Map<String, Object> pvcInfo = new HashMap<>();
            pvcInfo.put("name", pvc.getMetadata().getName());
            pvcInfo.put("labels", pvc.getMetadata().getLabels());
            pvcInfo.put("status", pvc.getStatus().getPhase());
            pvcInfo.put("storageClass", pvc.getSpec().getStorageClassName());
            pvcInfo.put("capacity", pvc.getStatus().getCapacity());
            pvcInfo.put("time", pvc.getMetadata().getCreationTimestamp());
            pvcInfo.put("accessModes", pvc.getSpec().getAccessModes());
            pvcInfo.put("volumeName", pvc.getSpec().getVolumeName());

            return Result.success(pvcInfo);
        } catch (Exception e) {
            log.error("K8s连接异常", e);
            return Result.error("集群连接异常");
        }
    }

    @Override
    public Result updateK8sPvc(Integer clusterId, String name, String content) {
        // 参数校验
        if (clusterId == null || StringUtils.isBlank(name) || StringUtils.isBlank(content)) {
            return Result.error("参数错误：必须提供集群ID、PVC名称和更新内容");
        }

        // 获取kubeconfig
        String kubeConfig = getKubeConfig(clusterId);
        if (kubeConfig == null) {
            return Result.error(NO_KUBECONFIG_MSG);
        }

        // 更新PVC
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfig)) {
            PersistentVolumeClaim pvc = client.persistentVolumeClaims()
                    .inNamespace(Constant.K8S_NAMESPACE)
                    .withName(name)
                    .get();

            if (pvc == null) {
                return Result.error("未找到匹配的PVC");
            }

            // 解析并更新PVC内容
            Map<String, Object> updateContent = parsePvcContent(content);
            if (updateContent.containsKey("labels")) {
                pvc.getMetadata().setLabels((Map<String, String>) updateContent.get("labels"));
            }
            if (updateContent.containsKey("storageClass")) {
                pvc.getSpec().setStorageClassName((String) updateContent.get("storageClass"));
            }
            if (updateContent.containsKey("accessModes")) {
                pvc.getSpec().setAccessModes((List<String>) updateContent.get("accessModes"));
            }

            client.persistentVolumeClaims()
                    .inNamespace(Constant.K8S_NAMESPACE)
                    .resource(pvc)
                    .update();

            return Result.success("PVC更新成功");
        } catch (Exception e) {
            log.error("K8s连接异常", e);
            return Result.error("集群连接异常");
        }
    }

    private Map<String, Object> parsePvcContent(String content) {
        // 这里实现从字符串内容解析出PVC更新参数的逻辑
        // 可以根据实际需求实现，例如解析JSON格式的内容
        return new HashMap<>();
    }
}