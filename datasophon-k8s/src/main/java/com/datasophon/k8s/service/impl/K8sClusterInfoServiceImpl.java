package com.datasophon.k8s.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.enums.Status;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.K8sClusterInfoEntity;
import com.datasophon.dao.enums.ClusterState;
import com.datasophon.dao.mapper.K8sClusterInfoMapper;
import com.datasophon.k8s.service.K8sClusterInfoService;
import com.datasophon.k8s.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service("k8sClusterInfoService")
public class K8sClusterInfoServiceImpl extends ServiceImpl<K8sClusterInfoMapper, K8sClusterInfoEntity>
        implements
        K8sClusterInfoService {

    @Autowired
    private KubeUtil kubeUtil;

    @Autowired
    private ClusterHostService clusterHostService;

    @Override
    public Result saveCluster(K8sClusterInfoEntity k8sClusterInfo) {
        List<K8sClusterInfoEntity> list = this.lambdaQuery()
                .eq(K8sClusterInfoEntity::getClusterCode, k8sClusterInfo.getClusterCode())
                .list();
        if (Objects.nonNull(list) && !CollUtil.isNotEmpty(list)) {
            return Result.error(Status.CLUSTER_CODE_EXISTS.getMsg());
        }

        KubernetesClient kubernetesClient = kubeUtil.getKubeClientByConfig(k8sClusterInfo.getKubeConfig());

        if (!kubeUtil.checkNamespace(kubernetesClient, k8sClusterInfo.getNamespace())) {
            return Result.error(Status.K8S_NAMESPACE_NOT_EXIST.getMsg());
        }

        k8sClusterInfo.setCreateTime(new Date());
        k8sClusterInfo.setCreateBy(SecurityUtils.getAuthUser().getUsername());
        k8sClusterInfo.setClusterState(ClusterState.NEED_CONFIG);
        this.save(k8sClusterInfo);

        List<ClusterHostDO> hostList = kubeUtil.getHostListByConfig(k8sClusterInfo.getId());
        clusterHostService.saveBatch(hostList);

        return Result.success();
    }

}
