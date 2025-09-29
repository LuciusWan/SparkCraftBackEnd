package com.lucius.sparkcraftbackend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent;
import com.lucius.sparkcraftbackend.exception.SseExceptionHandler;
import com.lucius.sparkcraftbackend.service.WorkflowProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 工作流进度服务实现类
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Slf4j
@Service
public class WorkflowProgressServiceImpl implements WorkflowProgressService {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 存储SSE连接的Map，key为jobId，value为SseEmitter
     */
    private final ConcurrentHashMap<String, SseEmitter> connections = new ConcurrentHashMap<>();

    /**
     * 存储连接创建时间的Map，用于清理过期连接
     */
    private final ConcurrentHashMap<String, Long> connectionTimes = new ConcurrentHashMap<>();
    
    /**
     * 存储imageProjectId到jobId的映射关系
     */
    private final ConcurrentHashMap<String, String> imageProjectToJobMapping = new ConcurrentHashMap<>();
    
    /**
     * 支持广播模式：一个工作流多个连接
     */
    private final ConcurrentHashMap<String, java.util.Set<SseEmitter>> workflowConnections = new ConcurrentHashMap<>();

    /**
     * 定时清理过期连接的执行器
     */
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * SSE连接超时时间（30分钟）
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 连接过期时间（1小时）
     */
    private static final long CONNECTION_EXPIRE_TIME = 60 * 60 * 1000L;

    public WorkflowProgressServiceImpl() {
        // 每5分钟清理一次过期连接
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredConnections, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public SseEmitter createConnection(String imageProjectId, Long userId) {
        log.info("🔗 创建SSE连接，imageProjectId: {}, UserId: {}", imageProjectId, userId);

        // 检查是否已有该imageProjectId对应的jobId连接
        String existingJobId = imageProjectToJobMapping.get(imageProjectId);
        if (existingJobId != null && connections.containsKey(existingJobId)) {
            closeConnection(existingJobId);
        }

        // 创建新的SSE连接
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 设置连接事件处理器
        emitter.onCompletion(() -> {
            log.info("✅ SSE连接完成，imageProjectId: {}", imageProjectId);
            cleanupConnectionByImageProjectId(imageProjectId);
        });

        emitter.onTimeout(() -> {
            log.warn("⏰ SSE连接超时，imageProjectId: {}", imageProjectId);
            SseExceptionHandler.handleConnectionTimeoutException(imageProjectId);
            cleanupConnectionByImageProjectId(imageProjectId);
        });

        emitter.onError((throwable) -> {
            log.error("❌ SSE连接错误，imageProjectId: {}", imageProjectId, throwable);
            SseExceptionHandler.handleSseConnectionException(imageProjectId, (Exception) throwable, emitter);
            cleanupConnectionByImageProjectId(imageProjectId);
        });

        // 暂时使用imageProjectId作为key存储连接，等待jobId映射
        connections.put(imageProjectId, emitter);
        connectionTimes.put(imageProjectId, System.currentTimeMillis());

        // 发送连接成功消息
        try {
            WorkflowProgressEvent connectEvent = WorkflowProgressEvent.builder()
                    .eventType(WorkflowProgressEvent.EventType.WORKFLOW_STARTED)
                    .imageProjectId(Long.valueOf(imageProjectId))
                    .status(WorkflowProgressEvent.NodeStatus.PENDING)
                    .progress(0)
                    .message("SSE连接已建立，等待工作流开始...")
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            String eventData = objectMapper.writeValueAsString(connectEvent);
            emitter.send(SseEmitter.event()
                    .name("workflow-progress")
                    .data(eventData));

            log.info("📡 SSE连接建立成功，imageProjectId: {}", imageProjectId);
        } catch (java.io.IOException e) {
            log.error("❌ 发送连接成功消息失败，imageProjectId: {}", imageProjectId, e);
            SseExceptionHandler.handleSseEventSendException(imageProjectId, e, emitter);
            cleanupConnectionByImageProjectId(imageProjectId);
        }

        return emitter;
    }

    @Override
    public void sendProgressEvent(WorkflowProgressEvent event) {
        String jobId = event.getJobId();
        String imageProjectId = event.getImageProjectId() != null ? event.getImageProjectId().toString() : null;
        
        // 首先尝试通过jobId查找连接
        SseEmitter emitter = connections.get(jobId);
        
        // 如果通过jobId找不到，尝试通过imageProjectId查找
        if (emitter == null && imageProjectId != null) {
            emitter = connections.get(imageProjectId);
            if (emitter != null) {
                // 建立jobId到imageProjectId的映射关系
                log.info("🔗 建立映射关系: jobId {} -> imageProjectId {}", jobId, imageProjectId);
                imageProjectToJobMapping.put(imageProjectId, jobId);
                
                // 将连接从imageProjectId迁移到jobId
                connections.remove(imageProjectId);
                connectionTimes.remove(imageProjectId);
                connections.put(jobId, emitter);
                connectionTimes.put(jobId, System.currentTimeMillis());
            }
        }

        if (emitter == null) {
            log.warn("⚠️ 未找到JobId: {} 或 ImageProjectId: {} 对应的SSE连接", jobId, imageProjectId);
            return;
        }

        try {
            String eventData = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event()
                    .name("workflow-progress")
                    .data(eventData));

            log.info("📡 发送进度事件成功，JobId: {}, 事件类型: {}, 消息: {}", 
                    jobId, event.getEventType(), event.getMessage());

            // 如果是工作流完成或失败，关闭连接
            if (event.getEventType() == WorkflowProgressEvent.EventType.WORKFLOW_COMPLETED ||
                event.getEventType() == WorkflowProgressEvent.EventType.WORKFLOW_FAILED) {
                
                // 延迟3秒后关闭连接，确保前端能收到最后的消息
                cleanupExecutor.schedule(() -> closeConnection(jobId), 3, TimeUnit.SECONDS);
            }

        } catch (IOException e) {
            log.error("❌ 发送进度事件失败，JobId: {}", jobId, e);
            closeConnection(jobId);
        }
    }

