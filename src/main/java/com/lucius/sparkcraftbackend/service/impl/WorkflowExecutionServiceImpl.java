package com.lucius.sparkcraftbackend.service.impl;

import cn.hutool.core.util.IdUtil;
import com.lucius.sparkcraftbackend.ai.SimpleWorkflowService;
import com.lucius.sparkcraftbackend.entity.User;
import com.lucius.sparkcraftbackend.service.WorkflowExecutionService;
import com.lucius.sparkcraftbackend.service.WorkflowJobService;
import com.lucius.sparkcraftbackend.service.WorkflowProgressService;
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
    
    @Resource
    private WorkflowJobService workflowJobService;
    
    @Resource
    private WorkflowProgressService workflowProgressService;

    @Override
    public WorkflowExecuteVO executeWorkflow(Long imageProjectId, String originalPrompt, User loginUser) {
        String executionId = IdUtil.simpleUUID();
        LocalDateTime startTime = LocalDateTime.now();
        
        // 创建工作流任务并获取jobId
        String jobId = workflowJobService.createJob(loginUser.getId(), imageProjectId, originalPrompt);
        
        try {
            log.info("开始执行工作流，项目ID: {}, 执行ID: {}, jobId: {}", imageProjectId, executionId, jobId);
            
            // 更新任务状态为运行中
            workflowJobService.updateJobStatus(jobId, "RUNNING", "工作流执行中", 10);
            
            // 使用简化的工作流服务，传递 imageProjectId 和 userId
            Map<String, Object> nodeResults = simpleWorkflowService.executeWorkflow(originalPrompt, imageProjectId, loginUser.getId());
            
            // 从工作流结果中获取增强提示词、关键词、图片列表、AI生成图片和生产工艺
            String enhancedPrompt = (String) nodeResults.getOrDefault("enhancedPrompt", 
                    "增强后的提示词：" + originalPrompt + "（已结合历史对话记忆）");
            String keyPoint = (String) nodeResults.get("keyPoint");
            @SuppressWarnings("unchecked")
            java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource> imageList = 
                    (java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource>) nodeResults.get("imageList");
            com.lucius.sparkcraftbackend.entity.ImageResource aiImage = 
                    (com.lucius.sparkcraftbackend.entity.ImageResource) nodeResults.get("aiImage");
            String productionProcess = (String) nodeResults.get("productionProcess");
            
            LocalDateTime endTime = LocalDateTime.now();
            
            // 构建返回结果
            WorkflowExecuteVO result = new WorkflowExecuteVO();
            result.setExecutionId(executionId);
            result.setJobId(jobId);  // 添加jobId到返回结果
            result.setImageProjectId(imageProjectId);
            result.setStatus("COMPLETED");
            result.setOriginalPrompt(originalPrompt);
            result.setEnhancedPrompt(enhancedPrompt);
            result.setKeyPoint(keyPoint);
            result.setImageList(imageList);
            result.setAiImage(aiImage);
            result.setProductionProcess(productionProcess);
            result.setNodeResults(nodeResults);
            result.setStartTime(startTime);
            result.setEndTime(endTime);
            result.setDuration(java.time.Duration.between(startTime, endTime).toMillis());
            
            // 更新任务状态为完成
            workflowJobService.updateJobResult(jobId, com.alibaba.fastjson.JSON.toJSONString(nodeResults));
            
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
            
            // 输出AI生成的图片信息
            if (aiImage != null) {
                log.info("🎨 AI 生成图片: {} - {}", aiImage.getDescription(), aiImage.getUrl());
            } else {
                log.info("未生成 AI 图片");
            }
            
            // 输出生产工艺信息
            if (productionProcess != null && !productionProcess.isEmpty()) {
                log.info("🏭 生产工艺流程已生成，长度: {} 字符", productionProcess.length());
                // 输出前200个字符作为预览
                String preview = productionProcess.length() > 200 ? 
                    productionProcess.substring(0, 200) + "..." : productionProcess;
                log.info("📋 工艺预览: {}", preview);
            } else {
                log.info("未生成生产工艺流程");
            }
            
            log.info("工作流执行完成，执行ID: {}, 耗时: {}ms", executionId, result.getDuration());
            return result;
            
        } catch (Exception e) {
            log.error("工作流执行失败，执行ID: {}, jobId: {}", executionId, jobId, e);
            
            // 更新任务错误状态
            workflowJobService.updateJobError(jobId, e.getMessage());
            
            LocalDateTime endTime = LocalDateTime.now();
            WorkflowExecuteVO result = new WorkflowExecuteVO();
            result.setExecutionId(executionId);
            result.setJobId(jobId);  // 添加jobId到返回结果
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
                
                // 模拟AI生成的图片
                com.lucius.sparkcraftbackend.entity.ImageResource mockAiImage = 
                        com.lucius.sparkcraftbackend.entity.ImageResource.builder()
                        .description("AI 生成图片 - " + originalPrompt)
                        .url("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80")
                        .build();
                
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
                    stepResult.setAiImage(i >= 2 ? mockAiImage : null); // 从第三个节点开始显示AI图片
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
                finalResult.setAiImage(mockAiImage);
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

    @Override
    public WorkflowExecuteVO executeWorkflowAsync(Long imageProjectId, String originalPrompt, User loginUser) {
        String executionId = IdUtil.simpleUUID();
        LocalDateTime startTime = LocalDateTime.now();
        
        // 创建工作流任务并获取jobId
        String jobId = workflowJobService.createJob(loginUser.getId(), imageProjectId, originalPrompt);
        
        log.info("🚀 开始异步执行工作流，项目ID: {}, 执行ID: {}, jobId: {}", imageProjectId, executionId, jobId);
        
        // 创建返回结果
        WorkflowExecuteVO result = new WorkflowExecuteVO();
        result.setExecutionId(executionId);
        result.setJobId(jobId);
        result.setImageProjectId(imageProjectId);
        result.setStatus("PENDING");
        result.setOriginalPrompt(originalPrompt);
        result.setStartTime(startTime);
        
        // 异步执行工作流
        new Thread(() -> {
            try {
                log.info("📋 异步工作流线程开始执行，jobId: {}", jobId);
                
                // 更新任务状态为运行中
                workflowJobService.updateJobStatus(jobId, "RUNNING", "工作流执行中", 10);
                
                // 使用简化的工作流服务，传递 imageProjectId 和 userId
                Map<String, Object> nodeResults = simpleWorkflowService.executeWorkflow(originalPrompt, imageProjectId, loginUser.getId());
                
                // 从工作流结果中获取增强提示词、关键词、图片列表、AI生成图片和生产工艺
                String enhancedPrompt = (String) nodeResults.getOrDefault("enhancedPrompt", 
                        "增强后的提示词：" + originalPrompt + "（已结合历史对话记忆）");
                String keywords = (String) nodeResults.getOrDefault("keywords", "关键词提取中...");
                @SuppressWarnings("unchecked")
                java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource> imageList = 
                    (java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource>) nodeResults.get("imageList");
                com.lucius.sparkcraftbackend.entity.ImageResource aiImage = 
                    (com.lucius.sparkcraftbackend.entity.ImageResource) nodeResults.get("aiImage");
                String productionProcess = (String) nodeResults.getOrDefault("productionProcess", "");
                
                // 更新任务状态为完成
                workflowJobService.updateJobStatus(jobId, "COMPLETED", "工作流执行完成", 100);
                
                log.info("✅ 异步工作流执行完成，jobId: {}", jobId);
                
            } catch (Exception e) {
                log.error("❌ 异步工作流执行失败，jobId: {}", jobId, e);
                
                // 更新任务错误状态
                workflowJobService.updateJobError(jobId, e.getMessage());
            }
        }, "WorkflowAsync-" + jobId).start();
        
        return result;
    }
    
    @Override
    public WorkflowExecuteVO executeWorkflowAsyncWithSSE(Long imageProjectId, String originalPrompt, User loginUser) {
        String executionId = IdUtil.simpleUUID();
        LocalDateTime startTime = LocalDateTime.now();
        
        // 创建工作流任务并获取jobId
        String jobId = workflowJobService.createJob(loginUser.getId(), imageProjectId, originalPrompt);
        
        log.info("🚀 开始异步执行工作流（支持SSE），项目ID: {}, 执行ID: {}, jobId: {}", imageProjectId, executionId, jobId);
        
        // 创建返回结果
        WorkflowExecuteVO result = new WorkflowExecuteVO();
        result.setExecutionId(executionId);
        result.setJobId(jobId);
        result.setImageProjectId(imageProjectId);
        result.setStatus("PENDING");
        result.setOriginalPrompt(originalPrompt);
        result.setStartTime(startTime);
        
        // 异步执行工作流（带SSE进度推送）
        new Thread(() -> {
            try {
                log.info("📋 异步工作流线程开始执行（SSE模式），jobId: {}", jobId);
                
                // 发送工作流开始事件
                workflowProgressService.sendProgressEvent(
                    com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent.workflowStarted(jobId, imageProjectId, originalPrompt));
                
                // 更新任务状态为运行中
                workflowJobService.updateJobStatus(jobId, "RUNNING", "工作流执行中", 10);
                
                // 使用简化的工作流服务，传递 imageProjectId 和 userId，同时传递jobId用于SSE推送
                Map<String, Object> nodeResults = simpleWorkflowService.executeWorkflowWithSSE(originalPrompt, imageProjectId, loginUser.getId(), jobId);
                
                // 从工作流结果中获取增强提示词、关键词、图片列表、AI生成图片和生产工艺
                String enhancedPrompt = (String) nodeResults.getOrDefault("enhancedPrompt", 
                        "增强后的提示词：" + originalPrompt + "（已结合历史对话记忆）");
                String keywords = (String) nodeResults.getOrDefault("keyPoint", "关键词提取中...");
                @SuppressWarnings("unchecked")
                java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource> imageList = 
                    (java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource>) nodeResults.get("imageList");
                com.lucius.sparkcraftbackend.entity.ImageResource aiImage = 
                    (com.lucius.sparkcraftbackend.entity.ImageResource) nodeResults.get("aiImage");
                String productionProcess = (String) nodeResults.getOrDefault("productionProcess", "");
                
                // 发送工作流完成事件
                workflowProgressService.sendProgressEvent(
                    com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent.workflowCompleted(jobId, imageProjectId, nodeResults));
                
                // 更新任务状态为完成
                workflowJobService.updateJobStatus(jobId, "COMPLETED", "工作流执行完成", 100);
                
                log.info("✅ 异步工作流执行完成（SSE模式），jobId: {}", jobId);
                
            } catch (Exception e) {
                log.error("❌ 异步工作流执行失败（SSE模式），jobId: {}", jobId, e);
                
                // 发送工作流失败事件
                workflowProgressService.sendProgressEvent(
                    com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent.workflowFailed(jobId, imageProjectId, e.getMessage()));
                
                // 更新任务错误状态
                workflowJobService.updateJobError(jobId, e.getMessage());
            }
        }, "WorkflowAsyncSSE-" + jobId).start();
        
        return result;
    }
}