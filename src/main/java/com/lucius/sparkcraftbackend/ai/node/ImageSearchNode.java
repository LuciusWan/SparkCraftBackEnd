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
import com.lucius.sparkcraftbackend.properties.AiServiceProperties;
import com.lucius.sparkcraftbackend.service.WorkflowProgressService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "spark.ai.coze")
public class ImageSearchNode {
    
    // Coze API 配置常量
    private static final String COZE_API_URL = "https://api.coze.cn/v1/workflow/stream_run";
    private static String COZE_API_TOKEN;
    private static final String COZE_WORKFLOW_ID = "7554768700659515435";
    private static final int COZE_TIMEOUT = 30000;
    
    // 静态配置属性，通过配置类注入
    private static AiServiceProperties aiServiceProperties;
    private static WorkflowProgressService workflowProgressService;
    public void setApiKey(String apiKey) {
        ImageSearchNode.COZE_API_TOKEN = apiKey;
    }
    /**
     * 设置AI服务配置属性
     * @param properties AI服务配置属性
     */
    public static void setAiServiceProperties(AiServiceProperties properties) {
        aiServiceProperties = properties;
    }

    public static void setWorkflowProgressService(WorkflowProgressService service) {
        workflowProgressService = service;
    }
    
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            log.info("开始执行 Coze 图片搜索节点");
            