    @Override
    public void closeConnection(String jobId) {
        SseEmitter emitter = connections.remove(jobId);
        connectionTimes.remove(jobId);

        if (emitter != null) {
            try {
                emitter.complete();
                log.info("🔚 SSE连接已关闭，JobId: {}", jobId);
            } catch (Exception e) {
                log.warn("⚠️ 关闭SSE连接时发生异常，JobId: {}", jobId, e);
            }
        }
    }

    @Override
    public void cleanupExpiredConnections() {
        long currentTime = System.currentTimeMillis();
        int cleanedCount = 0;

        for (String jobId : connectionTimes.keySet()) {
            Long connectionTime = connectionTimes.get(jobId);
            if (connectionTime != null && (currentTime - connectionTime) > CONNECTION_EXPIRE_TIME) {
                closeConnection(jobId);
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info("🧹 清理了 {} 个过期的SSE连接", cleanedCount);
        }
    }

    /**
     * 获取当前活跃连接数
     */
    public int getActiveConnectionCount() {
        return connections.size();
    }

    /**
     * 检查指定jobId是否有活跃连接
     */
    public boolean hasActiveConnection(String jobId) {
        return connections.containsKey(jobId);
    }
    
    /**
     * 添加连接到广播模式
     */
    public void addConnectionToBroadcast(String jobId, SseEmitter emitter) {
        workflowConnections.computeIfAbsent(jobId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        log.info("📡 将连接添加到广播模式，JobId: {}", jobId);
    }
    
    /**
     * 仏广播模式移除连接
     */
    public void removeConnectionFromBroadcast(String jobId, SseEmitter emitter) {
        java.util.Set<SseEmitter> emitters = workflowConnections.get(jobId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                workflowConnections.remove(jobId);
            }
            log.info("📡 仏广播模式移除连接，JobId: {}", jobId);
        }
    }
    
    /**
     * 获取所有活跃连接的统计信息
     */
    public java.util.Map<String, Object> getConnectionStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalConnections", connections.size());
        stats.put("broadcastGroups", workflowConnections.size());
        stats.put("mappings", imageProjectToJobMapping.size());
        
        int totalBroadcastConnections = workflowConnections.values().stream()
                .mapToInt(java.util.Set::size)
                .sum();
        stats.put("totalBroadcastConnections", totalBroadcastConnections);
        
        return stats;
    }
    
    /**
     * 验证和清理无效连接
     */
    public void validateAndCleanupConnections() {
        int cleanedCount = 0;
        
        // 清理无效的单个连接
        java.util.Iterator<java.util.Map.Entry<String, SseEmitter>> iterator = connections.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<String, SseEmitter> entry = iterator.next();
            try {
                // 发送一个心跳消息来检查连接是否有效
                entry.getValue().send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (Exception e) {
                log.warn("📋 发现无效连接，清理: {}", entry.getKey());
                iterator.remove();
                connectionTimes.remove(entry.getKey());
                cleanedCount++;
            }
        }
        
        // 清理广播组中的无效连接
        workflowConnections.forEach((jobId, emitters) -> {
            emitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                    return false;
                } catch (Exception e) {
                    try {
                        emitter.complete();
                    } catch (Exception closeEx) {
                        // 忽略关闭异常
                    }
                    return true;
                }
            });
        });
        
        // 移除空的广播组
        workflowConnections.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        if (cleanedCount > 0) {
            log.info("🧽 清理了 {} 个无效连接", cleanedCount);
        }
    }
    
    /**
     * 通过imageProjectId清理连接
     */
    private void cleanupConnectionByImageProjectId(String imageProjectId) {
        // 清理直接的imageProjectId连接
        connections.remove(imageProjectId);
        connectionTimes.remove(imageProjectId);
        
        // 查找并清理映射的jobId连接
        String mappedJobId = imageProjectToJobMapping.get(imageProjectId);
        if (mappedJobId != null) {
            connections.remove(mappedJobId);
            connectionTimes.remove(mappedJobId);
            imageProjectToJobMapping.remove(imageProjectId);
        }
    }
}