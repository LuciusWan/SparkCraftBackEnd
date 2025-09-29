package com.lucius.sparkcraftbackend.ai.node;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent;
import com.lucius.sparkcraftbackend.entity.ImageResource;
import com.lucius.sparkcraftbackend.service.WorkflowProgressService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
@Component
@Slf4j
@ConfigurationProperties(prefix = "spark.ai.doubao")
public class ImageMakerNode {
    // 豆包 AI 配置
    private static final String DOUBAO_API_URL = "https://ark.cn-beijing.volces.com/api/v3/images/generations";
    private static String DOUBAO_API_KEY;
    private static final String DOUBAO_MODEL = "doubao-seedream-4-0-250828";
    private static final int TIMEOUT = 60000; // 60秒超时，图片生成可能需要较长时间
    
    private static WorkflowProgressService workflowProgressService;

    public void setApiKey(String apiKey) {
        ImageMakerNode.DOUBAO_API_KEY = apiKey;
    }

    public static void setWorkflowProgressService(WorkflowProgressService service) {
        workflowProgressService = service;
    }
    
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("开始执行 AI 图片生成节点");
            
            // 发送节点开始事件
        if (workflowProgressService != null && context.getJobId() != null) {
            WorkflowProgressEvent startEvent = WorkflowProgressEvent.nodeStarted(
                context.getJobId(), context.getAppId(), "image_maker", "AI图片生成", 3, 5);
            workflowProgressService.sendProgressEvent(startEvent);
        }
            
