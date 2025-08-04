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
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.api.utils.string.validator.WordValidator;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.dto.ClusterTenantDTO;
import com.datasophon.common.enums.TROperateType;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.model.tenant.resource.TenantFrameResource;
import com.datasophon.common.model.tenant.resource.TenantResource;
import com.datasophon.dao.entity.ClusterTenant;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.entity.ClusterUserTenant;
import com.datasophon.dao.mapper.ClusterTenantMapper;
import com.mybatisflex.core.query.QueryChain;
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
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterTenantService")
@Slf4j
public class ClusterTenantServiceImpl extends ServiceImpl<ClusterTenantMapper, ClusterTenant>
        implements ClusterTenantService {

    @Autowired
    private ClusterTenantConverter clusterTenantConverter;

    @Override
    public PageResult<ClusterTenantDTO> listTenant(Integer clusterId, Integer page, Integer size, String tenantName) {
        // 使用DAO层分页查询方法
        com.mybatisflex.core.paginate.Page<ClusterTenant> flexPage = getMapper().selectPageByClusterId(clusterId, tenantName, page, size);
        
        List<ClusterTenantDTO> dtoList = clusterTenantConverter.entityListToDtoList(flexPage.getRecords());
        return PageResult.of(dtoList, flexPage.getTotalRow(), flexPage.getPageNumber(), flexPage.getPageSize());
    }

    @Override
    public ClusterTenantDTO saveOrUpdateTenant(ClusterTenantDTO clusterTenantDTO) {
        ClusterTenant clusterTenant = clusterTenantConverter.dtoToEntity(clusterTenantDTO);

        try {
            checkTenant(clusterTenant);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        filterDeleteResource(clusterTenant);

        TenantResource resource = new TenantResource();
        BeanUtil.copyProperties(clusterTenant, resource);
        List<TenantFrameResource> allFrameResource = CollUtil.unionAll(
                resource.getHdfsResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenant.getClusterId())).toList(),
                resource.getHbaseResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenant.getClusterId())).toList(),
                resource.getHiveResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenant.getClusterId())).toList(),
                resource.getKafkaResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenant.getClusterId())).toList(),
                resource.getYarnResourceList().stream().map(t -> (TenantFrameResource) t)
                        .peek(t -> t.setClusterId(clusterTenant.getClusterId())).collect(Collectors.toList()));

        // 框架资源操作
        Map<String, String> globalVariables = GlobalVariables.get(clusterTenant.getClusterId());
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

        this.saveOrUpdate(clusterTenant);

        return clusterTenantConverter.entityToDto(clusterTenant);
    }

    private void filterDeleteResource(ClusterTenant clusterTenant) {
        clusterTenant.getHdfsResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenant.getYarnResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenant.getKafkaResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenant.getHiveResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
        clusterTenant.getHbaseResourceList()
                .removeIf(resource -> TROperateType.DELETE.name().equals(resource.getType()));
    }

    @Override
    public boolean deleteTenantById(Integer id) {
        // 是否授权授权用户校验
        List<ClusterUserTenant> userTenantList = QueryChain.of(ClusterUserTenant.class)
                .where(ClusterUserTenant::getTenantId).eq(id)
                .list();

        if (CollUtil.isNotEmpty(userTenantList)) {
            List<Integer> userIds = userTenantList.stream().map(ClusterUserTenant::getUserId)
                    .toList();

            List<String> usernames = QueryChain.of(ClusterUser.class)
                    .where(ClusterUser::getId).in(userIds)
                    .list()
                    .stream()
                    .map(ClusterUser::getUsername)
                    .toList();

            throw new RuntimeException("当前租户已经授权给用户：" + usernames + ", 请先取消授权");
        }

        ClusterTenant tenant = this.getById(id);
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
    public ClusterTenantDTO getByIdAsDto(Integer id) {
        ClusterTenant entity = getById(id);
        return Objects.nonNull(entity) ? clusterTenantConverter.entityToDto(entity) : null;
    }

    @Override
    public List<ClusterTenantDTO> getTenantsByClusterId(Integer clusterId) {
        // 使用DAO层查询方法
        List<ClusterTenant> entities = getMapper().selectByClusterId(clusterId);
        return clusterTenantConverter.entityListToDtoList(entities);
    }

    @Override
    public ClusterTenantDTO getTenantByName(Integer clusterId, String tenantName) {
        // 使用DAO层查询方法
        ClusterTenant entity = getMapper().selectByClusterIdAndTenantName(clusterId, tenantName);
        return Objects.nonNull(entity) ? clusterTenantConverter.entityToDto(entity) : null;
    }

    @Override
    public ClusterTenantDTO updateTenant(ClusterTenantDTO dto) {
        ClusterTenant entity = clusterTenantConverter.dtoToEntity(dto);
        updateById(entity);
        return clusterTenantConverter.entityToDto(entity);
    }

    private void checkTenant(ClusterTenant clusterTenant) throws Exception {
        List<ClusterTenant> tenantList = this.list();

        // 校验名称
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        WordValidator wordValidator = new WordValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(wordValidator);
        wordValidator.setNext(lengthValidator);
        notEmptyValidator.validate(clusterTenant.getTenantName());

        List<String> exitsName = tenantList.stream().map(ClusterTenant::getTenantName).toList();
        if (Objects.nonNull(clusterTenant.getId())) {
            exitsName.remove(clusterTenant.getTenantName());
        }
        if (CollUtil.isNotEmpty(exitsName) && exitsName.contains(clusterTenant.getTenantName())) {
            throw new IllegalArgumentException("租户名称已存在");
        }

        // 校验hdfs资源
        if (CollUtil.isNotEmpty(clusterTenant.getHdfsResourceList())) {
            List<String> existHdfsPaths = tenantList.stream()
                    .map(ClusterTenant::getHdfsResourceList)
                    .map(this::convertToMap)
                    .flatMap(List::stream)
                    .map(t -> t.get("hdfsPath"))
                    .toList();
            for (com.datasophon.dao.entity.tenantResource.TenantHdfsResource hdfsResource : clusterTenant
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
        if (CollUtil.isNotEmpty(clusterTenant.getYarnResourceList())) {
            List<String> existYarnQueue = tenantList.stream()
                    .map(ClusterTenant::getYarnResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("parentQueueName") + "." + t.get("queueName"))
                    .toList();

            for (com.datasophon.dao.entity.tenantResource.TenantYarnResource tenantYarnResource : clusterTenant
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
        if (CollUtil.isNotEmpty(clusterTenant.getHiveResourceList())) {
            List<String> existHiveDbName = tenantList.stream()
                    .map(ClusterTenant::getHiveResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("hiveDatabase"))
                    .toList();
            for (com.datasophon.dao.entity.tenantResource.TenantHiveResource tenantHiveResource : clusterTenant
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
        if (CollUtil.isNotEmpty(clusterTenant.getHbaseResourceList())) {
            List<String> existHbaseNameSpace = tenantList.stream()
                    .map(ClusterTenant::getHbaseResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("hbaseNamespace"))
                    .toList();

            for (com.datasophon.dao.entity.tenantResource.TenantHbaseResource tenantHbaseResource : clusterTenant
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
        if (CollUtil.isNotEmpty(clusterTenant.getKafkaResourceList())) {
            List<String> existKafkaTopic = tenantList.stream()
                    .map(ClusterTenant::getKafkaResourceList)
                    .map(this::convertToMap)
                    .flatMap(Collection::parallelStream)
                    .map(t -> t.get("kafkaTopicName"))
                    .toList();
            for (com.datasophon.dao.entity.tenantResource.TenantKafkaResource tenantKafkaResource : clusterTenant
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