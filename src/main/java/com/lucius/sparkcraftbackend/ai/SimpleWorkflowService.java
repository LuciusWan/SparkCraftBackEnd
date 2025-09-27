package com.lucius.sparkcraftbackend.ai;

import com.lucius.sparkcraftbackend.ai.node.PromptEnhancerNode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 简化的工作流服务
 */
@Slf4j
@Service
public class SimpleWorkflowService {

    /**
     * 创建工作节点的通用方法
     */
    private AsyncNodeAction<MessagesState<String>> makeNode(String nodeName, String message) {
        return node_async(state -> {
            log.info("执行节点: {} - {}", nodeName, message);
            
            // 模拟节点处理逻辑
            Map<String, Object> result = new HashMap<>();
            
            switch (nodeName) {
                case "prompt_enhancer":
                    result.put("messages", "增强后的提示词：用户输入（已结合历史对话记忆）");
                    break;
                case "image_collector":
                    result.put("messages", "已收集到相关图片素材：成都火锅、茶具、传统工艺等");
                    break;
                case "image_maker":
                    result.put("messages", "已生成文创产品设计图：融合成都元素的茶具套装设计");
                    break;
                case "production_process":
                    result.put("messages", "已生成生产工艺流程：陶瓷制作 -> 图案绘制 -> 烧制 -> 包装");
                    break;
                case "model_maker":
                    result.put("messages", "已生成3D模型文件：茶具套装.obj");
                    break;
                default:
                    result.put("messages", "节点执行完成：" + message);
            }
            
            return result;
        });
    }

    /**
     * 执行工作流（使用默认 appId）
     */
    public Map<String, Object> executeWorkflow(String originalPrompt) throws GraphStateException {
        return executeWorkflow(originalPrompt, 1L); // 默认使用 appId = 1
    }

    /**
     * 执行工作流（带 appId）
     */
    public Map<String, Object> executeWorkflow(String originalPrompt, Long appId) throws GraphStateException {
        log.info("开始执行工作流，原始提示词: {}, appId: {}", originalPrompt, appId);
        
        // 创建工作流图
        CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                // 添加节点
                .addNode("prompt_enhancer", PromptEnhancerNode.create())
                .addNode("image_collector", com.lucius.sparkcraftbackend.ai.node.ImageSearchNode.create())
                .addNode("image_maker", makeNode("image_maker", "制作图片"))
                .addNode("production_process", makeNode("production_process", "生成生产工艺"))
                .addNode("model_maker", makeNode("model_maker", "制作模型"))

                // 添加边
                .addEdge("__START__", "prompt_enhancer")
                .addEdge("prompt_enhancer", "image_collector")
                .addEdge("image_collector", "image_maker")
                .addEdge("image_maker", "production_process")
                .addEdge("production_process", "model_maker")
                .addEdge("model_maker", "__END__")

                // 编译工作流
                .compile();

        // 执行工作流
        Map<String, Object> nodeResults = new HashMap<>();
        Map<String, Object> initialInput = new HashMap<>();
        initialInput.put("messages", originalPrompt);
        initialInput.put("appId", appId);
        
        // 创建初始的 WorkflowContext 并设置到 ThreadLocal
        WorkflowContext initialContext = new WorkflowContext();
        initialContext.setAppId(appId);
        initialContext.setOriginalPrompt(originalPrompt);
        WorkflowContext.setCurrentContext(initialContext);
        
        try {
            for (NodeOutput<MessagesState<String>> step : workflow.stream(initialInput)) {
                log.info("工作流步骤完成: {} - {}", step.node(), step.state());
                
                String nodeName = step.node();
                if (nodeName != null && step.state() != null) {
                    // 简化处理：根据节点名称设置结果
                    switch (nodeName) {
                        case "prompt_enhancer":
                            nodeResults.put(nodeName, "提示词增强完成");
                            break;
                        case "image_collector":
                            nodeResults.put(nodeName, "已收集到相关图片素材");
                            break;
                        case "image_maker":
                            nodeResults.put(nodeName, "已生成文创产品设计图");
                            break;
                        case "production_process":
                            nodeResults.put(nodeName, "已生成生产工艺流程");
                            break;
                        case "model_maker":
                            nodeResults.put(nodeName, "已生成3D模型文件");
                            break;
                        default:
                            nodeResults.put(nodeName, "节点执行完成");
                    }
                }
            }
        
            // 获取工作流上下文中的关键信息
            WorkflowContext finalContext = WorkflowContext.getCurrentContext();
            
            // 添加上下文信息到结果中
            if (finalContext != null) {
                nodeResults.put("enhancedPrompt", finalContext.getEnhancedPrompt());
                nodeResults.put("keyPoint", finalContext.getKeyPoint());
                nodeResults.put("originalPrompt", finalContext.getOriginalPrompt());
                nodeResults.put("imageList", finalContext.getImageList());
                
                log.info("工作流执行完成 - 关键词: {}", finalContext.getKeyPoint());
                log.info("工作流执行完成 - 增强提示词长度: {}", 
                        finalContext.getEnhancedPrompt() != null ? finalContext.getEnhancedPrompt().length() : 0);
                log.info("工作流执行完成 - 收集到图片数量: {}", 
                        finalContext.getImageList() != null ? finalContext.getImageList().size() : 0);
            }
            
            log.info("工作流执行完成，结果: {}", nodeResults);
            return nodeResults;
        } finally {
            // 清理 ThreadLocal 上下文
            WorkflowContext.clearCurrentContext();
        }
    }
}