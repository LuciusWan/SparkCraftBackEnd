package com.lucius.sparkcraftbackend.service;

import com.lucius.sparkcraftbackend.entity.User;
import com.lucius.sparkcraftbackend.vo.WorkflowExecuteVO;
import reactor.core.publisher.Flux;

/**
 * 工作流执行服务接口
 */
public interface WorkflowExecutionService {

    /**
     * 同步执行工作流
     *
     * @param imageProjectId 项目ID
     * @param originalPrompt 原始提示词
     * @param loginUser      登录用户
     * @return 执行结果
     */
    WorkflowExecuteVO executeWorkflow(Long imageProjectId, String originalPrompt, User loginUser);

    /**
     * 异步执行工作流（支持SSE进度推送）
     *
     * @param imageProjectId 项目ID
     * @param originalPrompt 原始提示词
     * @param loginUser      登录用户
     * @return 任务信息（包含jobId）
     */
    WorkflowExecuteVO executeWorkflowAsync(Long imageProjectId, String originalPrompt, User loginUser);

    /**
     * 异步执行工作流（支持SSE实时进度推送）
     *
     * @param imageProjectId 项目ID
     * @param originalPrompt 原始提示词
     * @param loginUser      登录用户
     * @return 任务信息（包含jobId）
     */
    WorkflowExecuteVO executeWorkflowAsyncWithSSE(Long imageProjectId, String originalPrompt, User loginUser);

    /**
     * 流式执行工作流（实时返回各节点执行状态）
     *
     * @param imageProjectId 项目ID
     * @param originalPrompt 原始提示词
     * @param loginUser      登录用户
     * @return 执行状态流
     */
    Flux<WorkflowExecuteVO> executeWorkflowStream(Long imageProjectId, String originalPrompt, User loginUser);
}