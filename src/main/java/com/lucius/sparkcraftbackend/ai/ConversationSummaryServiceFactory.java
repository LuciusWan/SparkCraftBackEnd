package com.lucius.sparkcraftbackend.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 对话记忆总结服务工厂
 * 简化版本，不依赖 LangChain4j
 */
@Component
@Slf4j
public class ConversationSummaryServiceFactory {

    /**
     * 创建对话总结服务实例
     * 
     * @return ConversationSummaryService 实例
     */
    public ConversationSummaryService createConversationSummaryService() {
        log.debug("创建对话总结服务实例（简化版本）");
        
        return new ConversationSummaryService() {
            @Override
            public String summarizeConversation(String chatHistory) {
                // 简化的总结逻辑，不使用 AI
                if (chatHistory == null || chatHistory.trim().isEmpty()) {
                    return "暂无对话记录";
                }
                
                // 简单的文本处理总结
                String[] lines = chatHistory.split("\n");
                StringBuilder summary = new StringBuilder();
                summary.append("对话总结：\n");
                
                int userCount = 0;
                int aiCount = 0;
                
                for (String line : lines) {
                    if (line.contains("[用户]")) {
                        userCount++;
                    } else if (line.contains("[AI]")) {
                        aiCount++;
                    }
                }
                
                summary.append("- 用户发言 ").append(userCount).append(" 次\n");
                summary.append("- AI 回复 ").append(aiCount).append(" 次\n");
                summary.append("- 主要讨论内容：文创产品设计相关话题");
                
                return summary.toString();
            }
        };
    }
}