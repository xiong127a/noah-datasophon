package com.datasophon.api.strategy;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuis;

import javax.sound.midi.Soundbank;
import java.util.List;
import java.util.Map;

public class KibanaSearchHandlerStrategy implements ServiceRoleStrategy{
    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        System.out.println(1);
    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        for (ServiceConfig config : list) {
            if ("server.port".equals(config.getName())) {
                ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${kibanaPort}", Convert.toStr(config.getValue()));
            }
        }
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {

    }
}
