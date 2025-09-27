package com.lucius.sparkcraftbackend.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lucius.sparkcraftbackend.ai.SimpleWorkflowService;
import com.lucius.sparkcraftbackend.entity.User;
import com.lucius.sparkcraftbackend.service.WorkflowExecutionService;
import com.lucius.sparkcraftbackend.vo.WorkflowExecuteVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流执行服务实现类
 */
@Slf4j
@Service
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    @Resource
    private SimpleWorkflowService simpleWorkflowService;

    @Override
    public WorkflowExecuteVO executeWorkflow(Long imageProjectId, String originalPrompt, User loginUser) {
        String executionId = IdUtil.simpleUUID();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            log.info("开始执行工作流，项目ID: {}, 执行ID: {}", imageProjectId, executionId);
            
            // 使用简化的工作流服务，传递 imageProjectId
            Map<String, Object> nodeResults = simpleWorkflowService.executeWorkflow(originalPrompt, imageProjectId);
            
            // 从工作流结果中获取增强提示词、关键词和图片列表
            String enhancedPrompt = (String) nodeResults.getOrDefault("enhancedPrompt", 
                    "增强后的提示词：" + originalPrompt + "（已结合历史对话记忆）");
            String keyPoint = (String) nodeResults.get("keyPoint");
            @SuppressWarnings("unchecked")
            java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource> imageList = 
                    (java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource>) nodeResults.get("imageList");
            
            LocalDateTime endTime = LocalDateTime.now();
            
            // 构建返回结果
            WorkflowExecuteVO result = new WorkflowExecuteVO();
            result.setExecutionId(executionId);
            result.setImageProjectId(imageProjectId);
            result.setStatus("COMPLETED");
            result.setOriginalPrompt(originalPrompt);
            result.setEnhancedPrompt(enhancedPrompt);
            result.setKeyPoint(keyPoint);
            result.setImageList(imageList);
            result.setNodeResults(nodeResults);
            result.setStartTime(startTime);
            result.setEndTime(endTime);
            result.setDuration(java.time.Duration.between(startTime, endTime).toMillis());
            
            // 输出收集到的图片素材信息
            if (imageList != null && !imageList.isEmpty()) {
                log.info("工作流执行完成，收集到 {} 张图片素材:", imageList.size());
                for (int i = 0; i < imageList.size(); i++) {
                    com.lucius.sparkcraftbackend.entity.ImageResource image = imageList.get(i);
                    log.info("  图片 {}: {} - {}", i + 1, image.getDescription(), image.getUrl());
                }
            } else {
                log.info("工作流执行完成，未收集到图片素材");
            }
            
            log.info("工作流执行完成，执行ID: {}, 耗时: {}ms", executionId, result.getDuration());
            return result;
            
        } catch (Exception e) {
            log.error("工作流执行失败，执行ID: {}", executionId, e);
            
            LocalDateTime endTime = LocalDateTime.now();
            WorkflowExecuteVO result = new WorkflowExecuteVO();
            result.setExecutionId(executionId);
            result.setImageProjectId(imageProjectId);
            result.setStatus("FAILED");
            result.setOriginalPrompt(originalPrompt);
            result.setStartTime(startTime);
            result.setEndTime(endTime);
            result.setDuration(java.time.Duration.between(startTime, endTime).toMillis());
            result.setErrorMessage(e.getMessage());
            
            return result;
        }
    }

    @Override
    public Flux<WorkflowExecuteVO> executeWorkflowStream(Long imageProjectId, String originalPrompt, User loginUser) {
        String executionId = IdUtil.simpleUUID();
        LocalDateTime startTime = LocalDateTime.now();
        
        return Flux.create(sink -> {
            try {
                log.info("开始流式执行工作流，项目ID: {}, 执行ID: {}", imageProjectId, executionId);
                
                // 发送开始状态
                WorkflowExecuteVO startResult = new WorkflowExecuteVO();
                startResult.setExecutionId(executionId);
                startResult.setImageProjectId(imageProjectId);
                startResult.setStatus("RUNNING");
                startResult.setOriginalPrompt(originalPrompt);
                startResult.setStartTime(startTime);
                startResult.setNodeResults(new HashMap<>());
                sink.next(startResult);
                
                // 使用简化的工作流服务执行并模拟流式返回
                Map<String, Object> allNodeResults = new HashMap<>();
                String enhancedPrompt = "增强后的提示词：" + originalPrompt + "（已结合历史对话记忆）";
                String keyPoint = "西安古建筑 文创 古都文化"; // 模拟关键词
                
                // 模拟图片素材列表
                java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource> mockImageList = new java.util.ArrayList<>();
                mockImageList.add(com.lucius.sparkcraftbackend.entity.ImageResource.builder()
                        .description("西安古建筑参考图片1")
                        .url("https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800")
                        .build());
                mockImageList.add(com.lucius.sparkcraftbackend.entity.ImageResource.builder()
                        .description("文创产品参考图片2")
                        .url("https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=800")
                        .build());
                mockImageList.add(com.lucius.sparkcraftbackend.entity.ImageResource.builder()
                        .description("传统工艺参考图片3")
                        .url("https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800")
                        .build());
                
                // 模拟各个节点的执行
                String[] nodeNames = {"prompt_enhancer", "image_collector", "image_maker", "production_process", "model_maker"};
                String[] nodeMessages = {
                    "提示词增强完成",
                    "已收集到相关图片素材：成都火锅、茶具、传统工艺等",
                    "已生成文创产品设计图：融合成都元素的茶具套装设计",
                    "已生成生产工艺流程：陶瓷制作 -> 图案绘制 -> 烧制 -> 包装",
                    "已生成3D模型文件：茶具套装.obj"
                };
                
                for (int i = 0; i < nodeNames.length; i++) {
                    // 模拟节点执行时间
                    try {
                        Thread.sleep(1000); // 1秒延迟
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    allNodeResults.put(nodeNames[i], nodeMessages[i]);
                    
                    // 发送当前步骤结果
                    WorkflowExecuteVO stepResult = new WorkflowExecuteVO();
                    stepResult.setExecutionId(executionId);
                    stepResult.setImageProjectId(imageProjectId);
                    stepResult.setStatus("RUNNING");
                    stepResult.setOriginalPrompt(originalPrompt);
                    stepResult.setEnhancedPrompt(enhancedPrompt);
                    stepResult.setKeyPoint(keyPoint);
                    stepResult.setImageList(i >= 1 ? mockImageList : null); // 从第二个节点开始显示图片列表
                    stepResult.setNodeResults(new HashMap<>(allNodeResults));
                    stepResult.setStartTime(startTime);
                    
                    sink.next(stepResult);
                }
                
                // 发送完成状态
                LocalDateTime endTime = LocalDateTime.now();
                WorkflowExecuteVO finalResult = new WorkflowExecuteVO();
                finalResult.setExecutionId(executionId);
                finalResult.setImageProjectId(imageProjectId);
                finalResult.setStatus("COMPLETED");
                finalResult.setOriginalPrompt(originalPrompt);
                finalResult.setEnhancedPrompt(enhancedPrompt);
                finalResult.setKeyPoint(keyPoint);
                finalResult.setImageList(mockImageList);
                finalResult.setNodeResults(allNodeResults);
                finalResult.setStartTime(startTime);
                finalResult.setEndTime(endTime);
                finalResult.setDuration(java.time.Duration.between(startTime, endTime).toMillis());
                
                sink.next(finalResult);
                sink.complete();
                
                log.info("流式工作流执行完成，执行ID: {}", executionId);
                
            } catch (Exception e) {
                log.error("流式工作流执行失败，执行ID: {}", executionId, e);
                
                LocalDateTime endTime = LocalDateTime.now();
                WorkflowExecuteVO errorResult = new WorkflowExecuteVO();
                errorResult.setExecutionId(executionId);
                errorResult.setImageProjectId(imageProjectId);
                errorResult.setStatus("FAILED");
                errorResult.setOriginalPrompt(originalPrompt);
                errorResult.setStartTime(startTime);
                errorResult.setEndTime(endTime);
                errorResult.setDuration(java.time.Duration.between(startTime, endTime).toMillis());
                errorResult.setErrorMessage(e.getMessage());
                
                sink.next(errorResult);
                sink.complete();
            }
        });
    }


}