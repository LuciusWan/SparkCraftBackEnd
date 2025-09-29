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
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
@ConfigurationProperties(prefix = "spark.ai.qwen")
public class ProductionProcessNode {
    // 通义千问多模态 API 配置
    private static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String QWEN_MODEL = "qwen-omni-turbo";
    private static String QWEN_API_KEY;
    private static final int TIMEOUT = 60000; // 60秒超时
    
    // 静态配置属性，通过配置类注入
    private static AiServiceProperties aiServiceProperties;
    private static WorkflowProgressService workflowProgressService;
    public void setApiKey(String apiKey) {
        ProductionProcessNode.QWEN_API_KEY = apiKey;
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
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("🏭 开始执行生产工艺分析节点");
            
            // 发送节点开始事件
        if (workflowProgressService != null && context.getJobId() != null) {
            WorkflowProgressEvent startEvent = WorkflowProgressEvent.nodeStarted(
                context.getJobId(), context.getAppId(), "production_process", "生产工艺分析", 4, 5);
            workflowProgressService.sendProgressEvent(startEvent);
        }
            
            try {
                // 获取 AI 生成的图片和相关信息
                ImageResource aiImage = context.getAiImage();
                String enhancedPrompt = context.getEnhancedPrompt();
                String originalPrompt = context.getOriginalPrompt();
                
                if (aiImage == null || StrUtil.isBlank(aiImage.getUrl())) {
                    log.warn("⚠️ 未找到 AI 生成的图片，使用模拟生产工艺");
                    String mockProcess = createMockProductionProcess(originalPrompt);
                    context.setProductionProcess(mockProcess);
                    context.setCurrentStep("生产工艺分析完成（使用模拟数据）");
                } else {
                    log.info("📸 分析 AI 生成图片: {}", aiImage.getUrl());
                    log.info("📝 原始提示词: {}", originalPrompt);
                    
                    // 调用通义千问多模态 API 分析图片并生成生产工艺
                    String productionProcess = analyzeImageAndGenerateProcess(aiImage, originalPrompt, enhancedPrompt);
                    
                    if (StrUtil.isNotBlank(productionProcess)) {
                        context.setProductionProcess(productionProcess);
                        context.setCurrentStep("生产工艺分析完成");
                        log.info("✅ 生产工艺分析成功");
                    } else {
                        // 分析失败，使用模拟数据
                        String mockProcess = createMockProductionProcess(originalPrompt);
                        context.setProductionProcess(mockProcess);
                        context.setCurrentStep("生产工艺分析失败，使用模拟数据");
                        log.warn("⚠️ 生产工艺分析失败，使用模拟数据");
                    }
                }
                
                // 发送节点完成事件
                if (workflowProgressService != null && context.getJobId() != null) {
                    Map<String, Object> nodeResult = new HashMap<>();
                    nodeResult.put("productionProcess", context.getProductionProcess());
                    
                    WorkflowProgressEvent completedEvent = WorkflowProgressEvent.nodeCompleted(
                        context.getJobId(), context.getAppId(), "production_process", "生产工艺分析", nodeResult, 4, 5);
                    workflowProgressService.sendProgressEvent(completedEvent);
                }
                
                return WorkflowContext.saveContext(context);
                
            } catch (Exception e) {
                log.error("生产工艺分析过程中发生异常", e);
                
                // 异常处理：使用模拟数据
                String mockProcess = createMockProductionProcess(context.getOriginalPrompt());
                context.setProductionProcess(mockProcess);
                context.setCurrentStep("生产工艺分析异常，使用模拟数据");
                
                return WorkflowContext.saveContext(context);
            }
        });
    }
    
    /**
     * 使用通义千问多模态 API 分析图片并生成生产工艺
     */
    private static String analyzeImageAndGenerateProcess(ImageResource aiImage, String originalPrompt, String enhancedPrompt) {
        try {
            log.info("🚀 调用通义千问多模态 API 分析图片");
            
            // 构建多模态请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", QWEN_MODEL);
            requestBody.set("stream", false); // 不使用流式响应，简化处理
            requestBody.set("max_tokens", 700); // 设置最大token数
            requestBody.set("temperature", 0.7); // 设置创造性参数
            
            // 构建消息数组
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.set("role", "user");
            
            // 构建内容数组（包含图片和文本）
            JSONArray content = new JSONArray();
            
            // 添加图片内容
            JSONObject imageContent = new JSONObject();
            imageContent.set("type", "image_url");
            JSONObject imageUrl = new JSONObject();
            imageUrl.set("url", aiImage.getUrl());
            // 添加图片详细度设置
            imageUrl.set("detail", "high");
            imageContent.set("image_url", imageUrl);
            content.add(imageContent);
            
            // 添加文本内容
            JSONObject textContent = new JSONObject();
            textContent.set("type", "text");
            String promptText = buildAnalysisPrompt(originalPrompt, enhancedPrompt);
            textContent.set("text", promptText);
            content.add(textContent);
            
            message.set("content", content);
            messages.add(message);
            requestBody.set("messages", messages);
            
            String apiKey = QWEN_API_KEY;
            if (StrUtil.isBlank(apiKey)) {
                log.error("❌ 通义千问 API Key 未配置");
                return "生产工艺分析失败 - API Key 未配置";
            }
            
            log.info("📍 API URL: {}", QWEN_API_URL);
            log.info("🔑 API Key: {}...{}", apiKey.substring(0, 8), apiKey.substring(apiKey.length() - 8));
            log.info("🎯 模型: {}", QWEN_MODEL);
            log.info("🖼️ 图片URL: {}", aiImage.getUrl());
            log.debug("📋 请求体: {}", requestBody.toString());
            
            // 发送 POST 请求
            HttpResponse response = HttpRequest.post(QWEN_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "SparkCraft-Backend/1.0")
                    .body(requestBody.toString())
                    .timeout(TIMEOUT)
                    .execute();
            
            log.info("📡 API响应状态码: {}", response.getStatus());
            
            if (response.isOk()) {
                String responseBody = response.body();
                log.info("✅ 通义千问API调用成功");
                log.debug("📄 响应内容: {}", responseBody);
                
                // 解析响应
                String productionProcess = parseQwenResponse(responseBody);
                
                if (StrUtil.isNotBlank(productionProcess)) {
                    log.info("🎯 生产工艺分析成功，内容长度: {} 字符", productionProcess.length());
                    return productionProcess;
                } else {
                    log.warn("⚠️ 通义千问响应解析失败，响应为空");
                }
                
            } else {
                String errorBody = response.body();
                log.error("❌ 通义千问请求失败，状态码: {}, 响应: {}", response.getStatus(), errorBody);
                
                // 针对不同错误码提供具体建议
                switch (response.getStatus()) {
                    case 400:
                        log.error("🚫 请求参数错误，请检查请求格式");
                        break;
                    case 401:
                        log.error("🔑 API Key 无效或已过期，请检查配置");
                        break;
                    case 403:
                        log.error("🚪 访问被拒绝，请检查API权限");
                        break;
                    case 429:
                        log.error("⏰ API 调用频率超限，请稍后重试");
                        break;
                    case 500:
                    case 502:
                    case 503:
                        log.error("🔧 通义千问服务器错误，请稍后重试");
                        break;
                    default:
                        log.error("❓ 未知错误，状态码: {}", response.getStatus());
                }
            }
            
        } catch (Exception e) {
            log.error("🔥 调用通义千问 API 时发生异常: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 构建分析提示词
     */
    private static String buildAnalysisPrompt(String originalPrompt, String enhancedPrompt) {
        return String.format("""
            你是一位专业的文创产品制作工艺专家，请仔细分析这张文创产品设计图片，并生成详细的制作工艺流程。
            
            **用户需求信息：**
            - 原始需求：%s
            - 设计描述：%s
            
            **分析要求：**
            请基于图片内容，从专业制作角度进行深入分析，提供可实际操作的制作指导。
            
            **输出格式要求：**
            
            ## 1. 产品设计分析
            - **产品类型**：明确产品分类（如茶具、摆件、文具等）
            - **设计风格**：分析设计元素、色彩搭配、文化内涵
            - **功能特点**：产品的实用性和装饰性分析
            - **尺寸规格**：基于图片推测的合理尺寸建议
            - **技术难点**：制作过程中的关键技术要求
            
            ## 2. 材料选择与规格
            - **主要材料**：详细列出所需主材料及其特性要求
            - **辅助材料**：胶粘剂、涂料、五金配件等
            - **材料规格**：具体的规格参数和质量标准
            - **用量估算**：基于产品尺寸的材料用量计算
            - **采购建议**：材料来源和选购要点
            
            ## 3. 详细制作工艺流程
            - **前期准备**：设计图纸、模具制作、材料准备
            - **制作步骤**：
              1. 第一步：具体操作内容、技术要点、注意事项
              2. 第二步：具体操作内容、技术要点、注意事项
              3. （继续列出所有关键步骤）
            - **工具设备**：每个步骤所需的专业工具和设备
            - **技术参数**：温度、时间、压力等关键参数
            - **制作周期**：各阶段时间安排和总体周期
            
            ## 4. 质量控制体系
            - **关键控制点**：制作过程中的质量检查节点
            - **检测标准**：外观、尺寸、功能性等检测要求
            - **常见缺陷**：可能出现的质量问题及预防措施
            - **返工处理**：不合格品的处理方案
            - **最终验收**：成品的验收标准和流程
            
            ## 5. 成本分析与控制
            - **材料成本**：详细的材料费用分解
            - **人工成本**：各工序的人工时间和费用
            - **设备成本**：设备使用费和折旧费
            - **其他费用**：包装、运输、管理费用等
            - **成本优化**：降低成本的可行性建议
            - **定价建议**：基于成本的合理定价区间
            
            ## 6. 生产建议
            - **批量生产**：规模化生产的可行性分析
            - **工艺改进**：提高效率和质量的改进建议
            - **设备投资**：必要的设备投资建议
            - **人员配置**：生产团队的技能要求和人员配置
            
            **注意事项：**
            1. 分析要基于图片的实际内容，不要脱离图片进行臆测
            2. 工艺流程要具有可操作性，避免过于理论化
            3. 成本估算要相对准确，考虑当前市场价格
            4. 质量标准要符合相关行业规范
            5. 用词要专业准确，适合制作人员参考使用
            
            请用中文详细回答，确保内容专业、实用、可操作。
            """, originalPrompt, enhancedPrompt != null ? enhancedPrompt : originalPrompt);
    }
    
    /**
     * 解析通义千问响应
     */
    private static String parseQwenResponse(String responseBody) {
        try {
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            
            // 通义千问 API 响应格式：{"choices": [{"message": {"content": "..."}}]}
            if (responseJson.containsKey("choices")) {
                JSONArray choices = responseJson.getJSONArray("choices");
                
                if (choices.size() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    
                    if (firstChoice.containsKey("message")) {
                        JSONObject message = firstChoice.getJSONObject("message");
                        String content = message.getStr("content");
                        
                        if (StrUtil.isNotBlank(content)) {
                            return content.trim();
                        }
                    }
                }
            }
            
            log.warn("通义千问响应中未找到有效内容，响应结构: {}", responseJson.keySet());
            
        } catch (Exception e) {
            log.error("解析通义千问响应失败", e);
        }
        
        return null;
    }
    
    /**
     * 创建模拟生产工艺数据（降级方案）
     */
    private static String createMockProductionProcess(String originalPrompt) {
        log.info("创建模拟生产工艺数据，原始提示词: {}", originalPrompt);
        
        // 根据提示词内容生成相应的模拟工艺
        String productType = "文创产品";
        String materials = "优质材料";
        String process = "传统工艺制作";
        
        if (StrUtil.isNotBlank(originalPrompt)) {
            String lowerPrompt = originalPrompt.toLowerCase();
            
            if (lowerPrompt.contains("茶具") || lowerPrompt.contains("茶")) {
                productType = "茶具套装";
                materials = "优质陶瓷、天然釉料";
                process = "陶瓷成型 → 素烧 → 施釉 → 釉烧 → 装饰";
            } else if (lowerPrompt.contains("摆件") || lowerPrompt.contains("装饰")) {
                productType = "装饰摆件";
                materials = "树脂、金属、颜料";
                process = "模具制作 → 浇注成型 → 打磨 → 上色 → 包装";
            } else if (lowerPrompt.contains("文具") || lowerPrompt.contains("笔")) {
                productType = "文具用品";
                materials = "木材、金属配件、环保漆";
                process = "木材切割 → 精细加工 → 组装 → 表面处理 → 质检";
            }
        }
        
        return String.format("""
            # %s 制作工艺流程
            
            ## 1. 产品分析
            - **产品类型**: %s
            - **设计风格**: 融合传统文化与现代美学
            - **规格尺寸**: 根据实际需求定制
            
            ## 2. 原材料清单
            - **主要材料**: %s
            - **辅助材料**: 包装材料、标签、说明书
            - **工具设备**: 专业制作工具、检测设备
            
            ## 3. 制作工艺流程
            %s
            
            ## 4. 质量控制
            - **检查要点**: 外观质量、尺寸精度、功能性能
            - **验收标准**: 符合设计要求，无明显缺陷
            
            ## 5. 成本估算
            - **材料成本**: 根据材料用量计算
            - **人工成本**: 按制作工时计算
            - **预估周期**: 7-15个工作日
            
            *注：此为模拟数据，实际制作请根据具体设计图纸调整*
            """, productType, productType, materials, process);
    }
}
