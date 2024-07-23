package com.datasophon.k8s.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.K8sClusterInfoEntity;

public interface K8sClusterInfoService extends IService<K8sClusterInfoEntity> {

    Result saveCluster(K8sClusterInfoEntity k8sClusterInfo);

}
