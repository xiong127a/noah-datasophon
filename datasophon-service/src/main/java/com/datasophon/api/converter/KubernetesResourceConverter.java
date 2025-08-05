package com.datasophon.api.converter;

import com.datasophon.common.dto.KubernetesResourceDTO;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kubernetes资源转换器
 * 使用MapStruct进行DTO和Kubernetes资源之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper(componentModel = "spring")
@Component
public interface KubernetesResourceConverter {

    /**
     * Pod转KubernetesResourceDTO
     */
    @Mapping(target = "name", source = "metadata.name")
    @Mapping(target = "namespace", source = "metadata.namespace")
    @Mapping(target = "kind", constant = "Pod")
    @Mapping(target = "creationTimestamp", source = "metadata.creationTimestamp")
    @Mapping(target = "status", source = "status.phase")
    @Mapping(target = "labels", source = "metadata.labels")
    @Mapping(target = "annotations", source = "metadata.annotations")
    @Mapping(target = "additionalProperties", source = ".", qualifiedByName = "mapPodProperties")
    KubernetesResourceDTO podToDto(Pod pod);

    /**
     * Deployment转KubernetesResourceDTO
     */
    @Mapping(target = "name", source = "metadata.name")
    @Mapping(target = "namespace", source = "metadata.namespace")
    @Mapping(target = "kind", constant = "Deployment")
    @Mapping(target = "creationTimestamp", source = "metadata.creationTimestamp")
    @Mapping(target = "status", source = ".", qualifiedByName = "mapDeploymentStatus")
    @Mapping(target = "labels", source = "metadata.labels")
    @Mapping(target = "annotations", source = "metadata.annotations")
    @Mapping(target = "additionalProperties", source = ".", qualifiedByName = "mapDeploymentProperties")
    KubernetesResourceDTO deploymentToDto(Deployment deployment);

    /**
     * Service转KubernetesResourceDTO
     */
    @Mapping(target = "name", source = "metadata.name")
    @Mapping(target = "namespace", source = "metadata.namespace")
    @Mapping(target = "kind", constant = "Service")
    @Mapping(target = "creationTimestamp", source = "metadata.creationTimestamp")
    @Mapping(target = "status", constant = "Active")
    @Mapping(target = "labels", source = "metadata.labels")
    @Mapping(target = "annotations", source = "metadata.annotations")
    @Mapping(target = "additionalProperties", source = ".", qualifiedByName = "mapServiceProperties")
    KubernetesResourceDTO serviceToDto(Service service);

    /**
     * 通用HasMetadata转换
     */
    @Mapping(target = "name", source = "metadata.name")
    @Mapping(target = "namespace", source = "metadata.namespace")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "creationTimestamp", source = "metadata.creationTimestamp")
    @Mapping(target = "status", constant = "Unknown")
    @Mapping(target = "labels", source = "metadata.labels")
    @Mapping(target = "annotations", source = "metadata.annotations")
    @Mapping(target = "additionalProperties", source = ".", qualifiedByName = "mapGenericProperties")
    KubernetesResourceDTO hasMetadataToDto(HasMetadata resource);

    @Named("mapPodProperties")
    default Map<String, Object> mapPodProperties(Pod pod) {
        Map<String, Object> props = new HashMap<>();
        if (pod.getStatus() != null) {
            props.put("podIP", pod.getStatus().getPodIP());
            props.put("hostIP", pod.getStatus().getHostIP());
            props.put("phase", pod.getStatus().getPhase());
        }
        return props;
    }

    @Named("mapDeploymentStatus")
    default String mapDeploymentStatus(Deployment deployment) {
        if (deployment.getStatus() != null) {
            Integer ready = deployment.getStatus().getReadyReplicas();
            Integer total = deployment.getStatus().getReplicas();
            return "就绪:" + (ready != null ? ready : 0) + "/" + (total != null ? total : 0);
        }
        return "Unknown";
    }

    @Named("mapDeploymentProperties")
    default Map<String, Object> mapDeploymentProperties(Deployment deployment) {
        Map<String, Object> props = new HashMap<>();
        if (deployment.getStatus() != null) {
            props.put("replicas", deployment.getStatus().getReplicas());
            props.put("readyReplicas", deployment.getStatus().getReadyReplicas());
            props.put("availableReplicas", deployment.getStatus().getAvailableReplicas());
        }
        return props;
    }

    @Named("mapServiceProperties")
    default Map<String, Object> mapServiceProperties(Service service) {
        Map<String, Object> props = new HashMap<>();
        if (service.getSpec() != null) {
            props.put("type", service.getSpec().getType());
            props.put("clusterIP", service.getSpec().getClusterIP());
            props.put("ports", service.getSpec().getPorts());
        }
        return props;
    }

    @Named("mapGenericProperties")
    default Map<String, Object> mapGenericProperties(HasMetadata resource) {
        Map<String, Object> props = new HashMap<>();
        props.put("apiVersion", resource.getApiVersion());
        props.put("kind", resource.getKind());
        return props;
    }

    /**
     * 根据资源类型自动选择合适的转换方法
     */
    default KubernetesResourceDTO convertToDto(HasMetadata resource) {
        return switch (resource) {
            case Pod pod -> podToDto(pod);
            case Deployment deployment -> deploymentToDto(deployment);
            case Service service -> serviceToDto(service);
            case null, default -> hasMetadataToDto(resource);
        };
    }
}