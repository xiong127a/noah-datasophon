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

package com.datasophon.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.TenantRangerActor;
import com.datasophon.api.master.TenantResourceDispatcherActor;
import com.datasophon.api.converter.ClusterTenantConverter;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.api.service.ClusterUserTenantService;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.api.utils.string.validator.WordValidator;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.dto.ClusterTenantDTO;
import com.datasophon.common.enums.TROperateType;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.model.tenant.resource.TenantFrameResource;
import com.datasophon.common.model.tenant.resource.TenantResource;
import com.datasophon.dao.entity.ClusterTenantEntity;
import com.datasophon.dao.entity.ClusterUserTenantEntity;
import com.datasophon.dao.entity.tenantResource.TenantHbaseResourceEntity;
import com.datasophon.dao.entity.tenantResource.TenantHdfsResourceEntity;
import com.datasophon.dao.entity.tenantResource.TenantHiveResourceEntity;
import com.datasophon.dao.entity.tenantResource.TenantKafkaResourceEntity;
import com.datasophon.dao.entity.tenantResource.TenantYarnResourceEntity;
import com.datasophon.dao.mapper.ClusterTenantMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.ActorRef;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

import static com.datasophon.common.enums.RangerOpType.DELETE_TENANT;

/**
 * 集群租户服务实现
 * 按照架构重构规范，迁移QueryChain到Service层调用
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterTenantService")
@Slf4j
public class ClusterTenantServiceImpl extends ServiceImpl<ClusterTenantMapper, ClusterTenantEntity>
        implements ClusterTenantService {

    @Autowired
    private ClusterTenantConverter clusterTenantConverter;

    @Autowired
    private ClusterUserTenantService clusterUserTenantService;

    @Autowired
    private ClusterUserService clusterUserService;

    @Override
    public PageResult<ClusterTenantDTO> listTenant(Long clusterId, Integer page, Integer size, String tenantName) {
        // 使用DAO层分页查询方法
        com.mybatisflex.core.paginate.Page<ClusterTenantEntity> flexPage = getMapper().selectPageByClusterId(clusterId,
                tenantName, page, size);

        List<ClusterTenantDTO> dtoList = clusterTenantConverter.entityListToDtoList(flexPage.getRecords());
        return PageResult.of(dtoList, flexPage.getTotalRow(), flexPage.getPageNumber(), flexPage.getPageSize());
    }

    @Override
    public ClusterTenantDTO saveOrUpdateTenant(ClusterTenantDTO clusterTenantDTO) {
        ClusterTenantEntity clusterTenantEntity = clusterTenantConverter.dtoToEntity(clusterTenantDTO);

        try {
            checkTenant(clusterTenantEntity);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        filterDeleteResource(clusterTenantEntity);

        TenantResource resource = new TenantResource();
        BeanUtil.copyProperties(clusterTenantEntity, resource);
        List<TenantFrameResource> allFrameResource = CollUtil.unionAll(
                resource.getHdfsResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenantEntity.getClusterId())).toList(),
                resource.getHbaseResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenantEntity.getClusterId())).toList(),
                resource.getHiveResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenantEntity.getClusterId())).toList(),
                resource.getKafkaResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenantEntity.getClusterId())).toList(),
                resource.getYarnResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenantEntity.getClusterId())).collect(Collectors.toList()));

        // 框架资源操作
        Map<String, String> globalVariables = GlobalVariables.get(clusterTenantEntity.getClusterId());
        ActorRef tenantResourceDispatcherActor = ActorUtils.getLocalActor(TenantResourceDispatcherActor.class,
                "tenantResourceDispatcherActor");
        for (TenantFrameResource tenantFrameResource : allFrameResource) {
            String enableKerberos = globalVariables
                    .get("${enable" + tenantFrameResource.getServiceName() + "Kerberos}");
            tenantFrameResource.setEnableKerberos(StrUtil.isNotEmpty(enableKerberos) && "true".equals(enableKerberos));
            tenantResourceDispatcherActor.tell(tenantFrameResource, ActorRef.noSender());
        }

        // ranger策略操作
        ActorRef tenantRangerActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");
        tenantRangerActor.tell(resource, ActorRef.noSender());

        this.saveOrUpdate(clusterTenantEntity);

        return clusterTenantConverter.entityToDto(clusterTenantEntity);
    }

    private void filterDeleteResource(ClusterTenantEntity clusterTenantEntity) {
        clusterTenantEntity.getHdfsResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenantEntity.getYarnResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenantEntity.getKafkaResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenantEntity.getHiveResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenantEntity.getHbaseResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
    }

    @Override
    public boolean deleteTenantById(Long id) {
        // 检查是否有用户被授权到此租户
        List<ClusterUserTenantEntity> userTenantList = clusterUserTenantService.getListByTenantId(id);

        if (CollUtil.isNotEmpty(userTenantList)) {
            List<Long> userIds = userTenantList.stream()
                    .map(ClusterUserTenantEntity::getUserId)
                    .toList(); // JDK21现代特性

            List<String> usernames = clusterUserService.getUsernamesByIds(userIds);

            throw new RuntimeException("当前租户已经授权给用户：" + usernames + ", 请先取消授权");
        }

        ClusterTenantEntity tenant = this.getById(id);
        TenantRangerCommand command = new TenantRangerCommand();
        command.setClusterId(tenant.getClusterId());
        command.setTenantName(tenant.getTenantName());
        command.setOperateType(DELETE_TENANT);

        // 删除所有ranger相关策略及角色
        ActorRef tenantRangerActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");
        tenantRangerActor.tell(command, ActorRef.noSender());

        return this.removeById(id);
    }

    @Override
    public ClusterTenantDTO getByIdAsDto(Long id) {
        ClusterTenantEntity entity = getById(id);
        return Objects.nonNull(entity) ? clusterTenantConverter.entityToDto(entity) : null;
    }

    @Override
    public List<ClusterTenantDTO> getTenantsByClusterId(Long clusterId) {
        // 使用DAO层查询方法
        List<ClusterTenantEntity> entities = getMapper().selectByClusterId(clusterId);
        return clusterTenantConverter.entityListToDtoList(entities);
    }

    @Override
    public ClusterTenantDTO getTenantByName(Long clusterId, String tenantName) {
        // 使用DAO层查询方法
        ClusterTenantEntity entity = getMapper().selectByClusterIdAndTenantName(clusterId, tenantName);
        return Objects.nonNull(entity) ? clusterTenantConverter.entityToDto(entity) : null;
    }

    @Override
    public ClusterTenantDTO updateTenant(ClusterTenantDTO dto) {
        ClusterTenantEntity entity = clusterTenantConverter.dtoToEntity(dto);
        updateById(entity);
        return clusterTenantConverter.entityToDto(entity);
    }

    private void checkTenant(ClusterTenantEntity clusterTenantEntity) throws Exception {
        List<ClusterTenantEntity> tenantList = this.list();

        // 校验名称
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        WordValidator wordValidator = new WordValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(wordValidator);
        wordValidator.setNext(lengthValidator);
        notEmptyValidator.validate(clusterTenantEntity.getTenantName());

        List<String> exitsName = tenantList.stream().map(ClusterTenantEntity::getTenantName).toList();
        if (Objects.nonNull(clusterTenantEntity.getId())) {
            exitsName.remove(clusterTenantEntity.getTenantName());
        }
        if (CollUtil.isNotEmpty(exitsName) && exitsName.contains(clusterTenantEntity.getTenantName())) {
            throw new IllegalArgumentException("租户名称已存在");
        }

        // 校验hdfs资源
        if (CollUtil.isNotEmpty(clusterTenantEntity.getHdfsResourceList())) {
            List<String> existHdfsPaths = tenantList.stream()
                    .map(ClusterTenantEntity::getHdfsResourceList)
                    .map(this::convertToMap)
                    .flatMap(List::stream)
                    .map(t -> t.get("hdfsPath"))
                    .toList();
            for (TenantHdfsResourceEntity hdfsResource : clusterTenantEntity
                    .getHdfsResourceList()) {
                if (!TROperateType.ADD.name().equals(hdfsResource.getType())) {
                    continue;
                }
                if (!StrUtil.startWith(hdfsResource.getHdfsPath(), "/")) {
                    throw new IllegalArgumentException("hdfs路径不合法");
                }
                if ("/".equals(hdfsResource.getHdfsPath())) {
                    throw new IllegalArgumentException("不能设置为hdfs跟路径");
                }
                if (existHdfsPaths.contains(hdfsResource.getHdfsPath())) {
                    throw new IllegalArgumentException("hdfs路径已被添加过,请勿重复添加");
                }
                if (!StrUtil.isNumeric(hdfsResource.getHdfsSpaceQuota())) {
                    throw new IllegalArgumentException("hdfs配置不合法");
                }
            }
        }

        // 校验yarn
        if (CollUtil.isNotEmpty(clusterTenantEntity.getYarnResourceList())) {
            List<String> existYarnQueue = tenantList.stream()
                    .map(ClusterTenantEntity::getYarnResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("parentQueueName") + "." + t.get("queueName"))
                    .toList();

            for (TenantYarnResourceEntity tenantYarnResource : clusterTenantEntity
                    .getYarnResourceList()) {
                if (!TROperateType.ADD.name().equals(tenantYarnResource.getType())) {
                    continue;
                }
                String queueName = tenantYarnResource.getParentQueueName() + "." + tenantYarnResource.getQueueName();
                if (existYarnQueue.contains(queueName)) {
                    throw new IllegalArgumentException("yarn队列已经被添加过");
                }
                if (!StrUtil.isNumeric(tenantYarnResource.getCapacityPercent())
                        ||
                        Convert.toInt(tenantYarnResource.getCapacityPercent()) < 0
                        ||
                        Convert.toInt(tenantYarnResource.getCapacityPercent()) > 100) {
                    throw new IllegalArgumentException("yarn队列配额不合法");
                }
            }
        }

        // 校验hive
        if (CollUtil.isNotEmpty(clusterTenantEntity.getHiveResourceList())) {
            List<String> existHiveDbName = tenantList.stream()
                    .map(ClusterTenantEntity::getHiveResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("hiveDatabase"))
                    .toList();
            for (TenantHiveResourceEntity tenantHiveResource : clusterTenantEntity
                    .getHiveResourceList()) {
                if (!TROperateType.ADD.name().equals(tenantHiveResource.getType())) {
                    continue;
                }
                if (existHiveDbName.contains(tenantHiveResource.getHiveDatabase())) {
                    throw new IllegalArgumentException("hive数据库已经被添加过");
                }
                if (!StrUtil.isNumeric(tenantHiveResource.getHiveDatabaseCapacity())) {
                    throw new IllegalArgumentException("hive配额设置不合法");
                }
            }
        }

        // 校验hbase
        if (CollUtil.isNotEmpty(clusterTenantEntity.getHbaseResourceList())) {
            List<String> existHbaseNameSpace = tenantList.stream()
                    .map(ClusterTenantEntity::getHbaseResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("hbaseNamespace"))
                    .toList();

            for (TenantHbaseResourceEntity tenantHbaseResource : clusterTenantEntity
                    .getHbaseResourceList()) {
                if (!TROperateType.ADD.name().equals(tenantHbaseResource.getType())) {
                    continue;
                }
                if (existHbaseNameSpace.contains(tenantHbaseResource.getHbaseNamespace())) {
                    throw new IllegalArgumentException("hbase namespace已经被添加过");
                }
                if (!StrUtil.isNumeric(tenantHbaseResource.getHbaseCapacity())
                        || !StrUtil.isNumeric(tenantHbaseResource.getHbaseRegionServerNum())) {
                    throw new IllegalArgumentException("hbase配额设置不合法");
                }
            }
        }

        // 校验kafka
        if (CollUtil.isNotEmpty(clusterTenantEntity.getKafkaResourceList())) {
            List<String> existKafkaTopic = tenantList.stream()
                    .map(ClusterTenantEntity::getKafkaResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("kafkaTopicName"))
                    .toList();
            for (TenantKafkaResourceEntity tenantKafkaResource : clusterTenantEntity
                    .getKafkaResourceList()) {
                if (!TROperateType.ADD.name().equals(tenantKafkaResource.getType())) {
                    continue;
                }
                if (existKafkaTopic.contains(tenantKafkaResource.getKafkaTopicName())) {
                    throw new IllegalArgumentException("kafka topic已经被添加过");
                }
                if (!StrUtil.isNumeric(tenantKafkaResource.getKafkaReplicas())
                        || !StrUtil.isNumeric(tenantKafkaResource.getKafkaTopicCapacity())) {
                    throw new IllegalArgumentException("kafka配额设置不合法");
                }
            }
        }
    }

    public <T> List<LinkedHashMap<String, String>> convertToMap(List<T> list) {
        return Convert.convert(new TypeReference<List<LinkedHashMap<String, String>>>() {
        }, list);
    }

}