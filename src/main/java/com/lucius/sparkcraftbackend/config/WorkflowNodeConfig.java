package com.lucius.sparkcraftbackend.config;

import com.lucius.sparkcraftbackend.ai.node.BaseWorkflowNode;
import com.lucius.sparkcraftbackend.ai.node.WorkflowNodeWrapper;
import com.lucius.sparkcraftbackend.service.WorkflowProgressService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 工作流节点配置类
 * 用于将Spring管理的服务注入到静态节点基类中
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Configuration
public class WorkflowNodeConfig {

    @Autowired
    private WorkflowProgressService workflowProgressService;

    /**
     * 在Spring容器初始化完成后，将服务注入到节点基类
     */
    @PostConstruct
    public void initWorkflowNodes() {
        // 设置基类的服务
        BaseWorkflowNode.setWorkflowProgressService(workflowProgressService);
        
        // 设置包装器的服务
        WorkflowNodeWrapper.setWorkflowProgressService(workflowProgressService);
    }
}