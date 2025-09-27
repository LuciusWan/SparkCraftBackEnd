package com.lucius.sparkcraftbackend.ai.node;

import com.lucius.sparkcraftbackend.ai.AiCodeGeneratorServiceFactory;
import com.lucius.sparkcraftbackend.ai.ConversationSummaryServiceFactory;
import com.lucius.sparkcraftbackend.service.ChatHistoryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PromptEnhancerNode 配置类
 * 用于向静态方法注入 Spring 管理的依赖
 */
@Component
@Slf4j
public class PromptEnhancerNodeConfig {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private ConversationSummaryServiceFactory conversationSummaryServiceFactory;

    /**
     * 在 Bean 初始化后注入依赖到静态方法
     */
    @PostConstruct
    public void init() {
        log.info("初始化 PromptEnhancerNode 依赖注入");
        PromptEnhancerNode.setChatHistoryService(chatHistoryService);
        PromptEnhancerNode.setAiCodeGeneratorServiceFactory(aiCodeGeneratorServiceFactory);
        PromptEnhancerNode.setConversationSummaryServiceFactory(conversationSummaryServiceFactory);
        log.info("PromptEnhancerNode 依赖注入完成");
    }
}