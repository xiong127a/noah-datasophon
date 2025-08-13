package com.datasophon.api.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.mapper.ClusterInfoMapper;

public class ClusterInfoUtils {

    public static String getKubernetesNamespace(Long clusterId) {
        ClusterInfoMapper clusterInfoMapper = SpringUtil.getBean(ClusterInfoMapper.class);
        ClusterInfoEntity clusterInfoEntity = clusterInfoMapper.selectOneById(clusterId);
        return clusterInfoEntity.getNamespace();
    }

}
