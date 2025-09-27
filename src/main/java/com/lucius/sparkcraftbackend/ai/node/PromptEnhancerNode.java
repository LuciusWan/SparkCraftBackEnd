package com.lucius.sparkcraftbackend.ai.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import com.lucius.sparkcraftbackend.entity.ChatHistory;
import com.lucius.sparkcraftbackend.service.ChatHistoryService;
import com.lucius.sparkcraftbackend.ai.AiCodeGeneratorServiceFactory;
import com.lucius.sparkcraftbackend.ai.ConversationSummaryService;
import com.lucius.sparkcraftbackend.ai.ConversationSummaryServiceFactory;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;



import java.util.List;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Data
public class PromptEnhancerNode {
    
    private static ChatHistoryService chatHistoryService;
    private static AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    private static ConversationSummaryServiceFactory conversationSummaryServiceFactory;

    // 静态方法设置依赖注入的服务
    public static void setChatHistoryService(ChatHistoryService service) {
        chatHistoryService = service;
    }

    public static void setAiCodeGeneratorServiceFactory(AiCodeGeneratorServiceFactory factory) {
        aiCodeGeneratorServiceFactory = factory;
    }

    public static void setConversationSummaryServiceFactory(ConversationSummaryServiceFactory factory) {
        conversationSummaryServiceFactory = factory;
    }
    
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 提示词增强");
            
            try {
                // 1. 从 context 中获取 appId（这里假设 appId 存储在某个字段中）
                // 注意：需要确保 WorkflowContext 中有 appId 字段，或者通过其他方式获取
                Long appId = getAppIdFromContext(context);
                if (appId == null) {
                    log.warn("无法从 context 中获取 appId，使用默认值");
                    appId = 1L; // 默认值，实际应用中需要正确传递
                }
                
                // 2. 根据 appId 查询相关聊天记录
                List<ChatHistory> chatHistories = getChatHistoriesByAppId(appId);
                log.info("查询到 {} 条聊天记录", chatHistories.size());
                
                // 3. 构建聊天记录摘要
                String chatSummary = buildChatSummary(chatHistories);
                
                // 4. 使用 AIService 构建增强后的提示词（Summary.txt 模板通过 @SystemMessage 自动加载）
                String enhancedPrompt = buildEnhancedPrompt(chatSummary, context.getOriginalPrompt());
                log.info("使用 AIService 构建增强后的提示词{}", enhancedPrompt);
                // 7. 更新状态
                context.setCurrentStep("提示词增强");
                context.setEnhancedPrompt(enhancedPrompt);
                
                log.info("提示词增强完成，增强后长度: {}", enhancedPrompt.length());
                return WorkflowContext.saveContext(context);
                
            } catch (Exception e) {
                log.error("提示词增强过程中发生错误", e);
                // 发生错误时使用原始提示词
                context.setCurrentStep("提示词增强");
                context.setEnhancedPrompt(context.getOriginalPrompt() != null ? context.getOriginalPrompt() : "默认提示词");
                return WorkflowContext.saveContext(context);
            }
        });
    }
    
    /**
     * 从 WorkflowContext 中获取 appId
     */
    private static Long getAppIdFromContext(WorkflowContext context) {
        if (context == null) {
            log.warn("WorkflowContext 为空");
            return null;
        }
        return context.getAppId();
    }
    
    /**
     * 根据 appId 查询聊天记录
     */
    private static List<ChatHistory> getChatHistoriesByAppId(Long appId) {
        if (chatHistoryService == null) {
            log.warn("ChatHistoryService 未注入，返回空列表");
            return List.of();
        }
        
        try {
            // 构建查询条件：根据 appId 查询，按创建时间降序排列，限制数量
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(20); // 限制最近20条记录
            
            return chatHistoryService.list(queryWrapper);
        } catch (Exception e) {
            log.error("查询聊天记录失败，appId: {}", appId, e);
            return List.of();
        }
    }
    

    
    /**
     * 构建聊天记录摘要
     */
    private static String buildChatSummary(List<ChatHistory> chatHistories) {
        if (CollUtil.isEmpty(chatHistories)) {
            return "暂无历史对话记录。";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("=== 历史对话记录 ===\n");
        
        // 按时间正序排列（最早的在前）
        List<ChatHistory> sortedHistories = chatHistories.stream()
                .sorted((h1, h2) -> h1.getCreateTime().compareTo(h2.getCreateTime()))
                .collect(Collectors.toList());
        
        for (ChatHistory history : sortedHistories) {
            String messageType = "user".equals(history.getMessageType()) ? "用户" : "AI";
            summary.append(String.format("[%s]: %s\n", messageType, history.getMessage()));
        }
        
        summary.append("=== 对话记录结束 ===\n");
        return summary.toString();
    }
    
    /**
     * 使用 AIService 构建增强后的提示词
     */
    private static String buildEnhancedPrompt(String chatSummary, String originalPrompt) {
        try {
            // 1. 创建对话总结服务实例
            ConversationSummaryService summaryService = conversationSummaryServiceFactory.createConversationSummaryService();
            
            // 2. 使用 AIService 总结对话记忆（Summary.txt 模板已通过 @SystemMessage 注入）
            String conversationSummary = summaryService.summarizeConversation(chatSummary);
            
            // 3. 构建最终的增强提示词
            StringBuilder enhancedPrompt = new StringBuilder();
            enhancedPrompt.append("=== 对话记忆总结 ===\n");
            enhancedPrompt.append(conversationSummary).append("\n\n");
            
            // 4. 如果有原始提示词，也加入
            if (StrUtil.isNotBlank(originalPrompt)) {
                enhancedPrompt.append("=== 用户当前输入 ===\n");
                enhancedPrompt.append(originalPrompt);
            }
            
            return enhancedPrompt.toString();
            
        } catch (Exception e) {
            log.error("使用 AIService 构建增强提示词失败", e);
            // 降级处理：返回原始提示词
            return StrUtil.isNotBlank(originalPrompt) ? originalPrompt : "提示词增强失败，请重试";
        }
    }
    
    /**
     * 使用 AI 进一步增强提示词（可选）
     */
    private static String enhancePromptWithAI(String prompt, Long appId) {
        try {
            // 这里可以调用 AI 服务来进一步优化提示词
            // 暂时直接返回原提示词
            log.debug("AI 增强提示词功能暂未实现，直接返回原提示词");
            return prompt;
        } catch (Exception e) {
            log.error("AI 增强提示词失败", e);
            return prompt;
        }
    }
}
