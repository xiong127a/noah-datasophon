package com.datasophon.api.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.mapper.ClusterInfoMapper;

public class ClusterInfoUtils {

    public static String getKubernetesNamespace(Integer clusterId) {
        ClusterInfoMapper clusterInfoMapper = SpringUtil.getBean(ClusterInfoMapper.class);
        ClusterInfoEntity clusterInfoEntity = clusterInfoMapper.selectById(clusterId);
        return clusterInfoEntity.getNamespace();
    }

}
