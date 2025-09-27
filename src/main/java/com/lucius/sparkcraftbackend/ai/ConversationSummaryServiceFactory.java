package com.lucius.sparkcraftbackend.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 对话记忆总结服务工厂
 * 用于创建 ConversationSummaryService 实例
 */
@Component
@Slf4j
public class ConversationSummaryServiceFactory {

    @Resource
    private ChatModel chatModel;

    /**
     * 创建对话总结服务实例
     * 
     * @return ConversationSummaryService 实例
     */
    public ConversationSummaryService createConversationSummaryService() {
        log.debug("创建对话总结服务实例");
        
        return AiServices.builder(ConversationSummaryService.class)
                .chatModel(chatModel)
                .build();
    }
}