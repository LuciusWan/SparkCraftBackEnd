package com.lucius.sparkcraftbackend.ai.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import com.lucius.sparkcraftbackend.entity.ChatMemoryEntity;
import com.lucius.sparkcraftbackend.mapper.ChatMemoryMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Data
public class PromptEnhancerNode {
    
    private static ChatMemoryMapper chatMemoryMapper;
    private static ChatClient promptSummeryChatClient;
    private static ChatClient keyPointExtractionChatClient;

    // 静态方法设置依赖注入的服务
    public static void setChatMemoryMapper(ChatMemoryMapper mapper) {
        chatMemoryMapper = mapper;
    }

    public static void setPromptSummeryChatClient(ChatClient client) {
        promptSummeryChatClient = client;
    }

    public static void setKeyPointExtractionChatClient(ChatClient client) {
        keyPointExtractionChatClient = client;
    }
    
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            log.info("开始执行提示词增强节点");
            
            try {
                // 从 WorkflowContext 中获取必要信息
                WorkflowContext context = WorkflowContext.getContext(state);
                Long appId = context.getAppId();
                String originalPrompt = context.getOriginalPrompt();
                
                log.debug("获取到的上下文信息 - appId: {}, originalPrompt: {}", appId, originalPrompt);
                
                if (appId == null) {
                    log.warn("无法获取 appId，跳过提示词增强");
                    context.setCurrentStep("提示词增强（跳过）");
                    context.setEnhancedPrompt(originalPrompt != null ? originalPrompt : "默认提示词");
                    
                    // 返回结果
                    Map<String, Object> result = new HashMap<>();
                    result.put("messages", context.getEnhancedPrompt());
                    return result;
                }
                
                log.info("当前处理的 appId: {}, 原始提示词: {}", appId, originalPrompt);
                
                // 1. 查询 chatMemory 表获取对话记录
                List<ChatMemoryEntity> chatMemories = getChatMemoriesByAppId(appId);
                log.info("查询到 {} 条对话记录", chatMemories.size());
                
                // 2. 构建对话记录摘要
                String chatSummary = buildChatSummary(chatMemories);
                log.debug("对话记录摘要构建完成，长度: {}", chatSummary.length());
                
                // 3. 使用 Spring AI 的 promptSummeryChatClient 进行总结和增强
                String enhancedPrompt = enhancePromptWithAI(chatSummary, originalPrompt);
                
                // 4. 验证增强结果
                if (StrUtil.isBlank(enhancedPrompt)) {
                    log.warn("AI 增强提示词失败，使用原始提示词");
                    enhancedPrompt = originalPrompt != null ? originalPrompt : "默认提示词";
                }
                
                // 5. 生成关键词用于图片搜索
                String keyPoint = extractKeyPointWithAI(enhancedPrompt);
                
                // 6. 更新 WorkflowContext
                context.setCurrentStep("提示词增强完成");
                context.setEnhancedPrompt(enhancedPrompt);
                context.setKeyPoint(keyPoint);
                
                log.info("提示词增强成功完成 - 原始长度: {}, 增强后长度: {}, 关键词: {}", 
                        originalPrompt != null ? originalPrompt.length() : 0, enhancedPrompt.length(), keyPoint);
                
                // 返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("messages", enhancedPrompt);
                return result;
                
            } catch (Exception e) {
                log.error("提示词增强过程中发生异常", e);
                
                // 异常处理：返回默认消息
                Map<String, Object> result = new HashMap<>();
                result.put("messages", "提示词增强失败，使用默认逻辑");
                return result;
            }
        });
    }
    
    /**
     * 根据 appId 查询 chatMemory 表中的对话记录
     */
    private static List<ChatMemoryEntity> getChatMemoriesByAppId(Long appId) {
        if (chatMemoryMapper == null) {
            log.warn("ChatMemoryMapper 未注入，无法查询对话记录");
            return List.of();
        }
        
        if (appId == null || appId <= 0) {
            log.warn("无效的 appId: {}，无法查询对话记录", appId);
            return List.of();
        }
        
        try {
            log.debug("开始查询 appId: {} 的对话记录", appId);
            
            // 使用 appId 作为 conversationId 查询最近20条记录
            String conversationId = String.valueOf(appId);
            List<ChatMemoryEntity> chatMemories = chatMemoryMapper.getMessages(conversationId, 20);
            
            if (chatMemories == null) {
                log.warn("查询结果为 null，appId: {}", appId);
                return List.of();
            }
            
            log.debug("成功查询到 {} 条对话记录，appId: {}", chatMemories.size(), appId);
            return chatMemories;
            
        } catch (Exception e) {
            log.error("查询对话记录时发生异常，appId: {}", appId, e);
            return List.of();
        }
    }
    
    /**
     * 构建对话记录摘要
     */
    private static String buildChatSummary(List<ChatMemoryEntity> chatMemories) {
        if (CollUtil.isEmpty(chatMemories)) {
            log.debug("对话记录为空");
            return "暂无历史对话记录。";
        }
        
        try {
            StringBuilder summary = new StringBuilder();
            summary.append("=== 历史对话记录 ===\n");
            
            // 过滤掉无效记录并按时间排序
            List<ChatMemoryEntity> validMemories = chatMemories.stream()
                    .filter(memory -> memory != null && StrUtil.isNotBlank(memory.getContent()))
                    .sorted((m1, m2) -> {
                        if (m1.getCreatedAt() == null && m2.getCreatedAt() == null) return 0;
                        if (m1.getCreatedAt() == null) return 1;
                        if (m2.getCreatedAt() == null) return -1;
                        return m1.getCreatedAt().compareTo(m2.getCreatedAt());
                    })
                    .collect(Collectors.toList());
            
            if (validMemories.isEmpty()) {
                log.debug("过滤后的对话记录为空");
                return "暂无有效的历史对话记录。";
            }
            
            // 构建对话记录摘要
            for (ChatMemoryEntity memory : validMemories) {
                String messageType = determineMessageType(memory.getMessageType());
                String content = truncateMessage(memory.getContent(), 500); // 限制单条消息长度
                summary.append(String.format("[%s]: %s\n", messageType, content));
            }
            
            summary.append("=== 对话记录结束 ===\n");
            
            String result = summary.toString();
            log.debug("对话记录摘要构建完成，包含 {} 条记录，总长度: {}", 
                     validMemories.size(), result.length());
            
            return result;
            
        } catch (Exception e) {
            log.error("构建对话记录摘要时发生异常", e);
            return "构建对话记录摘要失败。";
        }
    }
    
    /**
     * 确定消息类型的显示名称
     */
    private static String determineMessageType(String messageType) {
        if (StrUtil.isBlank(messageType)) {
            return "未知";
        }
        
        switch (messageType.toLowerCase()) {
            case "usermessage":
                return "用户";
            case "assistantmessage":
                return "AI";
            default:
                return "系统";
        }
    }
    
    /**
     * 截断过长的消息内容
     */
    private static String truncateMessage(String message, int maxLength) {
        if (StrUtil.isBlank(message)) {
            return "[空消息]";
        }
        
        if (message.length() <= maxLength) {
            return message.trim();
        }
        
        return message.substring(0, maxLength).trim() + "...";
    }
    
    /**
     * 使用 Spring AI 的 promptSummeryChatClient 增强提示词
     */
    private static String enhancePromptWithAI(String chatSummary, String originalPrompt) {
        try {
            // 验证 promptSummeryChatClient 是否可用
            if (promptSummeryChatClient == null) {
                log.warn("promptSummeryChatClient 未注入，跳过 AI 总结");
                return buildFallbackPrompt(chatSummary, originalPrompt);
            }
            
            // 构建用于 AI 总结的输入内容
            StringBuilder inputContent = new StringBuilder();
            inputContent.append("请根据以下历史对话记录，总结用户的需求和设计方向：\n\n");
            inputContent.append(chatSummary);
            
            if (StrUtil.isNotBlank(originalPrompt)) {
                inputContent.append("\n\n当前用户输入：").append(originalPrompt);
            }
            
            log.debug("开始使用 AI 总结对话记录，输入长度: {}", inputContent.length());
            
            // 使用 Spring AI 进行总结
            String aiSummary = promptSummeryChatClient.prompt()
                    .user(inputContent.toString())
                    .call()
                    .content();
            
            // 验证 AI 总结结果
            if (StrUtil.isBlank(aiSummary)) {
                log.warn("AI 总结结果为空，使用降级方案");
                return buildFallbackPrompt(chatSummary, originalPrompt);
            }
            
            // 构建最终的增强提示词
            StringBuilder enhancedPrompt = new StringBuilder();
            enhancedPrompt.append("=== 对话记忆总结 ===\n");
            enhancedPrompt.append(aiSummary.trim()).append("\n\n");
            
            // 添加用户当前输入
            if (StrUtil.isNotBlank(originalPrompt)) {
                enhancedPrompt.append("=== 用户当前输入 ===\n");
                enhancedPrompt.append(originalPrompt.trim());
            }
            
            String result = enhancedPrompt.toString();
            log.debug("AI 增强提示词构建成功，总长度: {}", result.length());
            
            return result;
            
        } catch (Exception e) {
            log.error("使用 AI 增强提示词时发生异常", e);
            // 降级处理：使用简单的拼接方式
            return buildFallbackPrompt(chatSummary, originalPrompt);
        }
    }
    
    /**
     * 降级方案：不使用 AI 总结，直接拼接历史记录和当前输入
     */
    private static String buildFallbackPrompt(String chatSummary, String originalPrompt) {
        StringBuilder fallbackPrompt = new StringBuilder();
        
        // 添加历史对话记录（如果有的话）
        if (StrUtil.isNotBlank(chatSummary) && !chatSummary.equals("暂无历史对话记录。")) {
            fallbackPrompt.append("=== 历史对话记录 ===\n");
            fallbackPrompt.append(chatSummary.trim()).append("\n\n");
        }
        
        // 添加当前用户输入
        if (StrUtil.isNotBlank(originalPrompt)) {
            fallbackPrompt.append("=== 用户当前输入 ===\n");
            fallbackPrompt.append(originalPrompt.trim());
        }
        
        String result = fallbackPrompt.toString();
        if (StrUtil.isBlank(result)) {
            result = "请提供您的需求，我将为您提供帮助。";
        }
        
        log.info("使用降级方案构建提示词，长度: {}", result.length());
        return result;
    }
    
    /**
     * 使用 Spring AI 的 keyPointExtractionChatClient 提取关键词
     */
    private static String extractKeyPointWithAI(String enhancedPrompt) {
        try {
            // 验证 keyPointExtractionChatClient 是否可用
            if (keyPointExtractionChatClient == null) {
                log.warn("keyPointExtractionChatClient 未注入，跳过关键词提取");
                return extractKeyPointFallback(enhancedPrompt);
            }
            
            log.debug("开始使用 AI 提取关键词，输入长度: {}", enhancedPrompt.length());
            
            // 使用 Spring AI 进行关键词提取
            String keyPoint = keyPointExtractionChatClient.prompt()
                    .user(enhancedPrompt)
                    .call()
                    .content();
            
            // 验证关键词提取结果
            if (StrUtil.isBlank(keyPoint)) {
                log.warn("AI 关键词提取结果为空，使用降级方案");
                return extractKeyPointFallback(enhancedPrompt);
            }
            
            // 清理和格式化关键词
            String cleanedKeyPoint = cleanKeyPoint(keyPoint);
            
            log.debug("AI 关键词提取成功: {}", cleanedKeyPoint);
            return cleanedKeyPoint;
            
        } catch (Exception e) {
            log.error("使用 AI 提取关键词时发生异常", e);
            // 降级处理：使用简单的关键词提取
            return extractKeyPointFallback(enhancedPrompt);
        }
    }
    
    /**
     * 降级方案：简单的关键词提取
     */
    private static String extractKeyPointFallback(String enhancedPrompt) {
        try {
            if (StrUtil.isBlank(enhancedPrompt)) {
                return "文创产品";
            }
            
            // 简单的关键词提取逻辑
            String text = enhancedPrompt.toLowerCase();
            
            // 预定义的关键词映射
            if (text.contains("茶具") || text.contains("茶")) {
                if (text.contains("成都") || text.contains("火锅")) {
                    return "茶具 成都 火锅元素";
                }
                return "茶具 文创";
            }
            
            if (text.contains("西安") || text.contains("古建筑")) {
                return "西安古建筑 文创 古都文化";
            }
            
            if (text.contains("中秋") || text.contains("月亮") || text.contains("兔子")) {
                return "中秋 月亮 兔子摆件";
            }
            
            if (text.contains("摆件") || text.contains("装饰")) {
                return "文创摆件 装饰品";
            }
            
            // 默认关键词
            return "文创产品 设计";
            
        } catch (Exception e) {
            log.error("降级关键词提取失败", e);
            return "文创产品";
        }
    }
    
    /**
     * 清理和格式化关键词
     */
    private static String cleanKeyPoint(String keyPoint) {
        if (StrUtil.isBlank(keyPoint)) {
            return "文创产品";
        }
        
        // 移除多余的标点符号和换行符
        String cleaned = keyPoint.trim()
                .replaceAll("[\\n\\r\\t]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        
        // 限制长度，避免关键词过长
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 50).trim();
        }
        
        // 如果清理后为空，返回默认值
        if (StrUtil.isBlank(cleaned)) {
            return "文创产品";
        }
        
        return cleaned;
    }
}
