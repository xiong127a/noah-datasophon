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

package com.datasophon.api.converter;

import com.datasophon.common.dto.ServiceConfigGroupDTO;
import com.datasophon.common.model.ServiceConfig;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务配置分组转换器
 * 负责将分组后的配置数据转换为DTO
 * 使用JDK21语法特性进行现代化实现
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 * @since JDK21
 */
@Component
public class ServiceConfigGroupConverter {

    /**
     * 将分组后的配置映射转换为DTO
     * 支持Kubernetes配置的子分组处理
     * 
     * @param groupedConfigs 分组后的配置映射
     * @return 服务配置分组DTO
     */
    public ServiceConfigGroupDTO toDto(Map<String, List<ServiceConfig>> groupedConfigs) {
        if (groupedConfigs == null || groupedConfigs.isEmpty()) {
            return new ServiceConfigGroupDTO(Map.of());
        }

        var groups = groupedConfigs.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> processRoleGroup(entry.getKey(), entry.getValue())
                ));

        return new ServiceConfigGroupDTO(groups);
    }

    /**
     * 处理单个角色分组，支持Kubernetes子分组
     * 
     * @param roleName 角色名称
     * @param configs 配置列表
     * @return 分组信息
     */
    private ServiceConfigGroupDTO.GroupInfo processRoleGroup(String roleName, List<ServiceConfig> configs) {
        // 分离Kubernetes配置和普通配置
        var kubernetesConfigs = new java.util.HashMap<String, List<ServiceConfig>>();
        var normalConfigs = new java.util.ArrayList<ServiceConfig>();

        for (var config : configs) {
            var configGroup = config.getConfigGroup();
            if (configGroup != null && configGroup.startsWith("kubernetes.config.")) {
                // 解析kubernetes配置类型
                var parts = configGroup.split("\\.");
                if (parts.length >= 3) {
                    var type = parts[2]; // persistent-volume-claims, resources, services
                    kubernetesConfigs.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(config);
                }
            } else {
                normalConfigs.add(config);
            }
        }

        // 构建最终的配置列表和子分组
        var finalConfigs = new java.util.ArrayList<>(normalConfigs);
        var subGroups = new java.util.HashMap<String, ServiceConfigGroupDTO.GroupInfo>();

        // 处理Kubernetes子分组
        if (!kubernetesConfigs.isEmpty()) {
            for (var entry : kubernetesConfigs.entrySet()) {
                var type = entry.getKey();
                var typeConfigs = entry.getValue();
                
                // 创建子分组显示名称
                var subGroupDisplayName = generateKubernetesSubGroupDisplayName(type);
                var subGroupInfo = new ServiceConfigGroupDTO.GroupInfo(subGroupDisplayName, typeConfigs);
                
                // 使用完整的分组键作为子分组键
                var subGroupKey = "kubernetes.config." + type + "." + roleName;
                subGroups.put(subGroupKey, subGroupInfo);
            }
        }

        // 生成角色显示名称
        var displayName = generateDisplayName(roleName);
        
        // 如果有子分组，创建包含子分组的GroupInfo
        if (!subGroups.isEmpty()) {
            return new ServiceConfigGroupDTO.GroupInfo(displayName, finalConfigs, subGroups);
        } else {
            return new ServiceConfigGroupDTO.GroupInfo(displayName, finalConfigs);
        }
    }

    /**
     * 生成Kubernetes子分组的显示名称
     * 
     * @param type Kubernetes配置类型
     * @return 显示名称
     */
    private String generateKubernetesSubGroupDisplayName(String type) {
        return switch (type) {
            case "persistent-volume-claims" -> "存储配置";
            case "resources" -> "资源配置";
            case "services" -> "服务配置";
            case "config-maps" -> "配置映射";
            case "secrets" -> "密钥配置";
            case "volumes" -> "存储卷配置";
            case "node-selector" -> "节点选择";
            case "affinity" -> "亲和性配置";
            case "tolerations" -> "容忍度配置";
            case "security-context" -> "安全上下文";
            default -> type + "配置";
        };
    }

    /**
     * 生成友好的显示名称
     * 
     * @param groupKey 分组键
     * @return 显示名称
     */
    private String generateDisplayName(String groupKey) {
        if (groupKey == null || groupKey.isBlank()) {
            return "通用配置";
        }

        // 处理自定义和高级配置分组
        if (groupKey.startsWith("自定义") || groupKey.startsWith("高级")) {
            return groupKey; // 直接返回原始分组名，已经是中文友好名称
        }
        
        // 使用JDK21增强的switch表达式进行名称转换
        return switch (groupKey.toLowerCase()) {
            case "general" -> "通用配置";
            case "prometheus" -> "Prometheus监控";
            case "hdfs" -> "HDFS存储";
            case "yarn" -> "YARN资源管理";
            case "hive" -> "Hive数据仓库";
            case "spark" -> "Spark计算引擎";
            case "flink" -> "Flink流计算";
            case "kafka" -> "Kafka消息队列";
            case "zookeeper" -> "ZooKeeper协调服务";
            case "elasticsearch" -> "Elasticsearch搜索引擎";
            case "kibana" -> "Kibana可视化";
            case "logstash" -> "Logstash日志处理";
            case "mysql" -> "MySQL数据库";
            case "redis" -> "Redis缓存";
            case "mongodb" -> "MongoDB文档数据库";
            case "cassandra" -> "Cassandra分布式数据库";
            case "hbase" -> "HBase列式存储";
            case "solr" -> "Solr搜索平台";
            case "storm" -> "Storm实时计算";
            case "flume" -> "Flume数据采集";
            case "sqoop" -> "Sqoop数据传输";
            case "oozie" -> "Oozie工作流";
            case "ranger" -> "Ranger权限管理";
            case "knox" -> "Knox网关服务";
            case "ambari" -> "Ambari集群管理";
            case "atlas" -> "Atlas数据治理";
            case "nifi" -> "NiFi数据流";
            case "superset" -> "Superset可视化";
            case "airflow" -> "Airflow调度";
            case "jupyter" -> "Jupyter笔记本";
            case "zeppelin" -> "Zeppelin笔记本";
            case "livy" -> "Livy Spark REST服务";
            case "kudu" -> "Kudu存储引擎";
            case "impala" -> "Impala查询引擎";
            case "druid" -> "Druid时序数据库";
            case "kylin" -> "Kylin OLAP引擎";
            case "dolphinscheduler" -> "DolphinScheduler调度";
            case "streampark" -> "StreamPark流计算";
            case "dinky" -> "Dinky实时计算";
            case "seatunnel" -> "SeaTunnel数据集成";
            case "doris" -> "Doris分析数据库";
            case "starrocks" -> "StarRocks分析数据库";
            case "clickhouse" -> "ClickHouse分析数据库";
            case "greenplum" -> "Greenplum数据仓库";
            case "tidb" -> "TiDB分布式数据库";
            case "oceanbase" -> "OceanBase数据库";
            case "polardb" -> "PolarDB数据库";
            case "gaussdb" -> "GaussDB数据库";
            case "dameng" -> "达梦数据库";
            case "kingbase" -> "人大金仓数据库";
            case "oscar" -> "神舟通用数据库";
            case "opengauss" -> "openGauss数据库";
            case "tbase" -> "TBase数据库";
            case "gbase" -> "南大通用数据库";
            case "shentong" -> "神通数据库";
            default -> {
                // 处理Kubernetes配置分组
                if (groupKey.startsWith("kubernetes.config.")) {
                    yield parseKubernetesGroupName(groupKey);
                }
                // 处理角色配置分组
                if (groupKey.endsWith("Server") || groupKey.endsWith("Node") || 
                    groupKey.endsWith("Master") || groupKey.endsWith("Worker")) {
                    yield groupKey + "配置";
                }
                // 处理高级和自定义配置
                if (groupKey.startsWith("advanced_")) {
                    yield "高级" + groupKey.substring("advanced_".length()) + "配置";
                }
                if (groupKey.startsWith("custom_")) {
                    yield "自定义" + groupKey.substring("custom_".length()) + "配置";
                }
                // 默认返回首字母大写的分组名
                yield capitalizeFirst(groupKey);
            }
        };
    }

    /**
     * 解析Kubernetes配置分组名称
     * 
     * @param groupKey 分组键
     * @return 友好的显示名称
     */
    private String parseKubernetesGroupName(String groupKey) {
        // 示例: kubernetes.config.persistent-volume-claims.DataNode
        var parts = groupKey.split("\\.");
        if (parts.length >= 3) {
            var configType = parts[2];
            var roleName = parts.length >= 4 ? parts[3] : "";
            
            var typeName = switch (configType) {
                case "persistent-volume-claims" -> "存储配置";
                case "resources" -> "资源配置";
                case "services" -> "服务配置";
                case "config-maps" -> "配置映射";
                case "secrets" -> "密钥配置";
                case "volumes" -> "存储卷配置";
                case "node-selector" -> "节点选择";
                case "affinity" -> "亲和性配置";
                case "tolerations" -> "容忍度配置";
                case "security-context" -> "安全上下文";
                default -> configType + "配置";
            };
            
            return roleName.isBlank() ? 
                    "Kubernetes " + typeName : 
                    roleName + " " + typeName;
        }
        
        return "Kubernetes配置";
    }

    /**
     * 首字母大写
     * 
     * @param str 输入字符串
     * @return 首字母大写的字符串
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