            try {
                // 从 WorkflowContext 中获取关键词
                WorkflowContext context = WorkflowContext.getContext(state);
                String keyPoint = context.getKeyPoint();
                
                // 发送节点开始事件
                if (workflowProgressService != null && context.getJobId() != null) {
                    WorkflowProgressEvent startEvent = WorkflowProgressEvent.nodeStarted(
                        context.getJobId(), context.getAppId(), "image_collector", "图片素材收集", 2, 5);
                    workflowProgressService.sendProgressEvent(startEvent);
                }
                
                if (StrUtil.isBlank(keyPoint)) {
                    log.warn("关键词为空，跳过图片搜索");
                    context.setCurrentStep("图片搜索（跳过）");
                    context.setImageList(new ArrayList<>());
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("messages", "图片搜索跳过（关键词为空）");
                    return result;
                }
                
                log.info("开始使用 Coze API 搜索图片，关键词: [{}]", keyPoint);
                
                // 1. 调用 Coze API 搜索图片
                List<ImageResource> searchResults = searchImagesWithCoze(keyPoint);
                
                // 2. 更新 WorkflowContext
                context.setCurrentStep("Coze 图片搜索完成");
                context.setImageList(searchResults);
                
                log.info("Coze 图片搜索完成，共获取 {} 张图片", searchResults.size());
                
                // 发送节点完成事件
                if (workflowProgressService != null && context.getJobId() != null) {
                    Map<String, Object> nodeResult = new HashMap<>();
                    nodeResult.put("imageCount", searchResults.size());
                    nodeResult.put("images", searchResults);
                    
                    WorkflowProgressEvent completedEvent = WorkflowProgressEvent.nodeCompleted(
                        context.getJobId(), context.getAppId(), "image_collector", "图片素材收集", nodeResult, 2, 5);
                    workflowProgressService.sendProgressEvent(completedEvent);
                }
                
                // 返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("messages", String.format("Coze 图片搜索完成，找到 %d 张相关图片", searchResults.size()));
                return result;
                
            } catch (Exception e) {
                log.error("Coze 图片搜索过程中发生异常", e);
                
                // 异常处理：使用模拟数据
                WorkflowContext context = WorkflowContext.getContext(state);
                List<ImageResource> mockImages = getMockImages(context.getKeyPoint(), 2);
                context.setCurrentStep("Coze 图片搜索失败，使用模拟数据");
                context.setImageList(mockImages);
                
                Map<String, Object> result = new HashMap<>();
                result.put("messages", "Coze 图片搜索失败，使用模拟数据");
                return result;
            }
        });
    }
    
    /**
     * 使用 Coze API 搜索图片
     */
    private static List<ImageResource> searchImagesWithCoze(String keyPoint) {
        List<ImageResource> images = new ArrayList<>();
        
        try {
            String apiToken = COZE_API_TOKEN;
            if (StrUtil.isBlank(apiToken)) {
                log.error("❌ Coze API Token 未配置");
                return images;
            }
            
            log.info("调用 Coze API，工作流ID: {}, 关键词: {}", COZE_WORKFLOW_ID, keyPoint);
            log.debug("使用 Token: {}...{}", apiToken.substring(0, 10), apiToken.substring(apiToken.length() - 10));
            
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("workflow_id", COZE_WORKFLOW_ID);
            
            JSONObject parameters = new JSONObject();
            parameters.put("mainpotic", keyPoint);
            requestBody.put("parameters", parameters);
            
            log.debug("Coze API 请求体: {}", requestBody.toString());
            
            // 发送 POST 请求
            HttpResponse response = HttpRequest.post(COZE_API_URL)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(COZE_TIMEOUT)
                    .execute();
            
            if (response.isOk()) {
                String responseBody = response.body();
                log.debug("Coze API 响应: {}", responseBody);
                
                // 解析流式响应
                images = parseCozeStreamResponse(responseBody, keyPoint);
                
                if (images.isEmpty()) {
                    log.warn("Coze API 返回成功但未解析到图片，使用模拟数据");
                    images = getMockImages(keyPoint, 2);
                }
                
            } else {
                log.error("Coze API 请求失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                images = getMockImages(keyPoint, 2);
            }
            
        } catch (Exception e) {
            log.error("调用 Coze API 时发生异常", e);
            images = getMockImages(keyPoint, 2);
        }
        
        return images;
    }
    
    /**
     * 解析 Coze 流式响应 (Server-Sent Events 格式)
     */
    private static List<ImageResource> parseCozeStreamResponse(String responseBody, String keyPoint) {
        List<ImageResource> images = new ArrayList<>();
        
        try {
            log.info("开始解析 Coze SSE 响应，响应长度: {} 字符", responseBody.length());
            log.debug("完整响应内容: {}", responseBody);
            
            // 按行分割响应，处理 SSE 格式
            BufferedReader reader = new BufferedReader(new StringReader(responseBody));
            String line;
            int lineCount = 0;
            String currentData = null;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                line = line.trim();
                if (StrUtil.isBlank(line)) {
                    continue;
                }
                
                log.debug("解析第 {} 行: {}", lineCount, line);
                
                // 解析 SSE 格式
                if (line.startsWith("data: ")) {
                    currentData = line.substring(6); // 移除 "data: " 前缀
                    log.info("找到 data 行，内容长度: {} 字符", currentData.length());
                    
                    try {
                        // 解析 JSON 数据
                        JSONObject dataJson = JSONUtil.parseObj(currentData);
                        
                        String nodeType = dataJson.getStr("node_type");
                        String nodeTitle = dataJson.getStr("node_title");
                        boolean nodeIsFinish = dataJson.getBool("node_is_finish", false);
                        
                        log.info("解析到节点: type={}, title={}, isFinish={}", nodeType, nodeTitle, nodeIsFinish);
                        
                        // 检查是否包含 content 字段
                        if (dataJson.containsKey("content")) {
                            String content = dataJson.getStr("content");
                            
                            if (StrUtil.isNotBlank(content)) {
                                log.info("找到 content 字段，内容长度: {} 字符", content.length());
                                log.debug("Content 内容: {}", content);
                                
                                // 解析 content 中的图片信息
                                List<ImageResource> parsedImages = parseImageContent(content, keyPoint);
                                if (!parsedImages.isEmpty()) {
                                    images.addAll(parsedImages);
                                    log.info("从第 {} 行解析到 {} 张图片", lineCount, parsedImages.size());
                                }
                            }
                        }
                        
                    } catch (Exception e) {
                        log.warn("解析 data JSON 失败: {}, 错误: {}", currentData, e.getMessage());
                    }
                }
                // 其他 SSE 字段（id, event 等）暂时忽略
                else if (line.startsWith("id: ") || line.startsWith("event: ")) {
                    log.debug("SSE 元数据: {}", line);
                }
            }
            
            log.info("SSE 响应解析完成，共处理 {} 行，解析到 {} 张图片", lineCount, images.size());
            
            // 如果没有解析到图片，尝试直接用正则表达式从整个响应中提取
            if (images.isEmpty()) {
                log.info("JSON 解析未找到图片，尝试从整个响应中用正则表达式提取");
                images = extractUrlsWithRegex(responseBody, keyPoint);
            }
            
        } catch (Exception e) {
            log.error("解析 Coze SSE 响应失败", e);
            // 异常情况下也尝试正则表达式提取
            log.info("异常情况下尝试正则表达式提取");
            images = extractUrlsWithRegex(responseBody, keyPoint);
        }
        
        return images;
    }
    
    /**
     * 解析图片内容
     */
    private static List<ImageResource> parseImageContent(String content, String keyPoint) {
        List<ImageResource> images = new ArrayList<>();
        
        try {
            log.debug("开始解析图片内容: {}", content);
            
            // 尝试解析 JSON
            JSONObject contentJson = JSONUtil.parseObj(content);
            log.debug("成功解析为 JSON，包含的键: {}", contentJson.keySet());
            
            // 检查是否包含 imageurl 字段
            if (contentJson.containsKey("imageurl")) {
                JSONArray imageArray = contentJson.getJSONArray("imageurl");
                log.info("找到 imageurl 数组，包含 {} 个元素", imageArray.size());
                
                for (int i = 0; i < imageArray.size(); i++) {
                    JSONObject imageObj = imageArray.getJSONObject(i);
                    log.debug("处理第 {} 个图片对象: {}", i + 1, imageObj);
                    
                    if (imageObj.containsKey("picture_info")) {
                        JSONObject pictureInfo = imageObj.getJSONObject("picture_info");
                        log.debug("图片信息: {}", pictureInfo);
                        
                        String displayUrl = pictureInfo.getStr("display_url");
                        String title = pictureInfo.getStr("title", keyPoint + " 相关图片");
                        
                        if (StrUtil.isNotBlank(displayUrl)) {
                            ImageResource image = ImageResource.builder()
                                    .description(title + " - " + keyPoint)
                                    .url(displayUrl)
                                    .build();
                            
                            images.add(image);
                            
                            log.info("✅ 成功解析图片 {}: {} - {}", i + 1, title, displayUrl);
                        } else {
                            log.warn("⚠️ 第 {} 个图片的 display_url 为空", i + 1);
                        }
                    } else {
                        log.warn("⚠️ 第 {} 个图片对象缺少 picture_info 字段", i + 1);
                    }
                }
            } else {
                log.warn("⚠️ Content 中未找到 imageurl 字段，可用字段: {}", contentJson.keySet());
                
                // 尝试其他可能的字段名
                String[] possibleKeys = {"images", "image_list", "picture_list", "results"};
                for (String key : possibleKeys) {
                    if (contentJson.containsKey(key)) {
                        log.info("尝试使用字段: {}", key);
                        // 可以在这里添加其他字段的解析逻辑
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("解析图片内容失败，Content: {}", content, e);
            
            // 尝试直接在字符串中查找 URL 模式
            log.info("尝试使用正则表达式直接提取图片 URL");
            images.addAll(extractUrlsWithRegex(content, keyPoint));
        }
        
        return images;
    }

    /**
     * 使用正则表达式直接提取图片 URL（备用方法）
     */
    private static List<ImageResource> extractUrlsWithRegex(String content, String keyPoint) {
        List<ImageResource> images = new ArrayList<>();
        
        try {
            log.info("使用正则表达式提取图片 URL，内容长度: {}", content.length());
            
            // 查找 display_url 模式
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"display_url\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            int count = 0;
            while (matcher.find() && count < 3) {
                String url = matcher.group(1);
                if (StrUtil.isNotBlank(url)) {
                    ImageResource image = ImageResource.builder()
                            .description(keyPoint + " - 图片" + (count + 1))
                            .url(url)
                            .build();
                    
                    images.add(image);
                    count++;
                    
                    log.info("🔍 正则表达式提取到图片 {}: {}", count, url);
                }
            }
            
            // 如果还没找到，尝试更宽松的模式
            if (images.isEmpty()) {
                log.info("尝试更宽松的正则表达式模式");
                java.util.regex.Pattern relaxedPattern = java.util.regex.Pattern.compile("https://[^\\s\"']+\\.(jpeg|jpg|png|gif|webp)", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher relaxedMatcher = relaxedPattern.matcher(content);
                
                count = 0;
                while (relaxedMatcher.find() && count < 3) {
                    String url = relaxedMatcher.group();
                    if (StrUtil.isNotBlank(url)) {
                        ImageResource image = ImageResource.builder()
                                .description(keyPoint + " - 图片" + (count + 1))
                                .url(url)
                                .build();
                        
                        images.add(image);
                        count++;
                        
                        log.info("🔍 宽松模式提取到图片 {}: {}", count, url);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("正则表达式提取 URL 失败", e);
        }
        
        return images;
    }
    
    /**
     * 获取模拟图片数据（降级方案）
     */
    private static List<ImageResource> getMockImages(String keywords, int count) {
        List<ImageResource> images = new ArrayList<>();
        
        log.info("使用模拟数据提供图片素材，关键词: {}", keywords);
        
        if (StrUtil.isNotBlank(keywords)) {
            String lowerKeywords = keywords.toLowerCase();
            
            // 西安古建筑相关
            if (lowerKeywords.contains("西安") || lowerKeywords.contains("古建筑") || lowerKeywords.contains("大雁塔")) {
                images.add(ImageResource.builder()
                        .description("西安大雁塔 - " + keywords)
                        .url("https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&q=80")
                        .build());
                images.add(ImageResource.builder()
                        .description("西安古城墙 - " + keywords)
                        .url("https://images.unsplash.com/photo-1547036967-23d11aacaee0?w=800&q=80")
                        .build());
            }
            // 茶具相关
            else if (lowerKeywords.contains("茶具") || lowerKeywords.contains("茶") || lowerKeywords.contains("紫砂")) {
                images.add(ImageResource.builder()
                        .description("精美茶具 - " + keywords)
                        .url("https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=800&q=80")
                        .build());
                images.add(ImageResource.builder()
                        .description("茶具套装 - " + keywords)
                        .url("https://images.unsplash.com/photo-1571934811356-5cc061b6821f?w=800&q=80")
                        .build());
            }
            // 成都相关
            else if (lowerKeywords.contains("成都") || lowerKeywords.contains("火锅")) {
                images.add(ImageResource.builder()
                        .description("成都火锅 - " + keywords)
                        .url("https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&q=80")
                        .build());
                images.add(ImageResource.builder()
                        .description("成都文化 - " + keywords)
                        .url("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80")
                        .build());
            }
            // 文创产品相关
            else if (lowerKeywords.contains("文创") || lowerKeywords.contains("工艺品")) {
                images.add(ImageResource.builder()
                        .description("文创产品 - " + keywords)
                        .url("https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=800&q=80")
                        .build());
                images.add(ImageResource.builder()
                        .description("传统工艺 - " + keywords)
                        .url("https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=800&q=80")
                        .build());
            }
            // 默认图片
            else {
                images.add(ImageResource.builder()
                        .description(keywords + " - 参考图片1")
                        .url("https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=800&q=80")
                        .build());
                images.add(ImageResource.builder()
                        .description(keywords + " - 参考图片2")
                        .url("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80")
                        .build());
            }
        }
        
        // 限制数量
        return images.subList(0, Math.min(images.size(), count));
    }
}
