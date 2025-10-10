package com.datasophon.kubernetes.util;

import com.datasophon.common.Constants;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.sshd.client.session.ClientSession;

import java.time.Duration;

public class SshConnectionPool {
    private final GenericObjectPool<ClientSession> pool;

    public SshConnectionPool(String host) {
        GenericObjectPoolConfig<ClientSession> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(5);
        config.setMaxIdle(3);
        config.setMinIdle(1);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(Duration.ofMinutes(30));
        config.setMinEvictableIdleTime(Duration.ofMinutes(5));

        pool = new GenericObjectPool<>(new SshConnectionFactory(host), config);
    }

    public SshPoolMetrics poolMetrics() {
        pool.getNumActive();
        pool.getNumIdle();
        pool.getNumWaiters();
        SshPoolMetrics sshPoolMetrics = new SshPoolMetrics(pool.getNumActive(), pool.getNumIdle(), pool.getNumWaiters());
        return sshPoolMetrics;
    }

    public ClientSession borrowObject() throws Exception {
        return pool.borrowObject();
    }

    public void returnObject(ClientSession session) {
        pool.returnObject(session);
    }

    public void close() {
        pool.close();
    }

    private static class SshConnectionFactory extends BasePooledObjectFactory<ClientSession> {
        private final String host;

        public SshConnectionFactory(String host) {
            this.host = host;
        }

        @Override
        public ClientSession create() throws Exception {
            return KubernetesMinaUtils.openConnection(host, 22, Constants.ROOT);
        }

        @Override
        public PooledObject<ClientSession> wrap(ClientSession session) {
            return new DefaultPooledObject<>(session);
        }

        @Override
        public void destroyObject(PooledObject<ClientSession> pooledObject) throws Exception {
            pooledObject.getObject().close();
        }

        @Override
        public boolean validateObject(PooledObject<ClientSession> pooledObject) {
            return pooledObject.getObject().isOpen();
        }
    }
}