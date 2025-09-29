package com.lucius.sparkcraftbackend.controller;

import com.lucius.sparkcraftbackend.ai.SimpleWorkflowService;
import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import com.lucius.sparkcraftbackend.common.BaseResponse;
import com.lucius.sparkcraftbackend.common.ResultUtils;
import com.lucius.sparkcraftbackend.vo.WorkflowContextVO;
import com.lucius.sparkcraftbackend.ai.node.ImageThreeDModelNode;
import com.lucius.sparkcraftbackend.entity.ImageResource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 工作流测试控制器
 */
@RestController
@RequestMapping("/workflow/test")
@Slf4j
public class WorkflowTestController {

    @Autowired
    private SimpleWorkflowService simpleWorkflowService;

    /**
     * 测试豆包 AI API 连接
     */
    @GetMapping("/doubao-api-test")
    public BaseResponse<Map<String, Object>> testDoubaoAPI() {
        log.info("测试豆包 AI API 连接");
        
        Map<String, Object> result = new HashMap<>();

            // 简单的 API 测试请求
            String testPrompt = "test image generation";
            
            result.put("status", "API 测试功能开发中");
            result.put("message", "使用豆包 AI 进行图片生成");
            result.put("api_url", "https://ark.cn-beijing.volces.com/api/v3/images/generations");
            result.put("model", "doubao-seedream-4-0-250828");
            
            return ResultUtils.success(result);
            

    }

    /**
     * 测试完整工作流（包含AI图片生成）
     */
    @GetMapping("/full-workflow-test")
    public BaseResponse<Map<String, Object>> testFullWorkflow(@RequestParam Long appId,
                                                             @RequestParam(defaultValue = "设计一款西安大雁塔主题的茶具") String prompt) throws GraphStateException {
        log.info("测试完整工作流（包含AI图片生成），appId: {}, 提示词: {}", appId, prompt);

            // 执行工作流并获取结果
            Map<String, Object> workflowResult = simpleWorkflowService.executeWorkflow(prompt, appId);
            
            // 获取当前上下文中的信息
            WorkflowContext context = WorkflowContext.getCurrentContext();
            
            Map<String, Object> result = new HashMap<>();
            result.put("originalPrompt", prompt);
            result.put("enhancedPrompt", context.getEnhancedPrompt());
            result.put("keyPoint", context.getKeyPoint());
            result.put("imageList", context.getImageList());
            result.put("aiImage", context.getAiImage());
            result.put("workflowResults", workflowResult);
            
            // 输出详细信息到日志
            log.info("=== 完整工作流执行结果 ===");
            log.info("📝 原始提示词: {}", prompt);
            log.info("✨ 增强提示词: {}", context.getEnhancedPrompt());
            log.info("🔑 关键词: {}", context.getKeyPoint());
            
            if (context.getImageList() != null && !context.getImageList().isEmpty()) {
                log.info("🖼️ 收集到 {} 张参考图片:", context.getImageList().size());
                for (int i = 0; i < context.getImageList().size(); i++) {
                    var image = context.getImageList().get(i);
                    log.info("  参考图片 {}: {} - {}", i + 1, image.getDescription(), image.getUrl());
                }
            } else {
                log.info("🖼️ 未收集到参考图片");
            }
            
            if (context.getAiImage() != null) {
                log.info("🎨 AI 生成图片: {} - {}", context.getAiImage().getDescription(), context.getAiImage().getUrl());
            } else {
                log.info("🎨 未生成 AI 图片");
            }
            
            log.info("=== 工作流执行完成 ===");
            
            return ResultUtils.success(result);

    }

    /**
     * 测试 ImageMakerNode 单独功能
     */
    /*@GetMapping("/image-maker-test")
    public BaseResponse<Map<String, Object>> testImageMaker(@RequestParam(defaultValue = "设计一款西安大雁塔主题的茶具") String prompt) throws ExecutionException, InterruptedException {
        log.info("测试 ImageMakerNode 单独功能，提示词: {}", prompt);
        
        Map<String, Object> result = new HashMap<>();

            // 创建测试上下文
            WorkflowContext context = new WorkflowContext();
            context.setEnhancedPrompt(prompt);
            context.setOriginalPrompt(prompt);
            
            // 模拟一些参考图片
            java.util.List<com.lucius.sparkcraftbackend.entity.ImageResource> mockImageList = new java.util.ArrayList<>();
            mockImageList.add(com.lucius.sparkcraftbackend.entity.ImageResource.builder()
                    .description("西安大雁塔参考图片")
                    .url("https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800")
                    .build());
            context.setImageList(mockImageList);
            
            WorkflowContext.setCurrentContext(context);
            
            // 执行 ImageMakerNode
            var imageMakerNode = com.lucius.sparkcraftbackend.ai.node.ImageMakerNode.create();
            
            // 创建初始状态
            Map<String, Object> initialState = new HashMap<>();
            initialState.put("messages", prompt);
            var state = new org.bsc.langgraph4j.prebuilt.MessagesState<String>(initialState);
            
            // 执行节点
            var nodeResult = imageMakerNode.apply(state).get();
            
            // 获取结果
            WorkflowContext finalContext = WorkflowContext.getCurrentContext();
            
            result.put("originalPrompt", prompt);
            result.put("enhancedPrompt", finalContext.getEnhancedPrompt());
            result.put("imageList", finalContext.getImageList());
            result.put("aiImage", finalContext.getAiImage());
            result.put("currentStep", finalContext.getCurrentStep());
            
            log.info("ImageMakerNode 测试完成");
            if (finalContext.getAiImage() != null) {
                log.info("生成的 AI 图片: {} - {}", 
                        finalContext.getAiImage().getDescription(), 
                        finalContext.getAiImage().getUrl());
            }
            
            return ResultUtils.success(result);

    }*/
    
