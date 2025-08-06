package com.datasophon.api.converter;

import com.datasophon.common.dto.K8sNamespaceDTO;
import com.datasophon.common.dto.K8sResourceStatsDTO;
import com.datasophon.common.vo.K8sNamespaceVO;
import com.datasophon.common.vo.K8sResourceStatsVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Kubernetes资源转换器
 * 用于DTO和VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface K8sResourceConverter {

    K8sResourceConverter INSTANCE = Mappers.getMapper(K8sResourceConverter.class);

    /**
     * K8sNamespaceDTO转换为K8sNamespaceVO
     */
    @Mapping(target = "name", source = "name")
    @Mapping(target = "phase", source = "phase")
    @Mapping(target = "creationTime", source = "creationTime")
    @Mapping(target = "displayName", expression = "java(dto.getDisplayName())")
    @Mapping(target = "phaseText", source = "phase", qualifiedByName = "getPhaseText")
    @Mapping(target = "isActive", expression = "java(dto.isActive())")
    @Mapping(target = "statusColor", source = "phase", qualifiedByName = "getStatusColor")
    K8sNamespaceVO namespaceToVo(K8sNamespaceDTO dto);

    /**
     * K8sResourceStatsDTO转换为K8sResourceStatsVO
     */
    @Mapping(target = "totalPodCount", expression = "java(dto.getTotalPodCount())")
    @Mapping(target = "podHealthRate", expression = "java(dto.getPodHealthRate())")
    @Mapping(target = "hasFailedPods", expression = "java(dto.hasFailedPods())")
    @Mapping(target = "healthStatus", expression = "java(getHealthStatus(dto.getPodHealthRate(), dto.hasFailedPods()))")
    @Mapping(target = "healthStatusColor", expression = "java(getHealthStatusColor(dto.getPodHealthRate(), dto.hasFailedPods()))")
    K8sResourceStatsVO statsToVo(K8sResourceStatsDTO dto);

    /**
     * 批量转换命名空间列表
     */
    List<K8sNamespaceVO> namespaceListToVoList(List<K8sNamespaceDTO> dtoList);

    /**
     * 获取状态显示文本
     */
    @Named("getPhaseText")
    default String getPhaseText(String phase) {
        if (phase == null) {
            return "未知";
        }
        if ("Active".equals(phase)) {
            return "活跃";
        } else if ("Terminating".equals(phase)) {
            return "终止中";
        } else {
            return phase;
        }
    }

    /**
     * 获取状态颜色
     */
    @Named("getStatusColor")
    default String getStatusColor(String phase) {
        if (phase == null) {
            return "gray";
        }
        if ("Active".equals(phase)) {
            return "green";
        } else if ("Terminating".equals(phase)) {
            return "orange";
        } else {
            return "red";
        }
    }

    /**
     * 获取健康状态文本
     */
    default String getHealthStatus(Double healthRate, Boolean hasFailed) {
        if (hasFailed) {
            return "异常";
        }
        if (healthRate >= 95.0) {
            return "健康";
        } else if (healthRate >= 80.0) {
            return "良好";
        } else {
            return "警告";
        }
    }

    /**
     * 获取健康状态颜色
     */
    default String getHealthStatusColor(Double healthRate, Boolean hasFailed) {
        if (hasFailed) {
            return "red";
        }
        if (healthRate >= 95.0) {
            return "green";
        } else if (healthRate >= 80.0) {
            return "blue";
        } else {
            return "orange";
        }
    }
}