package com.lucius.sparkcraftbackend.ai;

/**
 * 对话记忆总结服务
 * 简化版本，不依赖 LangChain4j
 */
public interface ConversationSummaryService {

    /**
     * 根据聊天记录总结对话记忆
     * 
     * @param chatHistory 聊天记录内容
     * @return 总结后的对话记忆
     */
    String summarizeConversation(String chatHistory);
}