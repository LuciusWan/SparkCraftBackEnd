package com.lucius.sparkcraftbackend.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 对话记忆总结服务
 * 使用 LangChain4j AIService 来总结对话记忆
 */
public interface ConversationSummaryService {

    /**
     * 根据聊天记录总结对话记忆
     * 
     * @param chatHistory 聊天记录内容
     * @return 总结后的对话记忆
     */
    @SystemMessage(fromResource = "prompt/Summary.txt")
    String summarizeConversation(@UserMessage String chatHistory);
}