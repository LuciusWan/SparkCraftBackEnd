package com.lucius.sparkcraftbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流进度事件
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowProgressEvent {

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 任务ID
     */
    private String jobId;

    /**
     * 项目ID
     */
    private Long imageProjectId;

    /**
     * 当前节点名称
     */
    private String currentNode;

    /**
     * 节点显示名称
     */
    private String nodeDisplayName;

    /**
     * 执行状态
     */
    private NodeStatus status;

    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 节点执行结果数据
     */
    private Object nodeResult;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 事件时间
     */
    private LocalDateTime timestamp;

    /**
     * 总节点数
     */
    private Integer totalNodes;

    /**
     * 当前节点索引（从1开始）
     */
    private Integer currentNodeIndex;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        WORKFLOW_STARTED("工作流开始"),
        NODE_STARTED("节点开始执行"),
        NODE_COMPLETED("节点执行完成"),
        NODE_FAILED("节点执行失败"),
        WORKFLOW_COMPLETED("工作流完成"),
        WORKFLOW_FAILED("工作流失败");

        private final String description;

        EventType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 节点状态枚举
     */
    public enum NodeStatus {
        PENDING("等待执行"),
        RUNNING("执行中"),
        COMPLETED("执行完成"),
        FAILED("执行失败"),
        SKIPPED("跳过执行");

        private final String description;

        NodeStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 创建工作流开始事件
     */
    public static WorkflowProgressEvent workflowStarted(String jobId, Long imageProjectId, String originalPrompt) {
        return WorkflowProgressEvent.builder()
                .eventType(EventType.WORKFLOW_STARTED)
                .jobId(jobId)
                .imageProjectId(imageProjectId)
                .status(NodeStatus.RUNNING)
                .progress(0)
                .message("工作流开始执行: " + originalPrompt)
                .timestamp(LocalDateTime.now())
                .totalNodes(5) // 当前有5个节点
                .currentNodeIndex(0)
                .build();
    }

    /**
     * 创建节点开始事件
     */
    public static WorkflowProgressEvent nodeStarted(String jobId, Long imageProjectId, String nodeName, 
                                                   String displayName, int nodeIndex, int totalNodes) {
        return WorkflowProgressEvent.builder()
                .eventType(EventType.NODE_STARTED)
                .jobId(jobId)
                .imageProjectId(imageProjectId)
                .currentNode(nodeName)
                .nodeDisplayName(displayName)
                .status(NodeStatus.RUNNING)
                .progress((nodeIndex - 1) * 100 / totalNodes)
                .message("开始执行: " + displayName)
                .timestamp(LocalDateTime.now())
                .totalNodes(totalNodes)
                .currentNodeIndex(nodeIndex)
                .build();
    }

    /**
     * 创建节点完成事件
     */
    public static WorkflowProgressEvent nodeCompleted(String jobId, Long imageProjectId, String nodeName, 
                                                     String displayName, Object result, int nodeIndex, int totalNodes) {
        return WorkflowProgressEvent.builder()
                .eventType(EventType.NODE_COMPLETED)
                .jobId(jobId)
                .imageProjectId(imageProjectId)
                .currentNode(nodeName)
                .nodeDisplayName(displayName)
                .status(NodeStatus.COMPLETED)
                .progress(nodeIndex * 100 / totalNodes)
                .message(displayName + " 执行完成")
                .nodeResult(result)
                .timestamp(LocalDateTime.now())
                .totalNodes(totalNodes)
                .currentNodeIndex(nodeIndex)
                .build();
    }

    /**
     * 创建节点失败事件
     */
    public static WorkflowProgressEvent nodeFailed(String jobId, Long imageProjectId, String nodeName, 
                                                  String displayName, String errorMessage, int nodeIndex, int totalNodes) {
        return WorkflowProgressEvent.builder()
                .eventType(EventType.NODE_FAILED)
                .jobId(jobId)
                .imageProjectId(imageProjectId)
                .currentNode(nodeName)
                .nodeDisplayName(displayName)
                .status(NodeStatus.FAILED)
                .progress((nodeIndex - 1) * 100 / totalNodes)
                .message(displayName + " 执行失败")
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .totalNodes(totalNodes)
                .currentNodeIndex(nodeIndex)
                .build();
    }

    /**
     * 创建工作流完成事件
     */
    public static WorkflowProgressEvent workflowCompleted(String jobId, Long imageProjectId, Object finalResult) {
        return WorkflowProgressEvent.builder()
                .eventType(EventType.WORKFLOW_COMPLETED)
                .jobId(jobId)
                .imageProjectId(imageProjectId)
                .status(NodeStatus.COMPLETED)
                .progress(100)
                .message("工作流执行完成")
                .nodeResult(finalResult)
                .timestamp(LocalDateTime.now())
                .totalNodes(5)
                .currentNodeIndex(5)
                .build();
    }

    /**
     * 创建工作流失败事件
     */
    public static WorkflowProgressEvent workflowFailed(String jobId, Long imageProjectId, String errorMessage) {
        return WorkflowProgressEvent.builder()
                .eventType(EventType.WORKFLOW_FAILED)
                .jobId(jobId)
                .imageProjectId(imageProjectId)
                .status(NodeStatus.FAILED)
                .progress(0)
                .message("工作流执行失败")
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}