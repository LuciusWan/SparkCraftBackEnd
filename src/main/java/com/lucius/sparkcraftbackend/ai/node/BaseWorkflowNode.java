package com.lucius.sparkcraftbackend.ai.node;

import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent;
import com.lucius.sparkcraftbackend.service.WorkflowProgressService;
import lombok.extern.slf4j.Slf4j;

/**
 * 工作流节点基类，提供SSE进度推送功能
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Slf4j
public abstract class BaseWorkflowNode {

    /**
     * 工作流进度服务，用于发送SSE事件
     */
    protected static WorkflowProgressService workflowProgressService;

    /**
     * 节点名称映射
     */
    private static final java.util.Map<String, String> NODE_DISPLAY_NAMES = new java.util.HashMap<>();

    static {
        NODE_DISPLAY_NAMES.put("prompt_enhancer", "提示词增强");
        NODE_DISPLAY_NAMES.put("image_collector", "图片搜集");
        NODE_DISPLAY_NAMES.put("image_maker", "图片生成");
        NODE_DISPLAY_NAMES.put("production_process", "生产工艺");
        NODE_DISPLAY_NAMES.put("model_maker", "3D建模");
    }

    /**
     * 设置工作流进度服务
     */
    public static void setWorkflowProgressService(WorkflowProgressService service) {
        workflowProgressService = service;
    }

    /**
     * 发送节点开始事件
     */
    protected void sendNodeStartedEvent(String nodeName, int nodeIndex, int totalNodes) {
        if (workflowProgressService != null) {
            WorkflowContext context = WorkflowContext.getCurrentContext();
            if (context != null && context.getJobId() != null) {
                String displayName = NODE_DISPLAY_NAMES.getOrDefault(nodeName, nodeName);
                WorkflowProgressEvent event = WorkflowProgressEvent.nodeStarted(
                    context.getJobId(), 
                    context.getAppId(), 
                    nodeName, 
                    displayName, 
                    nodeIndex, 
                    totalNodes
                );
                workflowProgressService.sendProgressEvent(event);
                log.info("📡 发送节点开始事件: {} - {}", nodeName, displayName);
            }
        }
    }

    /**
     * 发送节点完成事件
     */
    protected void sendNodeCompletedEvent(String nodeName, Object result, int nodeIndex, int totalNodes) {
        if (workflowProgressService != null) {
            WorkflowContext context = WorkflowContext.getCurrentContext();
            if (context != null && context.getJobId() != null) {
                String displayName = NODE_DISPLAY_NAMES.getOrDefault(nodeName, nodeName);
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
                log.info("✅ 发送节点完成事件: {} - {}", nodeName, displayName);
            }
        }
    }

    /**
     * 发送节点失败事件
     */
    protected void sendNodeFailedEvent(String nodeName, String errorMessage, int nodeIndex, int totalNodes) {
        if (workflowProgressService != null) {
            WorkflowContext context = WorkflowContext.getCurrentContext();
            if (context != null && context.getJobId() != null) {
                String displayName = NODE_DISPLAY_NAMES.getOrDefault(nodeName, nodeName);
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
                log.error("❌ 发送节点失败事件: {} - {} - {}", nodeName, displayName, errorMessage);
            }
        }
    }

    /**
     * 获取节点显示名称
     */
    protected String getNodeDisplayName(String nodeName) {
        return NODE_DISPLAY_NAMES.getOrDefault(nodeName, nodeName);
    }

    /**
     * 执行节点逻辑的抽象方法，子类需要实现
     */
    protected abstract Object executeNodeLogic(Object input) throws Exception;

    /**
     * 获取节点名称，子类需要实现
     */
    protected abstract String getNodeName();

    /**
     * 执行节点（带进度推送）
     * 这个方法提供了一个通用的执行模板，包含进度推送逻辑
     */
    public Object executeWithProgress(Object input, int nodeIndex, int totalNodes) {
        String nodeName = getNodeName();
        
        try {
            // 发送节点开始事件
            sendNodeStartedEvent(nodeName, nodeIndex, totalNodes);
            
            // 执行节点逻辑
            Object result = executeNodeLogic(input);
            
            // 发送节点完成事件
            sendNodeCompletedEvent(nodeName, result, nodeIndex, totalNodes);
            
            return result;
            
        } catch (Exception e) {
            // 发送节点失败事件
            sendNodeFailedEvent(nodeName, e.getMessage(), nodeIndex, totalNodes);
            throw new RuntimeException("节点执行失败: " + nodeName, e);
        }
    }

    /**
     * 获取当前工作流上下文
     */
    protected WorkflowContext getCurrentContext() {
        return WorkflowContext.getCurrentContext();
    }

    /**
     * 更新工作流上下文
     */
    protected void updateContext(java.util.function.Consumer<WorkflowContext> updater) {
        WorkflowContext context = WorkflowContext.getCurrentContext();
        if (context != null) {
            updater.accept(context);
        }
    }

    /**
     * 记录节点执行日志
     */
    protected void logNodeExecution(String nodeName, String message) {
        log.info("🔄 [{}] {}", nodeName, message);
    }

    /**
     * 记录节点错误日志
     */
    protected void logNodeError(String nodeName, String message, Throwable error) {
        log.error("❌ [{}] {} - 错误: {}", nodeName, message, error.getMessage(), error);
    }
}