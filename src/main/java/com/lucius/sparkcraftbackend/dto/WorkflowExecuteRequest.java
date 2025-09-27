package com.lucius.sparkcraftbackend.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工作流执行请求
 */
@Data
public class WorkflowExecuteRequest implements Serializable {

    /**
     * 项目ID
     */
    private Long imageProjectId;

    /**
     * 用户输入的原始提示词
     */
    private String originalPrompt;

    /**
     * 是否异步执行（默认为同步）
     */
    private Boolean async = false;

    @Serial
    private static final long serialVersionUID = 1L;
}