            try {
                // 获取增强后的提示词和参考图片
                String enhancedPrompt = context.getEnhancedPrompt();
                List<ImageResource> imageList = context.getImageList();
                
                if (StrUtil.isBlank(enhancedPrompt)) {
                    log.warn("增强提示词为空，使用原始提示词");
                    enhancedPrompt = context.getOriginalPrompt();
                }
                
                if (StrUtil.isBlank(enhancedPrompt)) {
                    log.error("没有可用的提示词，无法生成图片");
                    context.setCurrentStep("图片生成失败 - 缺少提示词");
                    return WorkflowContext.saveContext(context);
                }
                
                log.info("🎨 开始 AI 图片生成:");
                log.info("  📝 使用提示词: {}", enhancedPrompt);
                log.info("  🖼️ 参考图片数量: {}", imageList != null ? imageList.size() : 0);
                if (imageList != null && !imageList.isEmpty()) {
                    for (int i = 0; i < imageList.size(); i++) {
                        log.info("    参考图片 {}: {}", i + 1, imageList.get(i).getUrl());
                    }
                }
                
                // 调用豆包 AI 生成图片
                ImageResource aiImage = generateImageWithDoubaoAI(enhancedPrompt, imageList);
                
                if (aiImage != null) {
                    context.setAiImage(aiImage);
                    context.setCurrentStep("AI 图片生成完成");
                    log.info("✅ AI 图片生成成功!");
                    log.info("  🎨 生成图片: {}", aiImage.getDescription());
                    log.info("  🔗 图片URL: {}", aiImage.getUrl());
                } else {
                    // 生成失败，使用模拟数据
                    ImageResource mockImage = createMockImage(enhancedPrompt);
                    context.setAiImage(mockImage);
                    context.setCurrentStep("AI 图片生成失败，使用模拟数据");
                    log.warn("⚠️ AI 图片生成失败，使用模拟数据");
                    log.info("  🎨 模拟图片: {}", mockImage.getDescription());
                    log.info("  🔗 图片URL: {}", mockImage.getUrl());
                }
                
                // 发送节点完成事件
                if (workflowProgressService != null && context.getJobId() != null) {
                    Map<String, Object> nodeResult = new HashMap<>();
                    nodeResult.put("aiImage", context.getAiImage());
                    nodeResult.put("success", aiImage != null);
                    
                    WorkflowProgressEvent completedEvent = WorkflowProgressEvent.nodeCompleted(
                        context.getJobId(), context.getAppId(), "image_maker", "AI图片生成", nodeResult, 3, 5);
                    workflowProgressService.sendProgressEvent(completedEvent);
                }
                
                return WorkflowContext.saveContext(context);
                
            } catch (Exception e) {
                log.error("AI 图片生成过程中发生异常", e);
                
                // 异常处理：使用模拟数据
                ImageResource mockImage = createMockImage(context.getEnhancedPrompt());
                context.setAiImage(mockImage);
                context.setCurrentStep("AI 图片生成异常，使用模拟数据");
                
                return WorkflowContext.saveContext(context);
            }
        });
    }
    
    /**
     * 使用豆包 AI 生成图片
     */
    private static ImageResource generateImageWithDoubaoAI(String prompt, List<ImageResource> referenceImages) {
        try {
            log.info("调用豆包 AI 生成图片，提示词: {}", prompt);
            
            // 构建豆包 API 请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", DOUBAO_MODEL);
            requestBody.set("prompt", prompt);
            requestBody.set("response_format", "url");
            requestBody.set("size", "2K");
            requestBody.set("stream", false); // 不使用流式响应，简化处理
            requestBody.set("watermark", false);
            
            // 添加参考图片 URL（如果有的话）
            if (referenceImages != null && !referenceImages.isEmpty()) {
                JSONArray imageUrls = new JSONArray();
                for (ImageResource img : referenceImages) {
                    if (StrUtil.isNotBlank(img.getUrl())) {
                        imageUrls.add(img.getUrl());
                        // 最多使用前2张参考图片（根据豆包文档建议）
                        if (imageUrls.size() >= 2) {
                            break;
                        }
                    }
                }
                if (!imageUrls.isEmpty()) {
                    requestBody.set("image", imageUrls);
                    log.info("使用 {} 张参考图片", imageUrls.size());
                }
            }
            
            // 添加序列图片生成配置
            JSONObject seqOptions = new JSONObject();
            seqOptions.set("max_images", 1);
            requestBody.set("sequential_image_generation", "auto");
            requestBody.set("sequential_image_generation_options", seqOptions);
            
            log.info("🚀 发送豆包 AI 请求:");
            log.info("  📍 API URL: {}", DOUBAO_API_URL);
            log.info("  🔑 API Key: {}...{}", DOUBAO_API_KEY.substring(0, 8), DOUBAO_API_KEY.substring(DOUBAO_API_KEY.length() - 8));
            log.info("  🎯 模型: {}", DOUBAO_MODEL);
            log.debug("  📋 请求体: {}", requestBody.toString());
            
            // 发送 POST 请求
            HttpResponse response = HttpRequest.post(DOUBAO_API_URL)
                    .header("Authorization", "Bearer " + DOUBAO_API_KEY)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(TIMEOUT)
                    .execute();
            
            if (response.isOk()) {
                String responseBody = response.body();
                log.debug("豆包 AI 响应: {}", responseBody);
                
                // 解析响应
                ImageResource generatedImage = parseDoubaoResponse(responseBody, prompt);
                
                if (generatedImage != null) {
                    log.info("✅ 豆包 AI 图片生成成功: {}", generatedImage.getUrl());
                    return generatedImage;
                } else {
                    log.warn("⚠️ 豆包 AI 响应解析失败");
                }
                
            } else {
                String errorBody = response.body();
                log.error("❌ 豆包 AI 请求失败，状态码: {}, 响应: {}", response.getStatus(), errorBody);
                
                // 针对不同错误码提供具体建议
                if (response.getStatus() == 401) {
                    log.error("🔑 API Key 无效或已过期，请检查配置:");
                    log.error("   当前 API Key: {}...{}", 
                            DOUBAO_API_KEY.substring(0, 8), 
                            DOUBAO_API_KEY.substring(DOUBAO_API_KEY.length() - 8));
                    log.error("   请联系豆包 AI 获取有效的 API Key");
                } else if (response.getStatus() == 429) {
                    log.error("⏰ API 调用频率超限，请稍后重试");
                } else if (response.getStatus() >= 500) {
                    log.error("🔧 豆包 AI 服务器错误，请稍后重试");
                }
            }
            
        } catch (Exception e) {
            log.error("调用豆包 AI 时发生异常", e);
        }
        
        return null;
    }
    
    /**
     * 解析豆包 AI 响应
     */
    private static ImageResource parseDoubaoResponse(String responseBody, String prompt) {
        try {
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            
            // 豆包 API 响应格式：{"data": [{"url": "图片URL"}]}
            if (responseJson.containsKey("data")) {
                JSONArray dataArray = responseJson.getJSONArray("data");
                
                if (dataArray.size() > 0) {
                    JSONObject firstImage = dataArray.getJSONObject(0);
                    
                    // 获取图片 URL
                    String imageUrl = firstImage.getStr("url");
                    
                    if (StrUtil.isNotBlank(imageUrl)) {
                        return ImageResource.builder()
                                .description("豆包 AI 生成图片 - " + prompt)
                                .url(imageUrl)
                                .build();
                    }
                }
            }
            
            log.warn("豆包响应中未找到有效的图片 URL，响应结构: {}", responseJson.keySet());
            
        } catch (Exception e) {
            log.error("解析豆包 AI 响应失败", e);
        }
        
        return null;
    }
    
    /**
     * 解析云雾 AI 响应（保留作为备用）
     */
    private static ImageResource parseYunwuResponse(String responseBody, String prompt) {
        try {
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            
            // 检查是否有 images 字段
            if (responseJson.containsKey("images")) {
                JSONArray imagesArray = responseJson.getJSONArray("images");
                
                if (imagesArray.size() > 0) {
                    JSONObject firstImage = imagesArray.getJSONObject(0);
                    
                    // 获取图片 URL
                    String imageUrl = firstImage.getStr("url");
                    
                    if (StrUtil.isNotBlank(imageUrl)) {
                        return ImageResource.builder()
                                .description("AI 生成图片 - " + prompt)
                                .url(imageUrl)
                                .build();
                    }
                }
            }
            
            // 尝试其他可能的字段名
            String[] possibleFields = {"image_url", "result", "data", "output"};
            for (String field : possibleFields) {
                if (responseJson.containsKey(field)) {
                    String imageUrl = responseJson.getStr(field);
                    if (StrUtil.isNotBlank(imageUrl)) {
                        return ImageResource.builder()
                                .description("AI 生成图片 - " + prompt)
                                .url(imageUrl)
                                .build();
                    }
                }
            }
            
            log.warn("响应中未找到有效的图片 URL，可用字段: {}", responseJson.keySet());
            
        } catch (Exception e) {
            log.error("解析云雾 AI 响应失败", e);
        }
        
        return null;
    }
    
    /**
     * 创建模拟图片数据（降级方案）
     */
    private static ImageResource createMockImage(String prompt) {
        log.info("创建模拟 AI 图片数据，提示词: {}", prompt);
        
        // 根据提示词内容选择合适的模拟图片
        String mockUrl;
        String description;
        
        if (StrUtil.isNotBlank(prompt)) {
            String lowerPrompt = prompt.toLowerCase();
            
            if (lowerPrompt.contains("西安") || lowerPrompt.contains("古建筑") || lowerPrompt.contains("大雁塔")) {
                mockUrl = "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&q=80";
                description = "AI 生成图片 - 西安古建筑风格设计";
            } else if (lowerPrompt.contains("茶具") || lowerPrompt.contains("茶")) {
                mockUrl = "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=800&q=80";
                description = "AI 生成图片 - 茶具设计";
            } else if (lowerPrompt.contains("成都") || lowerPrompt.contains("火锅")) {
                mockUrl = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&q=80";
                description = "AI 生成图片 - 成都文化设计";
            } else if (lowerPrompt.contains("文创") || lowerPrompt.contains("工艺品")) {
                mockUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=800&q=80";
                description = "AI 生成图片 - 文创产品设计";
            } else {
                mockUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80";
                description = "AI 生成图片 - 创意设计";
            }
        } else {
            mockUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80";
            description = "AI 生成图片 - 默认设计";
        }
        
        return ImageResource.builder()
                .description(description)
                .url(mockUrl)
                .build();
    }
}