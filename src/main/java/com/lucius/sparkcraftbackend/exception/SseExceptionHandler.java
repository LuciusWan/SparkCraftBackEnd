package com.lucius.sparkcraftbackend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE相关异常处理器
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Slf4j
public class SseExceptionHandler {

    /**
     * 处理SSE连接异常
     */
    public static void handleSseConnectionException(String connectionId, Exception e, SseEmitter emitter) {
        log.error("❌ SSE连接异常，连接ID: {}", connectionId, e);
        
        try {
            if (emitter != null) {
                // 发送错误事件
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("连接异常: " + e.getMessage()));
                
                // 完成连接
                emitter.completeWithError(e);
            }
        } catch (Exception sendException) {
            log.error("发送SSE错误事件失败", sendException);
            try {
                if (emitter != null) {
                    emitter.complete();
                }
            } catch (Exception completeException) {
                log.error("完成SSE连接失败", completeException);
            }
        }
    }

    /**
     * 处理SSE事件发送异常
     */
    public static void handleSseEventSendException(String jobId, Exception e, SseEmitter emitter) {
        log.error("❌ SSE事件发送异常，jobId: {}", jobId, e);
        
        // 尝试重试发送（最多3次）
        int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                Thread.sleep(100 * (i + 1)); // 递增延迟
                emitter.send(SseEmitter.event()
                    .name("retry")
                    .data("重试发送事件，第" + (i + 1) + "次"));
                log.info("✅ SSE事件重试发送成功，jobId: {}, 重试次数: {}", jobId, i + 1);
                return;
            } catch (Exception retryException) {
                log.warn("⚠️ SSE事件重试发送失败，jobId: {}, 重试次数: {}", jobId, i + 1, retryException);
            }
        }
        
        // 重试失败，关闭连接
        try {
            emitter.send(SseEmitter.event()
                .name("error")
                .data("事件发送失败，连接将关闭"));
            emitter.complete();
        } catch (Exception finalException) {
            log.error("最终关闭SSE连接失败", finalException);
        }
    }

    /**
     * 处理工作流执行异常
     */
    public static void handleWorkflowExecutionException(String jobId, Long imageProjectId, Exception e) {
        log.error("❌ 工作流执行异常，jobId: {}, imageProjectId: {}", jobId, imageProjectId, e);
        
        // 这里可以添加额外的错误处理逻辑，比如：
        // 1. 记录到数据库
        // 2. 发送通知
        // 3. 清理资源
        // 4. 发送错误事件等
    }

    /**
     * 处理连接超时异常
     */
    public static void handleConnectionTimeoutException(String connectionId) {
        log.warn("⏰ SSE连接超时，连接ID: {}", connectionId);
        
        // 这里可以添加连接超时的处理逻辑
        // 比如清理资源、记录统计信息等
    }

    /**
     * 安全关闭SSE连接
     */
    public static void safeCloseSseConnection(SseEmitter emitter, String connectionId) {
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("🔚 安全关闭SSE连接，连接ID: {}", connectionId);
            } catch (Exception e) {
                log.warn("⚠️ 关闭SSE连接时发生异常，连接ID: {}", connectionId, e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception errorException) {
                    log.error("完成SSE连接时发生异常", errorException);
                }
            }
        }
    }

    /**
     * 检查连接是否有效
     */
    public static boolean isConnectionValid(SseEmitter emitter) {
        if (emitter == null) {
            return false;
        }
        
        try {
            // 发送心跳测试连接
            emitter.send(SseEmitter.event()
                .name("heartbeat")
                .data("ping"));
            return true;
        } catch (Exception e) {
            log.debug("连接无效: {}", e.getMessage());
            return false;
        }
    }
}