    /**
     * 测试完整工作流（返回WorkflowContextVO）
     */
    @GetMapping("/full-workflow-vo-test")
    public BaseResponse<WorkflowContextVO> testFullWorkflowWithVO(@RequestParam Long appId,
                                                                 @RequestParam(defaultValue = "设计一款成都熊猫主题的茶具套装") String prompt) throws GraphStateException {
        log.info("🧪 测试完整工作流（返回WorkflowContextVO），appId: {}, 提示词: {}", appId, prompt);

        try {
            // 执行工作流并获取完整上下文结果
            WorkflowContextVO result = simpleWorkflowService.executeWorkflowWithContext(prompt, appId);
            
            // 输出详细信息到日志
            log.info("=== 完整工作流执行结果（VO格式） ===");
            log.info("📝 原始提示词: {}", result.getOriginalPrompt());
            log.info("✨ 增强提示词长度: {}", result.getEnhancedPrompt() != null ? result.getEnhancedPrompt().length() : 0);
            log.info("🔑 关键词: {}", result.getKeyPoint());
            log.info("⏱️ 执行状态: {}", result.getStatus());
            log.info("⏰ 执行时长: {} ms", result.getDuration());
            
            if (result.getImageList() != null && !result.getImageList().isEmpty()) {
                log.info("🖼️ 收集到 {} 张参考图片:", result.getImageList().size());
                for (int i = 0; i < result.getImageList().size(); i++) {
                    var image = result.getImageList().get(i);
                    log.info("  参考图片 {}: {} - {}", i + 1, image.getDescription(), image.getUrl());
                }
            } else {
                log.info("🖼️ 未收集到参考图片");
            }
            
            if (result.getAiImage() != null) {
                log.info("🎨 AI 生成图片: {} - {}", result.getAiImage().getDescription(), result.getAiImage().getUrl());
            } else {
                log.info("🎨 未生成 AI 图片");
            }
            
            if (result.getProductionProcess() != null) {
                log.info("🏭 生产工艺流程长度: {} 字符", result.getProductionProcess().length());
                log.info("🏭 生产工艺预览: {}", 
                        result.getProductionProcess().length() > 200 ? 
                        result.getProductionProcess().substring(0, 200) + "..." : 
                        result.getProductionProcess());
            } else {
                log.info("🏭 未生成生产工艺流程");
            }
            
            log.info("=== 工作流执行完成（VO格式） ===");
            
            return ResultUtils.success(result);
            
        } catch (Exception e) {
            log.error("❌ 工作流执行失败", e);
            return new BaseResponse<>(500, null, "工作流执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试生产工艺分析节点
     */
    @GetMapping("/production-process-test")
    public BaseResponse<Map<String, Object>> testProductionProcess(@RequestParam(defaultValue = "设计一款青花瓷茶具") String prompt) throws ExecutionException, InterruptedException {
        log.info("🧪 测试生产工艺分析节点，提示词: {}", prompt);
        
        Map<String, Object> result = new HashMap<>();

        try {
            // 创建测试上下文
            WorkflowContext context = new WorkflowContext();
            context.setEnhancedPrompt(prompt + "（增强版）");
            context.setOriginalPrompt(prompt);
            
            // 模拟AI生成的图片
            com.lucius.sparkcraftbackend.entity.ImageResource mockAiImage = 
                com.lucius.sparkcraftbackend.entity.ImageResource.builder()
                    .description("AI生成的青花瓷茶具设计图")
                    .url("https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=800")
                    .build();
            context.setAiImage(mockAiImage);
            
            WorkflowContext.setCurrentContext(context);
            
            // 执行 ProductionProcessNode
            var productionProcessNode = com.lucius.sparkcraftbackend.ai.node.ProductionProcessNode.create();
            
            // 创建初始状态
            Map<String, Object> initialState = new HashMap<>();
            initialState.put("messages", prompt);
            var state = new org.bsc.langgraph4j.prebuilt.MessagesState<String>(initialState);
            
            // 执行节点
            var nodeResult = productionProcessNode.apply(state).get();
            
            // 获取结果
            WorkflowContext finalContext = WorkflowContext.getCurrentContext();
            
            result.put("originalPrompt", prompt);
            result.put("enhancedPrompt", finalContext.getEnhancedPrompt());
            result.put("aiImage", finalContext.getAiImage());
            result.put("productionProcess", finalContext.getProductionProcess());
            result.put("currentStep", finalContext.getCurrentStep());
            
            log.info("✅ ProductionProcessNode 测试完成");
            if (finalContext.getProductionProcess() != null) {
                log.info("🏭 生成的生产工艺流程长度: {} 字符", finalContext.getProductionProcess().length());
                log.info("🏭 生产工艺预览: {}", 
                        finalContext.getProductionProcess().length() > 300 ? 
                        finalContext.getProductionProcess().substring(0, 300) + "..." : 
                        finalContext.getProductionProcess());
            }
            
            return ResultUtils.success(result);
            
        } catch (Exception e) {
            log.error("❌ 生产工艺分析测试失败", e);
            return new BaseResponse<>(500, null, "生产工艺分析测试失败: " + e.getMessage());
        } finally {
            WorkflowContext.clearCurrentContext();
        }
    }
    
    /**
     * 测试ImageThreeDModelNode - 3D模型生成节点
     */
    @GetMapping("/3d-model-test")
    public BaseResponse<Map<String, Object>> testImageThreeDModel() {
        try {
            log.info("🎯 开始测试 ImageThreeDModelNode - 3D模型生成");
            
            // 创建测试上下文
            WorkflowContext context = new WorkflowContext();
            context.setAppId(1L);
            context.setOriginalPrompt("设计一个具有成都特色的茶具套装");
            context.setEnhancedPrompt("设计一个融合成都熊猫元素和传统川蜀文化的现代茶具套装，包含茶壶、茶杯、茶盘等，体现成都的悠闲文化和精致工艺");
            
            // 模拟AI生成的图片
            ImageResource mockAiImage = new ImageResource();
            mockAiImage.setUrl("https://mock-ai-image.example.com/teapot_design_" + System.currentTimeMillis() + ".jpg");
            mockAiImage.setDescription("AI生成的成都特色茶具设计图");
            context.setAiImage(mockAiImage);
            
            // 模拟生产工艺
            context.setProductionProcess("1. 陶瓷胚体制作：选用优质高岭土，手工拉坯成型\n2. 图案设计：融入熊猫和竹叶元素\n3. 釉料调配：采用传统青瓷釉\n4. 烧制工艺：1280°C高温烧制\n5. 品质检验：确保无裂纹、变形\n6. 包装设计：环保材料，体现成都文化");
            
            WorkflowContext.setCurrentContext(context);
            
            // 创建初始状态
            Map<String, Object> initialState = new HashMap<>();
            initialState.put("messages", "开始3D模型生成");
            var state = new org.bsc.langgraph4j.prebuilt.MessagesState<String>(initialState);
            
            log.info("📸 使用模拟AI图片: {}", mockAiImage.getUrl());
            
            // 执行ImageThreeDModelNode
            AsyncNodeAction<MessagesState<String>> node = ImageThreeDModelNode.create();
            var nodeResult = node.apply(state).get();
            
            // 获取执行后的上下文
            WorkflowContext finalContext = WorkflowContext.getCurrentContext();
            
            // 构建返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("nodeType", "ImageThreeDModelNode");
            response.put("status", "success");
            response.put("executionResult", nodeResult);
            
            if (finalContext != null) {
                response.put("appId", finalContext.getAppId());
                response.put("originalPrompt", finalContext.getOriginalPrompt());
                response.put("enhancedPrompt", finalContext.getEnhancedPrompt());
                response.put("aiImage", finalContext.getAiImage());
                response.put("productionProcess", finalContext.getProductionProcess());
                response.put("threeDModelUrl", finalContext.getThreeDModelUrl());
                response.put("modelImageUrl", finalContext.getModelImageUrl());
                response.put("currentStep", finalContext.getCurrentStep());
                
                log.info("✅ ImageThreeDModelNode 测试完成");
                log.info("🎨 3D模型URL: {}", finalContext.getThreeDModelUrl());
                log.info("🖼️ 模型预览图URL: {}", finalContext.getModelImageUrl());
                log.info("📊 当前步骤: {}", finalContext.getCurrentStep());
            }
            
            return ResultUtils.success(response);
            
        } catch (Exception e) {
            log.error("❌ 3D模型生成测试失败", e);
            return new BaseResponse<>(500, null, "3D模型生成测试失败: " + e.getMessage());
        } finally {
            WorkflowContext.clearCurrentContext();
        }
    }
}