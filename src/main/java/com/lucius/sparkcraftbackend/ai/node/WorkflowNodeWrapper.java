package com.lucius.sparkcraftbackend.ai.node;

import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent;
import com.lucius.sparkcraftbackend.service.WorkflowProgressService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Map;

/**
 * 工作流节点包装器，为现有节点提供SSE进度推送功能
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Slf4j
public class WorkflowNodeWrapper {

    private static WorkflowProgressService workflowProgressService;

    /**
     * 设置工作流进度服务
     */
    public static void setWorkflowProgressService(WorkflowProgressService service) {
        workflowProgressService = service;
    }

    /**
     * 包装现有节点，添加SSE进度推送功能
     */
    public static AsyncNodeAction<MessagesState<String>> wrapNode(
            String nodeName, 
            String displayName, 
            int nodeIndex, 
            int totalNodes,
            AsyncNodeAction<MessagesState<String>> originalNode) {
        
        return org.bsc.langgraph4j.action.AsyncNodeAction.node_async(state -> {
            log.info("🔄 开始执行节点: {} ({})", nodeName, displayName);
            
            WorkflowContext context = null;
            try {
                // 获取工作流上下文
                context = WorkflowContext.getContext(state);
                
                // 发送节点开始事件
                sendNodeStartedEvent(context, nodeName, displayName, nodeIndex, totalNodes);
                
                // 执行原节点逻辑并等待结果
                // 注意：这里使用同步模式，因为node_async的lambda期望返回Map
                java.util.concurrent.CompletableFuture<java.util.Map<String, Object>> future = originalNode.apply(state);
                java.util.Map<String, Object> result = future.join(); // 同步等待结果
                
                // 发送节点完成事件
                sendNodeCompletedEvent(context, nodeName, displayName, result, nodeIndex, totalNodes);
                
                log.info("✅ 节点执行完成: {} ({})", nodeName, displayName);
                return result;
                
            } catch (Exception e) {
                log.error("❌ 节点执行失败: {} ({})", nodeName, displayName, e);
                
                // 发送节点失败事件
                sendNodeFailedEvent(context, nodeName, displayName, e.getMessage(), nodeIndex, totalNodes);
                
                throw e;
            }
        });
    }

    /**
     * 发送节点开始事件
     */
    private static void sendNodeStartedEvent(WorkflowContext context, String nodeName, String displayName, 
                                           int nodeIndex, int totalNodes) {
        if (workflowProgressService != null && context != null && context.getJobId() != null) {
            try {
                WorkflowProgressEvent event = WorkflowProgressEvent.nodeStarted(
                    context.getJobId(), 
                    context.getAppId(), 
                    nodeName, 
                    displayName, 
                    nodeIndex, 
                    totalNodes
                );
                workflowProgressService.sendProgressEvent(event);
                log.debug("📡 发送节点开始事件: {} - {}", nodeName, displayName);
            } catch (Exception e) {
                log.warn("⚠️ 发送节点开始事件失败: {}", nodeName, e);
            }
        }
    }

    /**
     * 发送节点完成事件
     */
    private static void sendNodeCompletedEvent(WorkflowContext context, String nodeName, String displayName, 
                                             Object result, int nodeIndex, int totalNodes) {
        if (workflowProgressService != null && context != null && context.getJobId() != null) {
            try {
                WorkflowProgressEvent event = WorkflowProgressEvent.nodeCompleted(
                    context.getJobId(), 
                    context.getAppId(), 
                    nodeName, 
                    displayName, 
                    result, 
                    nodeIndex, 
                    totalNodes
                );
                workflowProgressService.sendProgressEvent(event);
                log.debug("✅ 发送节点完成事件: {} - {}", nodeName, displayName);
            } catch (Exception e) {
                log.warn("⚠️ 发送节点完成事件失败: {}", nodeName, e);
            }
        }
    }

    /**
     * 发送节点失败事件
     */
    private static void sendNodeFailedEvent(WorkflowContext context, String nodeName, String displayName, 
                                          String errorMessage, int nodeIndex, int totalNodes) {
        if (workflowProgressService != null && context != null && context.getJobId() != null) {
            try {
                WorkflowProgressEvent event = WorkflowProgressEvent.nodeFailed(
                    context.getJobId(), 
                    context.getAppId(), 
                    nodeName, 
                    displayName, 
                    errorMessage, 
                    nodeIndex, 
                    totalNodes
                );
                workflowProgressService.sendProgressEvent(event);
                log.debug("❌ 发送节点失败事件: {} - {} - {}", nodeName, displayName, errorMessage);
            } catch (Exception e) {
                log.warn("⚠️ 发送节点失败事件失败: {}", nodeName, e);
            }
        }
    }
}