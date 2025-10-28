package com.datasophon.api.controller.v1;

import com.datasophon.api.agent.AgentStateManager;
import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.event.AgentDistributionStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent分发状态SSE Controller
 * 实时推送Agent分发状态更新（替代轮询）
 * 
 * @author DataSophon Team
 * @date 2025-10-28
 */
@Slf4j
@RestController
@ApiVersion(path = "sse/agent-distribution-status")
@RequiredArgsConstructor
public class AgentDistributionStatusSSEController implements ApplicationListener<AgentDistributionStatusChangeEvent> {

    private final AgentStateManager stateManager;

    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    /**
     * 建立Agent分发状态SSE连接
     * 
     * @param clusterId 集群ID
     * @return SSE emitter
     */
    @GetMapping(path = "/stream/{clusterId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDistributionStatus(@PathVariable Long clusterId) {

        log.info("建立Agent分发状态SSE连接: 集群ID={}", clusterId);

        var emitter = new SseEmitter(30 * 60 * 1000L); // 30分钟超时
        var emitterKey = clusterId.toString();

        activeEmitters.put(emitterKey, emitter);

        emitter.onCompletion(() -> {
            log.info("Agent分发状态SSE连接完成: 集群ID={}", clusterId);
            cleanupConnection(emitterKey);
        });

        emitter.onTimeout(() -> {
            log.info("Agent分发状态SSE连接超时: 集群ID={}", clusterId);
            cleanupConnection(emitterKey);
            emitter.complete();
        });

        emitter.onError(e -> {
            log.error("Agent分发状态SSE连接错误: 集群ID={}, 错误={}", clusterId, e.getMessage());
            cleanupConnection(emitterKey);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("message", "连接建立成功", "clusterId", clusterId)));
        } catch (IOException e) {
            log.error("发送初始消息失败: {}", e.getMessage());
        }

        // 首次连接时立即推送一次当前状态
        pushStatusUpdate(clusterId, emitter);

        return emitter;
    }

    /**
     * 监听Agent分发状态变更事件
     */
    @Override
    public void onApplicationEvent(@NonNull AgentDistributionStatusChangeEvent event) {
        Long clusterId = event.getClusterId();
        String emitterKey = clusterId.toString();
        SseEmitter emitter = activeEmitters.get(emitterKey);

        if (emitter != null) {
            log.debug("收到Agent分发状态变更事件: 集群={}, 主机={}", clusterId, event.getHostIp());
            pushStatusUpdate(clusterId, emitter);
        } else {
            log.trace("没有活跃的SSE连接: 集群={}", clusterId);
        }
    }

    /**
     * 推送状态更新
     */
    private void pushStatusUpdate(Long clusterId, SseEmitter emitter) {
        try {
            var statusList = stateManager.getClusterStatus(clusterId);

            var data = Map.of(
                    "type", "status",
                    "data", statusList
            );

            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(data));

            log.debug("推送Agent分发状态更新: 集群={}, 主机数={}", clusterId, statusList.size());
        } catch (IOException e) {
            log.warn("SSE连接已断开: 集群={}", clusterId);
            activeEmitters.remove(clusterId.toString());
        } catch (Exception e) {
            log.error("推送状态失败: 集群={}, 错误={}", clusterId, e.getMessage(), e);
        }
    }

    /**
     * 清理连接
     */
    private void cleanupConnection(String emitterKey) {
        activeEmitters.remove(emitterKey);
        log.debug("清理Agent分发SSE连接缓存: key={}", emitterKey);
    }
}

