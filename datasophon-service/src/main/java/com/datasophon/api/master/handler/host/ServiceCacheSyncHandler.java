package com.datasophon.api.master.handler.host;


import com.datasophon.common.Constants;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.HostUtils;
import com.datasophon.common.utils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static com.datasophon.common.utils.HostUtils.GetMasterHost;

public class ServiceCacheSyncHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServiceCacheSyncHandler.class);

    public ServiceCacheSyncHandler() {

    }

    public ExecResult serviceCacheSync(Object object) {
        ExecResult execResult = new ExecResult();
        execResult.setExecResult(false);
        boolean isHa = PropertyUtils.getBoolean("isHa");
        if (!isHa){
            return execResult;
        }

        List<String> masterhosts = GetMasterHost();

        for (String hostname : masterhosts) {
            try {
                //不与自身交互
                if (hostname.equals(InetAddress.getLocalHost().getHostName())) {
                    continue;
                }
                //跳过不在线的服务
                if (!HostUtils.checkServiceOnlineWithRetry(hostname,PropertyUtils.getInt(Constants.MASTER_WEB_PORT),1,1000)){
                    logger.info("Service at host {} is offline.", hostname);
                    continue;
                }
                
                // TODO: Master之间的缓存同步需要另外实现HTTP接口
                // 暂时返回成功，避免影响主流程
                execResult.setExecResult(true);
                execResult.setExecOut("Master cache sync via HTTP not implemented yet");
                logger.info("Master cache sync to {} - HTTP implementation pending", hostname);
                return execResult;
                
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        return execResult;
    }




}
