package com.lucius.sparkcraftbackend.ai.node;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import com.lucius.sparkcraftbackend.dto.TencentCloud3DResponse;
import com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent;
import com.lucius.sparkcraftbackend.entity.ImageProject;
import com.lucius.sparkcraftbackend.entity.ThreeDResult;
import com.lucius.sparkcraftbackend.service.ImageProjectService;
import com.lucius.sparkcraftbackend.service.ThreeDResultService;
import com.lucius.sparkcraftbackend.service.WorkflowProgressService;
import com.tencentcloudapi.ai3d.v20250513.Ai3dClient;
import com.tencentcloudapi.ai3d.v20250513.models.*;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageThreeDModelNode {
    
    private static Ai3dClient ai3dClient;
    private static ImageProjectService imageProjectService;
    private static ThreeDResultService threeDResultService;
    private static WorkflowProgressService workflowProgressService;
    
    /**
     * 设置腾讯云AI 3D客户端（通过配置类注入）
     */
    public static void setAi3dClient(Ai3dClient client) {
        ai3dClient = client;
    }
    
    /**
     * 设置ImageProject服务（通过配置类注入）
     */
    public static void setImageProjectService(ImageProjectService service) {
        imageProjectService = service;
    }
    
    /**
     * 设置ThreeDResult服务（通过配置类注入）
     */
    public static void setThreeDResultService(ThreeDResultService service) {
        threeDResultService = service;
    }

    public static void setWorkflowProgressService(WorkflowProgressService service) {
        workflowProgressService = service;
    }
    
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("🎯 执行节点: 制作3D模型");
            
            // 发送节点开始事件
        if (workflowProgressService != null && context.getJobId() != null) {
            WorkflowProgressEvent startEvent = WorkflowProgressEvent.nodeStarted(
                context.getJobId(), context.getAppId(), "image_3d_model", "3D模型生成", 5, 5);
            workflowProgressService.sendProgressEvent(startEvent);
        }
            
            try {
                // 获取AI生成的图片URL
                String imageUrl = null;
                if (context.getAiImage() != null && context.getAiImage().getUrl() != null) {
                    imageUrl = context.getAiImage().getUrl();
                    log.info("📸 使用AI生成图片: {}", imageUrl);
                } else {
                    log.warn("⚠️ 未找到AI生成图片，使用模拟数据");
                    // 使用模拟数据进行测试
                    return handleMockData(context);
                }
                
                // 提交3D生成任务
                String jobId = submitHunyuanTo3DJob(imageUrl, context.getUserId());
                if (jobId == null) {
                    log.error("❌ 3D任务提交失败");
                    return handleMockData(context);
                }
                
                log.info("✅ 3D任务提交成功，JobId: {}", jobId);
                context.setCurrentStep("3D任务已提交，等待处理中...");
                
                // 异步等待2分半后查询结果
                CompletableFuture.delayedExecutor(150, TimeUnit.SECONDS).execute(() -> {
                    try {
                        queryAndSave3DResult(jobId, context);
                    } catch (Exception e) {
                        log.error("❌ 查询3D结果失败", e);
                    }
                });
                
                // 立即返回，不等待异步任务完成
                context.setCurrentStep("3D模型生成中，预计2分半后完成");
                log.info("🔄 3D模型生成任务已启动，异步处理中...");
                
                // 发送节点完成事件
                if (workflowProgressService != null && context.getJobId() != null) {
                    Map<String, Object> nodeResult = new HashMap<>();
                    nodeResult.put("status", "3D模型生成任务已启动");
                    nodeResult.put("message", "异步处理中，预计2分半后完成");
                    
                    WorkflowProgressEvent completedEvent = WorkflowProgressEvent.nodeCompleted(
                        context.getJobId(), context.getAppId(), "image_3d_model", "3D模型生成", nodeResult, 5, 5);
                    workflowProgressService.sendProgressEvent(completedEvent);
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("messages", "3D模型生成任务已启动，异步处理中...");
                return result;
                
            } catch (Exception e) {
                log.error("❌ 3D模型生成节点执行失败", e);
                return handleMockData(context);
            }
        });
    }
    
    /**
     * 提交混元3D生成任务
     */
    private static String submitHunyuanTo3DJob(String imageUrl, Long userId) {
        try {
            if (ai3dClient == null) {
                log.error("❌ 腾讯云AI 3D客户端未初始化");
                return null;
            }
            
            // 创建请求对象
            SubmitHunyuanTo3DJobRequest req = new SubmitHunyuanTo3DJobRequest();
            req.setResultFormat("STL");
            req.setImageUrl(imageUrl);
            
            log.info("🚀 提交3D生成任务，图片URL: {}", imageUrl);
            
            // 调用API
            SubmitHunyuanTo3DJobResponse resp = ai3dClient.SubmitHunyuanTo3DJob(req);
            String responseJson = AbstractModel.toJsonString(resp);
            log.info("📋 3D任务提交响应: {}", responseJson);
            
            // 解析响应获取JobId
            JSONObject jsonResponse = JSONUtil.parseObj(responseJson);
            String jobId = jsonResponse.getStr("JobId");
            
            if (jobId != null) {
                log.info("✅ 3D任务提交成功，JobId: {}", jobId);
                
                // 保存jobId到数据库
                if (threeDResultService != null && userId != null) {
                    try {
                        ThreeDResult savedRecord = threeDResultService.saveThreeDJob(jobId, userId);
                        if (savedRecord != null) {
                            log.info("💾 JobId已保存到数据库，记录ID: {}", savedRecord.getId());
                        } else {
                            log.error("❌ JobId保存到数据库失败");
                        }
                    } catch (Exception e) {
                        log.error("❌ 保存JobId到数据库时发生异常", e);
                    }
                } else {
                    log.warn("⚠️ ThreeDResultService未初始化或userId为空，跳过数据库保存");
                }
                
                return jobId;
            } else {
                log.error("❌ 响应中未找到JobId");
                return null;
            }
            
        } catch (TencentCloudSDKException e) {
            log.error("❌ 腾讯云API调用失败", e);
            return null;
        } catch (Exception e) {
            log.error("❌ 提交3D任务时发生未知错误", e);
            return null;
        }
    }
    
    /**
     * 查询3D生成结果并保存到数据库
     */
    private static void queryAndSave3DResult(String jobId, WorkflowContext context) {
        try {
            if (ai3dClient == null) {
                log.error("❌ 腾讯云AI 3D客户端未初始化");
                return;
            }
            
            // 创建查询请求
            QueryHunyuanTo3DJobRequest req = new QueryHunyuanTo3DJobRequest();
            req.setJobId(jobId);
            
            log.info("🔍 查询3D生成结果，JobId: {}", jobId);
            
            // 调用查询API
            QueryHunyuanTo3DJobResponse resp = ai3dClient.QueryHunyuanTo3DJob(req);
            String responseJson = AbstractModel.toJsonString(resp);
            log.info("📋 3D查询响应: {}", responseJson);
            
            // 解析响应
            TencentCloud3DResponse response = JSONUtil.toBean(responseJson, TencentCloud3DResponse.class);
            
            if (response.getResponse() != null && "DONE".equals(response.getResponse().getStatus())) {
                // 任务完成，获取3D文件URL
                if (response.getResponse().getResultFile3Ds() != null && 
                    !response.getResponse().getResultFile3Ds().isEmpty()) {
                    
                    TencentCloud3DResponse.ResultFile3D result = response.getResponse().getResultFile3Ds().get(0);
                    String threeDUrl = result.getUrl();
                    String previewImageUrl = result.getPreviewImageUrl();
                    
                    log.info("✅ 3D模型生成完成！");
                    log.info("🎨 3D文件URL: {}", threeDUrl);
                    log.info("🖼️ 预览图URL: {}", previewImageUrl);
                    
                    // 更新WorkflowContext
                    context.setThreeDModelUrl(threeDUrl);
                    context.setModelImageUrl(previewImageUrl);
                    context.setCurrentStep("3D模型生成完成");
                    
                    // 更新ThreeDResult数据库记录
                    if (threeDResultService != null) {
                        try {
                            boolean updateSuccess = threeDResultService.updateThreeDResult(
                                jobId, threeDUrl, previewImageUrl, "DONE");
                            if (updateSuccess) {
                                log.info("✅ ThreeDResult记录更新成功");
                            } else {
                                log.error("❌ ThreeDResult记录更新失败");
                            }
                        } catch (Exception e) {
                            log.error("❌ 更新ThreeDResult记录时发生异常", e);
                        }
                    } else {
                        log.warn("⚠️ ThreeDResultService未初始化，跳过数据库更新");
                    }
                    
                    // 保存到数据库
                    saveToDatabase(context);
                    
                } else {
                    log.warn("⚠️ 3D生成完成但未找到结果文件");
                }
            } else {
                log.warn("⚠️ 3D生成任务尚未完成，状态: {}", 
                        response.getResponse() != null ? response.getResponse().getStatus() : "未知");
            }
            
        } catch (TencentCloudSDKException e) {
            log.error("❌ 查询3D结果API调用失败", e);
        } catch (Exception e) {
            log.error("❌ 查询3D结果时发生未知错误", e);
        }
    }
    
    /**
     * 保存WorkflowContext数据到ImageProject数据库
     */
    private static void saveToDatabase(WorkflowContext context) {
        try {
            if (imageProjectService == null) {
                log.error("❌ ImageProjectService未初始化");
                return;
            }
            
            Long appId = context.getAppId();
            if (appId == null) {
                log.error("❌ AppId为空，无法保存到数据库");
                return;
            }
            
            // 根据appId查询ImageProject
            ImageProject imageProject = imageProjectService.getById(appId);
            if (imageProject == null) {
                log.error("❌ 未找到ID为{}的ImageProject", appId);
                return;
            }
            
            // 更新ImageProject数据
            imageProject.setProjectImageUrl(context.getAiImage() != null ? context.getAiImage().getUrl() : null);
            imageProject.setProductionprocess(context.getProductionProcess());
            imageProject.setThreeDModelUrl(context.getThreeDModelUrl());
            imageProject.setProjectStatus("COMPLETED");
            imageProject.setUpdateTime(LocalDateTime.now());
            
            // 保存到数据库
            boolean success = imageProjectService.updateById(imageProject);
            
            if (success) {
                log.info("✅ 工作流数据已保存到数据库，项目ID: {}", appId);
                log.info("📊 保存的数据:");
                log.info("  - 项目图片URL: {}", imageProject.getProjectImageUrl());
                log.info("  - 生产工艺: {} 字符", 
                        imageProject.getProductionprocess() != null ? imageProject.getProductionprocess().length() : 0);
                log.info("  - 3D模型URL: {}", imageProject.getThreeDModelUrl());
                log.info("  - 项目状态: {}", imageProject.getProjectStatus());
            } else {
                log.error("❌ 保存工作流数据到数据库失败");
            }
            
        } catch (Exception e) {
            log.error("❌ 保存到数据库时发生错误", e);
        }
    }
    
    /**
     * 处理模拟数据（当API调用失败时的降级方案）
     */
    private static Map<String, Object> handleMockData(WorkflowContext context) {
        log.info("🎭 使用模拟数据生成3D模型");
        
        // 生成模拟的3D模型URL
        String mockThreeDUrl = "https://mock-3d-storage.example.com/models/mock_model_" + 
                              System.currentTimeMillis() + ".stl";
        String mockPreviewUrl = "https://mock-3d-storage.example.com/previews/mock_preview_" + 
                               System.currentTimeMillis() + ".png";
        
        context.setThreeDModelUrl(mockThreeDUrl);
        context.setModelImageUrl(mockPreviewUrl);
        context.setCurrentStep("3D模型生成完成（模拟数据）");
        
        log.info("🎨 模拟3D文件URL: {}", mockThreeDUrl);
        log.info("🖼️ 模拟预览图URL: {}", mockPreviewUrl);
        
        // 保存模拟数据到数据库
         saveToDatabase(context);
         
         Map<String, Object> result = new HashMap<>();
         result.put("messages", "3D模型生成完成（使用模拟数据）");
         return result;
    }
}
