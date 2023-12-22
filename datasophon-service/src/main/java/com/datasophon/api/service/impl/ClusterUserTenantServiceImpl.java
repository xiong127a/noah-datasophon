package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.TenantRangerActor;
import com.datasophon.api.service.ClusterTenantService;
import com.datasophon.api.service.ClusterUserService;
import com.datasophon.api.service.ClusterUserTenantService;
import com.datasophon.common.command.TenantRangerCommand;
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

import java.util.ArrayList;
import java.util.List;
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

    public Result addUserToTenant(ClusterUserTenant clusterUserTenant) {
        List<ClusterTenant> tenants = clusterTenantService.list(
                new LambdaQueryWrapper<ClusterTenant>()
                        .eq(ClusterTenant::getClusterId, clusterUserTenant.getTenantId())
                        .eq(ClusterTenant::getId, clusterUserTenant.getTenantId())
        );
        if (CollUtil.isEmpty(tenants)) return Result.error("租户不存在");
        List<ClusterUser> users = clusterUserService.list(
                new LambdaQueryWrapper<ClusterUser>()
                        .eq(ClusterUser::getClusterId, clusterUserTenant.getClusterId())
                        .eq(ClusterUser::getId, clusterUserTenant.getUserId())
        );
        if (CollUtil.isEmpty(users)) return Result.error("用户不存在");
        List<String> addUserNames = new ArrayList<>();
        List<Integer> allUserIds = this
                .list(
                        new LambdaQueryWrapper<ClusterUserTenant>()
                                .eq(ClusterUserTenant::getClusterId, clusterUserTenant.getClusterId())
                                .eq(ClusterUserTenant::getTenantId, clusterUserTenant.getTenantId())
                )
                .stream()
                .map(ClusterUserTenant::getUserId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(allUserIds)) {
            addUserNames = clusterUserService
                    .listByIds(allUserIds)
                    .stream()
                    .map(ClusterUser::getUsername)
                    .collect(Collectors.toList());
        }
        String tenantName = tenants.get(0).getTenantName();
        String userName = users.get(0).getUsername();
        addUserNames.add(userName);

        ActorRef tenantRangerActor = ActorUtils.getLocalActor(TenantRangerActor.class, "tenantRangerActor");
        TenantRangerCommand tenantRangerCommand = new TenantRangerCommand();
        tenantRangerCommand.setClusterId(clusterUserTenant.getClusterId());
        tenantRangerCommand.setRoleName(tenantName);
        tenantRangerCommand.setOperateType("addUser");
        tenantRangerCommand.setUserList(addUserNames);
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> execFuture = Patterns.ask(tenantRangerActor, tenantRangerCommand, timeout);
        ExecResult execResult = null;
        try {
            execResult = (ExecResult) Await.result(execFuture, timeout.duration());
            if (execResult.getExecResult()) {
                logger.info("add user to ranger role success");
            } else {
                logger.error(execResult.getExecOut());
                throw new ServiceException(500, "add user to ranger role failed");
            }
        } catch (Exception e) {
            throw new ServiceException(500, "add user to ranger role failed");
        }

        this.saveOrUpdate(clusterUserTenant);

        return Result.success();
    }

}
