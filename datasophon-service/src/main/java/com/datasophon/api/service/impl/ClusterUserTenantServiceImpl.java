package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.TenantRangerActor;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.api.service.ClusterUserTenantService;
import com.datasophon.common.command.TenantRangerCommand;
import com.datasophon.common.enums.RangerOpType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterTenant;
import com.datasophon.dao.entity.ClusterUser;
import com.datasophon.dao.entity.ClusterUserTenant;
import com.datasophon.dao.mapper.ClusterUserTenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("clusterUserTenantService")
@Transactional
public class ClusterUserTenantServiceImpl extends ServiceImpl<ClusterUserTenantMapper, ClusterUserTenant> implements ClusterUserTenantService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterUserTenantServiceImpl.class);

    @Autowired
    private ClusterUserService clusterUserService;

    @Autowired
    private ClusterTenantService clusterTenantService;

    @Override
    public Result addUserToTenant(Integer clusterId, Integer userId, String tenantIds) {
        List<Integer> tenantIdList = StrUtil.split(tenantIds, ",").stream().map(Convert::toInt).collect(Collectors.toList());
        List<ClusterUserTenant> list = this.lambdaQuery()
                .eq(ClusterUserTenant::getClusterId, clusterId)
                .eq(ClusterUserTenant::getUserId, userId)
                .in(ClusterUserTenant::getTenantId, tenantIdList)
                .list();
        if (CollUtil.isNotEmpty(list)) return Result.error("当前用户授权已存在");
        List<ClusterUserTenant> addUserTenant = tenantIdList.stream()
                .map(t -> ClusterUserTenant.builder().tenantId(t).clusterId(clusterId).userId(userId).build())
                .collect(Collectors.toList());
        this.saveOrUpdateBatch(addUserTenant);
        operateTenantUser(clusterId, userId, tenantIdList);
        return Result.success();
    }

    @Override
    public Result deleteUser(Integer clusterId, Integer userId, String tenantIds) {
        List<Integer> tenantIdList = StrUtil.split(tenantIds, ",").stream().map(Convert::toInt).collect(Collectors.toList());
        QueryWrapper<ClusterUserTenant> removeQuery = new QueryWrapper<>();
        removeQuery.eq("cluster_id", clusterId);
        removeQuery.eq("user_id", userId);
        removeQuery.in("tenant_id", tenantIdList);
        this.remove(removeQuery);
        operateTenantUser(clusterId, userId, tenantIdList);
        return Result.success();
    }

    @Override
    public Result getListByUserId(Integer clusterId, Integer userId) {
        Map<Integer, String> tenantMap = clusterTenantService.list()
                .stream()
                .collect(Collectors.toMap(ClusterTenant::getId, ClusterTenant::getTenantName));
        List<ClusterUserTenant> userTenantList = this.lambdaQuery()
                .eq(ClusterUserTenant::getClusterId, clusterId)
                .eq(ClusterUserTenant::getUserId, userId)
                .list();
        userTenantList.forEach(t -> t.setTenantName(tenantMap.get(t.getTenantId())));
        return Result.success(userTenantList);
    }

    private Result operateTenantUser(Integer clusterId, Integer userId, List<Integer> tenantIdList) {
        List<ClusterUserTenant> allUserTenants = this.list();
        List<ClusterUser> allUsers = clusterUserService.list();
        List<ClusterUser> users = allUsers.stream()
                .filter(t -> t.getClusterId().equals(clusterId) && t.getId().equals(userId))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(users)) return Result.error("用户不存在");
        List<ClusterTenant> tenantList = clusterTenantService.lambdaQuery()
                .eq(ClusterTenant::getClusterId, clusterId)
                .in(ClusterTenant::getId, tenantIdList)
                .list();
        ActorRef tenantRangerActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");

        for (ClusterTenant clusterTenant : tenantList) {
            List<Integer> exitsUserIds = allUserTenants.stream()
                    .filter(t -> t.getClusterId().equals(clusterId) && t.getTenantId().equals(clusterTenant.getId()))
                    .map(ClusterUserTenant::getUserId)
                    .collect(Collectors.toList());
            List<String> exitsUserNames = allUsers.stream()
                    .filter(t -> exitsUserIds.contains(t.getId()))
                    .map(ClusterUser::getUsername)
                    .collect(Collectors.toList());

            TenantRangerCommand tenantRangerCommand = new TenantRangerCommand();
            tenantRangerCommand.setClusterId(clusterId);
            tenantRangerCommand.setRoleName(clusterTenant.getTenantName());
            tenantRangerCommand.setOperateType(RangerOpType.OP_USER_TO_ROLE);
            tenantRangerCommand.setUserList(exitsUserNames);
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            Future<Object> execFuture = Patterns.ask(tenantRangerActor, tenantRangerCommand, timeout);
            ExecResult execResult = null;
            try {
                execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("operate user to ranger role success");
                } else {
                    logger.error(execResult.getExecOut());
                    throw new ServiceException(500, "operate user to ranger role failed");
                }
            } catch (Exception e) {
                throw new ServiceException(500, "operate user to ranger role failed");
            }

        }
        return null;
    }

}
