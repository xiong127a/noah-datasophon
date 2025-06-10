package com.datasophon.api.service.impl;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.AutoScaleService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.AutoScaleTaskVO;
import com.datasophon.k8s.util.K8sUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.datasophon.k8s.constants.Constant.K8S_NAMESPACE;


@Service
public class AutoScaleServiceImpl implements AutoScaleService {

    private static final String SEATUNNEL_SERVER_NAME = "seatunnel-seatunnelserver";
    private static final int DEFAULT_SCALE_UP_REPLICAS = 3;
    private static final int DEFAULT_SCALE_DOWN_REPLICAS = 1;

    private ClusterInfoService getClusterInfoService() {
        return SpringTool.getApplicationContext().getBean(ClusterInfoService.class);
    }

    private boolean isAutoScaleEnabled(int clusterId) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        return Boolean.parseBoolean(globalVariables.get("${enableAutoScale}"));
    }

    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void scaleUp() {
        int clusterId = PropertyUtils.getInt("clusterId");
        if (!isAutoScaleEnabled(clusterId)) {
            return;
        }
        String kubeConfig = getClusterInfoService().getKubeConfigByClusterId(clusterId);
        K8sUtil.scaleStatefulSet(
                kubeConfig,
                K8S_NAMESPACE,
                SEATUNNEL_SERVER_NAME,
                DEFAULT_SCALE_UP_REPLICAS,
                "工作日早9点扩容"
        );

    }

    @Scheduled(cron = "0 0 18 * * MON-FRI")
    public void scaleDown() {
        int clusterId = PropertyUtils.getInt("clusterId");
        if (!isAutoScaleEnabled(clusterId)) {
            return;
        }
        String kubeConfig = getClusterInfoService().getKubeConfigByClusterId(clusterId);
        K8sUtil.scaleStatefulSet(
                kubeConfig,
                K8S_NAMESPACE,
                SEATUNNEL_SERVER_NAME,
                DEFAULT_SCALE_DOWN_REPLICAS,
                "工作日晚6点缩容"
        );

    }

    @Override
    public Result createAutoScaleTask(AutoScaleTaskVO taskVO) {
        //saveAutoScaleConfig(taskVO.getClusterId(), taskVO.getScaleType());
        saveAutoScaleConfig(taskVO.getClusterId(), "true");
        return Result.success();
    }

    @Override
    public Result updateAutoScaleTask(AutoScaleTaskVO taskVO) {
        //saveAutoScaleConfig(taskVO.getClusterId(), taskVO.getScaleType());
        saveAutoScaleConfig(taskVO.getClusterId(), "false");
        return Result.success();
    }

    private void saveAutoScaleConfig(Integer clusterId, String scaleType) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${enableAutoScale}", scaleType);
    }

    @Override
    public Result getAutoScaleTasks(AutoScaleTaskVO taskVO) {
        Map<String, String> globalVariables = GlobalVariables.get(taskVO.getClusterId());
        return  Result.success(globalVariables.get("${enableAutoScale}") != null ? globalVariables.get("${enableAutoScale}") : "false");
    }

    @Override
    public Result deleteAutoScaleTask(AutoScaleTaskVO taskVO) {
        return null;
    }
}