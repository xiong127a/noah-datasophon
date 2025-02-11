package com.datasophon.common.utils;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class JournalNodeClusterUtils {

    // 默认配置常量（统一使用秒）
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_TIMEOUT = 5;       // 默认超时5秒
    private static final int DEFAULT_RETRY_INTERVAL = 1; // 默认重试间隔1秒

    private JournalNodeClusterUtils() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }
    public static List<String> buildJournalNodeUrls(String journalNodes,int port) {
        // 1. 分割逗号分隔的主机列表
        return Stream.of(journalNodes.split(","))
                // 2. 去除可能存在的空格
                .map(String::trim)
                // 3. 拼接成完整URL格式
                .map(host -> String.format("http://%s:%s", host,port))
                // 4. 收集到List中
                .collect(Collectors.toList());
    }

    /**
     * 增强版重试拦截器（优化异常处理）
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        private final int retryIntervalSec;

        RetryInterceptor(int maxRetries, int retryIntervalSec) {
            this.maxRetries = maxRetries;
            this.retryIntervalSec = retryIntervalSec;
        }
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            IOException exception = null;
            int attempt = 0;

            while (attempt <= maxRetries) {
                attempt++;
                try {
                    // 只要获得响应即视为成功（不检查状态码）
                    return chain.proceed(request);
                } catch (IOException e) {
                    exception = e;
                    log.warn("⚠ 网络错误 ({}/{}): {}", attempt, maxRetries, e.getMessage());
                }

                if (attempt <= maxRetries) {
                    sleepSafely(retryIntervalSec);
                }
            }

            throw exception != null ? exception
                    : new IOException("请求失败，已重试" + maxRetries + "次");
        }

        private void sleepSafely(int seconds) {
            try {
                TimeUnit.SECONDS.sleep(seconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("重试等待被中断");
            }
        }
    }

    /**
     * 创建HTTP客户端（秒级参数）
     */
    private static OkHttpClient createHttpClient(int timeoutSec, int maxRetries, int retryIntervalSec) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeoutSec, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .writeTimeout(timeoutSec, TimeUnit.SECONDS)
                .addInterceptor(new RetryInterceptor(maxRetries, retryIntervalSec))
                .build();
    }

    // 方法重载版本
    public static boolean checkJournalNodeClusterAvailability(List<String> journalNodeUrls) {
        return checkJournalNodeClusterAvailability(journalNodeUrls, DEFAULT_TIMEOUT, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_INTERVAL);
    }

    public static boolean checkJournalNodeClusterAvailability(List<String> journalNodeUrls,
                                                              int timeoutSec,
                                                              int maxRetries) {
        return checkJournalNodeClusterAvailability(journalNodeUrls, timeoutSec, maxRetries, DEFAULT_RETRY_INTERVAL);
    }

    /**
     * 完整参数版本（秒级控制）
     * @param retryIntervalSec 重试间隔时间（秒）
     */
    public static boolean checkJournalNodeClusterAvailability(
            List<String> journalNodeUrls,
            int timeoutSec,
            int maxRetries,
            int retryIntervalSec
    ) {
        validateParameters(journalNodeUrls, timeoutSec, maxRetries, retryIntervalSec);

        final int totalNodes = journalNodeUrls.size();
        final int requiredSuccesses = (totalNodes / 2) + 1;
        int successfulNodes = 0;

        log.info("开始JournalNode集群检查 | 超时: {}秒 | 重试次数: {} | 重试间隔: {}秒",
                timeoutSec, maxRetries, retryIntervalSec);
        log.info("JournalNode集群健康要求: {}/{} 个节点健康", requiredSuccesses, totalNodes);

        OkHttpClient client = createHttpClient(timeoutSec, maxRetries, retryIntervalSec);

        for (String url : journalNodeUrls) {
            log.info("▷ 正在检查节点: {}", url);
            if (checkNode(client, url)) {
                successfulNodes++;
            }
        }

        boolean isHealthy = successfulNodes >= requiredSuccesses;
        log.info("检查结果: {}/{} 个节点健康 | 集群状态: {}",
                successfulNodes, totalNodes, isHealthy ? "健康" : "不健康");
        return isHealthy;
    }

    /**
     * 参数校验（秒级校验）
     */
    private static void validateParameters(List<String> urls,
                                           int timeoutSec,
                                           int maxRetries,
                                           int retryIntervalSec) {
        if (CollUtil.isEmpty(urls)) {
            throw new IllegalArgumentException("节点URL列表不能为空");
        }
        if (timeoutSec <= 0) {
            throw new IllegalArgumentException("超时时间必须大于0");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("重试次数不能为负数");
        }
        if (retryIntervalSec <= 0) {
            throw new IllegalArgumentException("重试间隔必须大于0");
        }
    }

    /**
     * 节点健康检查
     */
    private static boolean checkNode(OkHttpClient client, String url) {
        try {
            // 使用GET方法并添加容错处理
            Request request = new Request.Builder()
                    .url(url)
                    .get()  // 明确使用GET方法
                    .build();

            try (Response response = client.newCall(request).execute()) {
                // 只要收到响应（无论状态码）即视为成功
                log.debug("✓ [{}] 服务可达，状态码: {}", url, response.code());
                return true;
            }
        } catch (IllegalArgumentException e) {
            log.error("✗ 非法URL格式: {}", url);
        } catch (IOException e) {
            handleConnectException(url, e);
        }
        return false;
    }
    /**
     * 增强版异常处理
     */
    private static void handleConnectException(String url, IOException e) {
        String errorMsg = e.getMessage();

        // 特殊处理流意外结束错误
        if (errorMsg.contains("unexpected end of stream")) {
            log.warn("⚠ [{}] 连接异常终止（已建立连接但响应不完整）", url);
        }
        // 处理连接超时
        else if (errorMsg.contains("timeout")) {
            log.error("⌛ [{}] 连接超时", url);
        }
        // 处理拒绝连接
        else if (errorMsg.contains("refused")) {
            log.error("⛔ [{}] 连接被拒绝", url);
        }
        // 其他网络错误
        else {
            log.error("❌ [{}] 网络错误: {}", url, errorMsg);
        }
    }
}
