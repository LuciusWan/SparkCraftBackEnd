package com.lucius.sparkcraftbackend.service;

import com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 工作流进度服务接口
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
public interface WorkflowProgressService {

    /**
     * 创建SSE连接
     *
     * @param jobId 任务ID
     * @param userId 用户ID
     * @return SSE发射器
     */
    SseEmitter createConnection(String jobId, Long userId);

    /**
     * 发送进度事件
     *
     * @param event 进度事件
     */
    void sendProgressEvent(WorkflowProgressEvent event);

    /**
     * 关闭连接
     *
     * @param jobId 任务ID
     */
    void closeConnection(String jobId);

    /**
     * 清理过期连接
     */
    void cleanupExpiredConnections();
}