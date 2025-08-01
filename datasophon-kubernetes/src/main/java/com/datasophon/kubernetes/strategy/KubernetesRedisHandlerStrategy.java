package com.datasophon.kubernetes.strategy;

import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.TypeRefs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubeUtil;
import com.datasophon.kubernetes.util.KubernetesUtil;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KubernetesRedisHandlerStrategy extends KubernetesAbstractHandlerStrategy
        implements KubernetesServiceRoleStrategy {

    public KubernetesRedisHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();
        ExecResult startResult;

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            startResult = serviceHandler.start(command);
            List<String> podNames = CacheUtils.getGeneric(serviceRoleFullName + "_" + Constants.POD_NAME, TypeRefs.LIST_STRING);


            if (podNames == null || podNames.isEmpty()) {
                return startResult;
            }

            ArrayList<String> commands = new ArrayList<>();
            commands.add("sh");
            commands.add(workPath + "/redis-cluster.sh");
            try (KubernetesClient kubeClient = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                startResult = KubernetesUtil.runCmd(
                        command.getNamespace(),
                        kubeClient,
                        (command.getServiceName() + "-" + command.getServiceRoleName()).toLowerCase(),
                        command.getHostname(),
                        String.join(" ", commands));
                logger.info("sh redis-cluster.sh success");
            } catch (Exception e) {
                logger.info("sh redis-cluster.sh failed");
                startResult.setExecResult(false);
                return startResult;
            }
            return startResult;
        }
        startResult = serviceHandler.start(command);
        return startResult;
    }

    @Override
    public void getConfig(Integer clusterId, String namespace, List<ServiceConfig> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Redis配置列表为空，无法更新服务地址");
            return;
        }

        logger.info("开始更新Redis配置，适配Kubernetes服务发现...");

        for (ServiceConfig config : list) {
            String name = config.getName();
            if (name == null || config.getValue() == null) {
                continue;
            }

            try {
                if ("RedisMasterAddr".equals(name)) {
                    String value = (String) config.getValue();
                    StringBuilder newValue = new StringBuilder();
                    String[] split = value.split(" ");
                    for (int i = 0; i < split.length; i++) {
                        newValue.append("redis-redismaster-")
                                .append(i)
                                .append(".redis-redismaster.datasophon.svc.cluster.local:7000 ");
                    }
                    if (StrUtil.isNotBlank(newValue)) {
                        config.setValue(newValue.substring(0, newValue.length() - 1));
                        logger.info("RedisMasterAddr配置已更新为Kubernetes服务地址: {}", config.getValue());
                    }
                } else if ("RedisSlaveAddr".equals(name)) {
                    String value = (String) config.getValue();
                    StringBuilder newValue = new StringBuilder();
                    String[] split = value.split(" ");

                    List<String> workerList = Arrays.asList(split);
                    List<String> adjustedWorker = new ArrayList<>(workerList);

                    List<String> masterList = new ArrayList<>();
                    for (int i = 0; i < split.length; i++) {
                        masterList.add("redis-redismaster-" + i + ".redis-redismaster.datasophon.svc.cluster.local");
                    }

                    // 循环位移直到无冲突或尝试次数耗尽
                    int maxAttempts = adjustedWorker.size();
                    boolean conflictFound;
                    int attempts = 0;
                    do {
                        conflictFound = false;
                        // 检查所有下标，现在假设主从节点列表长度一致
                        for (int i = 0; i < masterList.size(); i++) {
                            String masterHost = masterList.get(i);
                            String workerHost = adjustedWorker.get(i);
                            if (masterHost.equals(workerHost)) {
                                conflictFound = true;
                                break;
                            }
                        }

                        if (conflictFound) {
                            Collections.rotate(adjustedWorker, 1);
                            attempts++;
                            logger.info("检测到主从节点冲突，执行第{}次位移调整", attempts);
                        }
                    } while (conflictFound && attempts < maxAttempts);

                    // 构建新的worker地址
                    for (int i = 0; i < adjustedWorker.size(); i++) {
                        newValue.append("redis-redisworker-")
                                .append(i)
                                .append(".redis-redisworker.datasophon.svc.cluster.local:7001 ");
                    }

                    if (StrUtil.isNotBlank(newValue)) {
                        config.setValue(newValue.substring(0, newValue.length() - 1));
                        logger.info("RedisSlaveAddr配置已更新为Kubernetes服务地址(经过冲突调整): {}", config.getValue());
                    }
                }
            } catch (Exception e) {
                logger.error("更新Redis服务地址配置失败", e);
            }
        }
        logger.info("Redis配置更新完成");
    }
}
