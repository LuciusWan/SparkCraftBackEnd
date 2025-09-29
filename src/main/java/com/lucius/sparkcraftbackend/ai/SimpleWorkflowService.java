package com.lucius.sparkcraftbackend.ai;


import com.lucius.sparkcraftbackend.ai.node.PromptEnhancerNode;
import com.lucius.sparkcraftbackend.ai.node.ProductionProcessNode;
import com.lucius.sparkcraftbackend.ai.node.ImageThreeDModelNode;
import com.lucius.sparkcraftbackend.ai.node.WorkflowNodeWrapper;
import com.lucius.sparkcraftbackend.vo.WorkflowContextVO;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 简化的工作流服务
 */
@Slf4j
@Service
public class SimpleWorkflowService {

    @Resource
    private com.lucius.sparkcraftbackend.service.WorkflowProgressService workflowProgressService;

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
        return executeWorkflow(originalPrompt, appId, null);
    }
    
    /**
     * 执行工作流（带 appId 和 userId）
     */
    public Map<String, Object> executeWorkflow(String originalPrompt, Long appId, Long userId) throws GraphStateException {
        log.info("开始执行工作流，原始提示词: {}, appId: {}, userId: {}", originalPrompt, appId, userId);
        
        // 创建工作流图
        CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                // 添加节点
                .addNode("prompt_enhancer", PromptEnhancerNode.create())
                .addNode("image_collector", com.lucius.sparkcraftbackend.ai.node.ImageSearchNode.create())
                .addNode("image_maker", com.lucius.sparkcraftbackend.ai.node.ImageMakerNode.create())
                .addNode("production_process", ProductionProcessNode.create())
                .addNode("model_maker", ImageThreeDModelNode.create())

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
        initialContext.setUserId(userId);  // 设置userId
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
                nodeResults.put("aiImage", finalContext.getAiImage());
                nodeResults.put("productionProcess", finalContext.getProductionProcess());
                
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
    
    /**
     * 执行工作流并返回完整的上下文结果
     */
    public WorkflowContextVO executeWorkflowWithContext(String originalPrompt, Long appId) throws GraphStateException {
        return executeWorkflowWithContext(originalPrompt, appId, null);
    }
    
    /**
     * 执行工作流并返回完整的上下文结果（带 userId）
     */
    public WorkflowContextVO executeWorkflowWithContext(String originalPrompt, Long appId, Long userId) throws GraphStateException {
        log.info("🚀 开始执行工作流（返回完整上下文），原始提示词: {}, appId: {}, userId: {}", originalPrompt, appId, userId);
        
        long startTime = System.currentTimeMillis();
        WorkflowContextVO result = new WorkflowContextVO();
        
        try {
            // 设置基本信息
            result.setAppId(appId);
            result.setUserId(userId);
            result.setOriginalPrompt(originalPrompt);
            result.setStartTime(startTime);
            result.setStatus("RUNNING");
            
            // 创建工作流图
            CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                    // 添加节点（使用包装器提供SSE支持）
                    .addNode("prompt_enhancer", WorkflowNodeWrapper.wrapNode(
                        "prompt_enhancer", "提示词增强", 1, 5, PromptEnhancerNode.create()))
                    .addNode("image_collector", WorkflowNodeWrapper.wrapNode(
                        "image_collector", "图片搜集", 2, 5, com.lucius.sparkcraftbackend.ai.node.ImageSearchNode.create()))
                    .addNode("image_maker", WorkflowNodeWrapper.wrapNode(
                        "image_maker", "图片生成", 3, 5, com.lucius.sparkcraftbackend.ai.node.ImageMakerNode.create()))
                    .addNode("production_process", WorkflowNodeWrapper.wrapNode(
                        "production_process", "生产工艺", 4, 5, ProductionProcessNode.create()))
                    .addNode("model_maker", WorkflowNodeWrapper.wrapNode(
                        "model_maker", "3D建模", 5, 5, ImageThreeDModelNode.create()))

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
            Map<String, Object> initialInput = new HashMap<>();
            initialInput.put("messages", originalPrompt);
            initialInput.put("appId", appId);
            
            // 创建初始的 WorkflowContext 并设置到 ThreadLocal
            WorkflowContext initialContext = new WorkflowContext();
            initialContext.setAppId(appId);
            initialContext.setUserId(userId);  // 设置userId
            initialContext.setOriginalPrompt(originalPrompt);
            WorkflowContext.setCurrentContext(initialContext);
            
            // 执行工作流
            for (NodeOutput<MessagesState<String>> step : workflow.stream(initialInput)) {
                log.info("🔄 工作流步骤完成: {} - {}", step.node(), step.state());
                result.setCurrentStep("正在执行: " + step.node());
            }
            
            // 获取最终的工作流上下文
            WorkflowContext finalContext = WorkflowContext.getCurrentContext();
            
            if (finalContext != null) {
                // 复制所有数据到 VO
                result.setUserId(finalContext.getUserId());
                result.setEnhancedPrompt(finalContext.getEnhancedPrompt());
                result.setKeyPoint(finalContext.getKeyPoint());
                result.setImageList(finalContext.getImageList());
                result.setAiImage(finalContext.getAiImage());
                result.setProductionProcess(finalContext.getProductionProcess());
                result.setThreeDModelUrl(finalContext.getThreeDModelUrl());
                result.setModelImageUrl(finalContext.getModelImageUrl());
                result.setCurrentStep(finalContext.getCurrentStep());
                
                log.info("✅ 工作流执行成功");
                log.info("📝 增强提示词长度: {}", 
                        finalContext.getEnhancedPrompt() != null ? finalContext.getEnhancedPrompt().length() : 0);
                log.info("🔍 关键词: {}", finalContext.getKeyPoint());
                log.info("🖼️ 收集图片数量: {}", 
                        finalContext.getImageList() != null ? finalContext.getImageList().size() : 0);
                log.info("🎨 AI图片: {}", 
                        finalContext.getAiImage() != null ? finalContext.getAiImage().getUrl() : "无");
                log.info("🏭 生产工艺长度: {}", 
                        finalContext.getProductionProcess() != null ? finalContext.getProductionProcess().length() : 0);
            }
            
            // 设置执行结果
            long endTime = System.currentTimeMillis();
            result.setEndTime(endTime);
            result.setDuration(endTime - startTime);
            result.setStatus("COMPLETED");
            
            log.info("🎯 工作流执行完成，总耗时: {} ms", result.getDuration());
            return result;
            
        } catch (Exception e) {
            log.error("❌ 工作流执行失败", e);
            
            // 设置错误信息
            long endTime = System.currentTimeMillis();
            result.setEndTime(endTime);
            result.setDuration(endTime - startTime);
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
            result.setCurrentStep("执行失败: " + e.getMessage());
            
            return result;
            
        } finally {
            // 清理 ThreadLocal 上下文
            WorkflowContext.clearCurrentContext();
        }
    }
    
    /**
     * 执行工作流（带SSE进度推送）
     */
    public Map<String, Object> executeWorkflowWithSSE(String originalPrompt, Long appId, Long userId, String jobId) throws GraphStateException {
        log.info("🚀 开始执行工作流（SSE模式），原始提示词: {}, appId: {}, userId: {}, jobId: {}", originalPrompt, appId, userId, jobId);
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> nodeResults = new HashMap<>();
        
        try {
            // 发送工作流开始事件
            if (workflowProgressService != null) {
                workflowProgressService.sendProgressEvent(
                    com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent.workflowStarted(jobId, appId, originalPrompt));
            }

            // 创建工作流图
            CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                    // 添加节点（使用包装器提供SSE支持）
                    .addNode("prompt_enhancer", WorkflowNodeWrapper.wrapNode(
                        "prompt_enhancer", "提示词增强", 1, 5, PromptEnhancerNode.create()))
                    .addNode("image_collector", WorkflowNodeWrapper.wrapNode(
                        "image_collector", "图片搜集", 2, 5, com.lucius.sparkcraftbackend.ai.node.ImageSearchNode.create()))
                    .addNode("image_maker", WorkflowNodeWrapper.wrapNode(
                        "image_maker", "图片生成", 3, 5, com.lucius.sparkcraftbackend.ai.node.ImageMakerNode.create()))
                    .addNode("production_process", WorkflowNodeWrapper.wrapNode(
                        "production_process", "生产工艺", 4, 5, ProductionProcessNode.create()))
                    .addNode("model_maker", WorkflowNodeWrapper.wrapNode(
                        "model_maker", "3D建模", 5, 5, ImageThreeDModelNode.create()))

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
            Map<String, Object> initialInput = new HashMap<>();
            initialInput.put("messages", originalPrompt);
            initialInput.put("appId", appId);
            
            // 创建初始的 WorkflowContext 并设置到 ThreadLocal
            WorkflowContext initialContext = new WorkflowContext();
            initialContext.setAppId(appId);
            initialContext.setUserId(userId);
            initialContext.setJobId(jobId); // 设置jobId
            initialContext.setOriginalPrompt(originalPrompt);
            WorkflowContext.setCurrentContext(initialContext);
            
            // 定义节点名称和显示名称的映射
            String[] nodeNames = {"prompt_enhancer", "image_collector", "image_maker", "production_process", "model_maker"};
            String[] displayNames = {"提示词增强", "图片搜集", "图片生成", "生产工艺", "3D建模"};
            Map<String, String> nodeDisplayMap = new HashMap<>();
            for (int i = 0; i < nodeNames.length; i++) {
                nodeDisplayMap.put(nodeNames[i], displayNames[i]);
            }
            
            int currentNodeIndex = 0;
            int totalNodes = nodeNames.length;
            
            // 执行工作流
            for (NodeOutput<MessagesState<String>> step : workflow.stream(initialInput)) {
                String nodeName = step.node();
                log.info("🔄 工作流步骤完成: {} - {}", nodeName, step.state());
                
                if (nodeName != null && step.state() != null) {
                    currentNodeIndex++;
                    String displayName = nodeDisplayMap.getOrDefault(nodeName, nodeName);
                    
                    // 发送节点开始事件
                    if (workflowProgressService != null) {
                        workflowProgressService.sendProgressEvent(
                            com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent.nodeStarted(
                                jobId, appId, nodeName, displayName, currentNodeIndex, totalNodes));
                    }
                    
                    // 根据节点名称设置结果
                    Object nodeResult = null;
                    switch (nodeName) {
                        case "prompt_enhancer":
                            nodeResults.put(nodeName, "提示词增强完成");
                            nodeResult = "提示词增强完成";
                            break;
                        case "image_collector":
                            nodeResults.put(nodeName, "已收集到相关图片素材");
                            nodeResult = "已收集到相关图片素材";
                            break;
                        case "image_maker":
                            nodeResults.put(nodeName, "已生成文创产品设计图");
                            nodeResult = "已生成文创产品设计图";
                            break;
                        case "production_process":
                            nodeResults.put(nodeName, "已生成生产工艺流程");
                            nodeResult = "已生成生产工艺流程";
                            break;
                        case "model_maker":
                            nodeResults.put(nodeName, "已生成3D模型文件");
                            nodeResult = "已生成3D模型文件";
                            break;
                        default:
                            nodeResults.put(nodeName, "节点执行完成");
                            nodeResult = "节点执行完成";
                    }
                    
                    // 发送节点完成事件
                    if (workflowProgressService != null) {
                        workflowProgressService.sendProgressEvent(
                            com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent.nodeCompleted(
                                jobId, appId, nodeName, displayName, nodeResult, currentNodeIndex, totalNodes));
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
                nodeResults.put("aiImage", finalContext.getAiImage());
                nodeResults.put("productionProcess", finalContext.getProductionProcess());
                
                log.info("✅ 工作流执行完成 - 关键词: {}", finalContext.getKeyPoint());
                log.info("✅ 工作流执行完成 - 增强提示词长度: {}", 
                        finalContext.getEnhancedPrompt() != null ? finalContext.getEnhancedPrompt().length() : 0);
                log.info("✅ 工作流执行完成 - 收集到图片数量: {}", 
                        finalContext.getImageList() != null ? finalContext.getImageList().size() : 0);
            }
            
            // 发送工作流完成事件
            if (workflowProgressService != null) {
                workflowProgressService.sendProgressEvent(
                    com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent.workflowCompleted(jobId, appId, nodeResults));
            }
            
            long endTime = System.currentTimeMillis();
            log.info("✅ 工作流执行完成（SSE模式），结果: {}，耗时: {}ms", nodeResults, (endTime - startTime));
            return nodeResults;
            
        } catch (Exception e) {
            log.error("❌ 工作流执行失败（SSE模式）", e);
            
            // 发送工作流失败事件
            if (workflowProgressService != null) {
                workflowProgressService.sendProgressEvent(
                    com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent.workflowFailed(jobId, appId, e.getMessage()));
            }
            
            throw e;
            
        } finally {
            // 清理 ThreadLocal 上下文
            WorkflowContext.clearCurrentContext();
        }
    }
}