package com.lucius.sparkcraftbackend.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流执行结果视图对象
 */
@Data
public class WorkflowExecuteVO implements Serializable {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 项目ID
     */
    private Long imageProjectId;

    /**
     * 执行状态（RUNNING, COMPLETED, FAILED）
     */
    private String status;

    /**
     * 原始提示词
     */
    private String originalPrompt;

    /**
     * 增强后的提示词
     */
    private String enhancedPrompt;

    /**
     * 各个节点的执行结果
     */
    private Map<String, Object> nodeResults;

    /**
     * 执行开始时间
     */
    private LocalDateTime startTime;

    /**
     * 执行结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long duration;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    @Serial
    private static final long serialVersionUID = 1L;
}