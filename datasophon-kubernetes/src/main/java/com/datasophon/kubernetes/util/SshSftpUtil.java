package com.datasophon.kubernetes.util;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SshSftpUtil {

    private static final Map<String, SshConnectionPool> sshPools = new ConcurrentHashMap<>();
    private static final Map<String, SftpFilesystemPool> sftpPools = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sshPools.values().forEach(SshConnectionPool::close);
            sftpPools.values().forEach(SftpFilesystemPool::close);
        }));
    }

    private static SshConnectionPool getSshPool(String host) {
        return sshPools.computeIfAbsent(host, SshConnectionPool::new);
    }

    private static SftpFilesystemPool getSftpPool(String host) {
        return sftpPools.computeIfAbsent(host, h -> new SftpFilesystemPool(getSshPool(h)));
    }

    public static ClientSession getClientSession(String host) throws Exception {
        return getSshPool(host).borrowObject();
    }

    public static void returnClientSession(String host, ClientSession session) {
        getSshPool(host).returnObject(session);
    }

    public static SftpFileSystem getSftpFileSystem(String host) throws Exception {
        return getSftpPool(host).borrowObject();
    }

    public static void returnSftpFileSystem(String host, SftpFileSystem sftpFileSystem) {
        getSftpPool(host).returnObject(sftpFileSystem);
    }

    public static <T> T withClientSession(String host, SessionFunction<ClientSession, T> function) {
        ClientSession session = null;
        try {
            session = getClientSession(host);
            return function.apply(session);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute operation with SSH session", e);
        } finally {
            if (session != null) {
                returnClientSession(host, session);
            }
        }
    }

    public static <T> T withSftpFileSystem(String host, SessionFunction<SftpFileSystem, T> function) {
        SftpFileSystem sftp = null;
        try {
            sftp = getSftpFileSystem(host);
            return function.apply(sftp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute operation with SFTP file system", e);
        } finally {
            if (sftp != null) {
                returnSftpFileSystem(host, sftp);
            }
        }
    }

    @FunctionalInterface
    public interface SessionFunction<S, R> {
        R apply(S session) throws Exception;
    }

}