package com.datasophon.api.service;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * K8s模式的增量日志跟踪服务
 * 使用Apache Commons IO Tailer + JDK21虚拟线程
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-18
 */
@Service
public class LogTailService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogTailService.class);
    
    // JDK21 虚拟线程支持
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Tailer> activeTailers = new ConcurrentHashMap<>();
    
    /**
     * 日志会话信息 - JDK21 record
     */
    public record LogSession(String sessionKey, String username, String logFile, Consumer<String> onNewContent) {}
    
    /**
     * 开始跟踪日志文件（仅K8s模式）
     */
    public void startTailing(LogSession session) {
        var logFile = new File(session.logFile());
        
        // 如果文件不存在，先创建空文件
        if (!logFile.exists()) {
            try {
                Files.createDirectories(logFile.getParentFile().toPath());
                Files.createFile(logFile.toPath());
                logger.info("创建日志文件: {}", session.logFile());
            } catch (Exception e) {
                logger.error("创建日志文件失败: {}", session.logFile(), e);
                return;
            }
        }
        
        // 停止已有的跟踪（如果存在）
        stopTailing(session.sessionKey());
        
        // 创建Tailer监听器 - 只处理新增内容
        var listener = new TailerListener() {
            @Override
            public void init(Tailer tailer) {
                logger.debug("开始跟踪日志文件: {}", session.logFile());
            }
            
            @Override
            public void fileNotFound() {
                logger.warn("日志文件未找到: {}", session.logFile());
            }
            
            @Override
            public void fileRotated() {
                logger.info("日志文件轮转: {}", session.logFile());
            }
            
            @Override
            public void handle(String line) {
                // 只有新的日志行才推送（增量机制的核心）
                session.onNewContent().accept(line + "\n");
            }
            
            @Override
            public void handle(Exception ex) {
                logger.error("日志文件跟踪异常: {}", session.logFile(), ex);
            }
        };
        
        // 创建Tailer（从文件末尾开始，只读取新内容）
        var tailer = Tailer.builder()
                .setFile(logFile)
                .setTailerListener(listener)
                .setDelayDuration(java.time.Duration.ofMillis(500)) // 500ms检查一次
                .setStartThread(false) // 手动启动
                .get();
        
        activeTailers.put(session.sessionKey(), tailer);
        
        // 使用JDK21虚拟线程启动
        executor.submit(tailer::run);
        
        logger.info("启动增量日志跟踪: sessionKey={}, file={}", session.sessionKey(), session.logFile());
    }
    
    /**
     * 停止跟踪日志文件
     */
    public void stopTailing(String sessionKey) {
        var tailer = activeTailers.remove(sessionKey);
        if (tailer != null) {
            tailer.close();
            logger.info("停止日志跟踪: sessionKey={}", sessionKey);
        }
    }
    
    /**
     * 获取文件的最后N行（首次加载历史日志用）
     */
    public String getLastLines(String logFile, int lines) {
        try {
            var path = Path.of(logFile);
            if (!Files.exists(path)) {
                return "";
            }
            
            // JDK21 简化的文件读取
            var allLines = Files.readAllLines(path);
            if (allLines.isEmpty()) {
                return "";
            }
            
            var startIndex = Math.max(0, allLines.size() - lines);
            return String.join("\n", allLines.subList(startIndex, allLines.size()));
            
        } catch (Exception e) {
            logger.error("读取日志文件失败: {}", logFile, e);
            return "";
        }
    }
}
