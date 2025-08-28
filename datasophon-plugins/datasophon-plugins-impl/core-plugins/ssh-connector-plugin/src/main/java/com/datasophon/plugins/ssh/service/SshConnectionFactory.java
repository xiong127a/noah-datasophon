/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.plugins.ssh.service;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;


import java.util.concurrent.TimeUnit;

/**
 * SSH连接工厂（插件内部组件）
 * 
 * 用于Commons Pool2管理SSH连接的生命周期
 * 严格限制在插件内部使用，主程序不直接访问
 * 
 * @author 任相鹏
 * @email 635887935@qq.com  
 * @date 2025-08-28
 */
@Slf4j
public class SshConnectionFactory extends BasePooledObjectFactory<SSHClient> {
    
    private final String hostIp;
    private final int port;
    private final String user;
    private final String password;
    
    // 连接配置
    private static final int CONNECT_TIMEOUT = 30_000;  // 30秒连接超时
    private static final int AUTH_TIMEOUT = 30_000;     // 30秒认证超时
    private static final String TEST_COMMAND = "echo 'pool_test'"; // 连接测试命令
    
    public SshConnectionFactory(String hostIp, int port, String user, String password) {
        this.hostIp = hostIp;
        this.port = port;
        this.user = user;
        this.password = password;
    }
    
    /**
     * 创建SSH连接（插件内部方法）
     */
    @Override
    public SSHClient create() throws Exception {
        log.debug("【插件内部】创建SSH连接: {}@{}:{}", user, hostIp, port);
        
        SSHClient client = new SSHClient();
        
        try {
            // 配置SSH客户端
            configureClient(client);
            
            // 建立连接
            client.connect(hostIp, port);
            
            // 身份验证
            client.authPassword(user, password);
            
            log.debug("【插件内部】SSH连接创建成功: {}@{}:{}", user, hostIp, port);
            return client;
            
        } catch (Exception e) {
            // 创建失败时清理资源
            try {
                client.close();
            } catch (Exception closeEx) {
                log.warn("【插件内部】关闭失败的SSH客户端时发生错误", closeEx);
            }
            
            log.error("【插件内部】创建SSH连接失败: {}@{}:{}, 错误: {}", user, hostIp, port, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 包装连接对象（插件内部方法）
     */
    @Override
    public PooledObject<SSHClient> wrap(SSHClient client) {
        return new DefaultPooledObject<>(client);
    }
    
    /**
     * 验证连接是否可用（插件内部方法）
     */
    @Override
    public boolean validateObject(PooledObject<SSHClient> pooledObject) {
        SSHClient client = pooledObject.getObject();
        
        try {
            // 检查连接状态
            if (!client.isConnected() || !client.isAuthenticated()) {
                log.debug("【插件内部】SSH连接验证失败: 连接状态异常 {}@{}:{}", user, hostIp, port);
                return false;
            }
            
            // 执行测试命令
            try (Session session = client.startSession()) {
                Session.Command cmd = session.exec(TEST_COMMAND);
                cmd.join(5, TimeUnit.SECONDS);  // 5秒超时
                
                boolean success = cmd.getExitStatus() == 0;
                if (success) {
                    log.debug("【插件内部】SSH连接验证成功: {}@{}:{}", user, hostIp, port);
                } else {
                    log.debug("【插件内部】SSH连接验证失败: 测试命令执行失败 {}@{}:{}", user, hostIp, port);
                }
                
                return success;
            }
            
        } catch (Exception e) {
            log.debug("【插件内部】SSH连接验证异常: {}@{}:{}, 错误: {}", user, hostIp, port, e.getMessage());
            return false;
        }
    }
    
    /**
     * 销毁连接（插件内部方法）
     */
    @Override
    public void destroyObject(PooledObject<SSHClient> pooledObject) throws Exception {
        SSHClient client = pooledObject.getObject();
        
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
                log.debug("【插件内部】SSH连接已销毁: {}@{}:{}", user, hostIp, port);
            } catch (Exception e) {
                log.warn("【插件内部】销毁SSH连接时发生错误: {}@{}:{}, 错误: {}", user, hostIp, port, e.getMessage());
            }
        }
    }
    
    /**
     * 激活连接（从池中取出时调用）（插件内部方法）
     */
    @Override
    public void activateObject(PooledObject<SSHClient> pooledObject) throws Exception {
        SSHClient client = pooledObject.getObject();
        
        // 检查连接状态，必要时重新连接
        if (!client.isConnected()) {
            log.debug("【插件内部】重新激活SSH连接: {}@{}:{}", user, hostIp, port);
            client.connect(hostIp, port);
            client.authPassword(user, password);
        }
    }
    
    /**
     * 钝化连接（归还到池中时调用）（插件内部方法）
     */
    @Override
    public void passivateObject(PooledObject<SSHClient> pooledObject) throws Exception {
        // 通常不需要特殊处理，SSHJ会自动管理会话
        log.debug("【插件内部】钝化SSH连接: {}@{}:{}", user, hostIp, port);
    }
    
    // ================== 私有方法 ==================
    
    /**
     * 配置SSH客户端（插件内部方法）
     */
    private void configureClient(SSHClient client) throws Exception {
        // 设置主机密钥验证器（生产环境应该使用更安全的验证器）
        client.addHostKeyVerifier(new PromiscuousVerifier());
        
        // 设置连接超时
        client.setConnectTimeout(CONNECT_TIMEOUT);
        client.setTimeout(AUTH_TIMEOUT);
        
        // 启用压缩（可选）
        client.useCompression();
        
        log.debug("【插件内部】SSH客户端配置完成: 连接超时={}ms, 认证超时={}ms", CONNECT_TIMEOUT, AUTH_TIMEOUT);
    }
}
