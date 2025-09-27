package com.lucius.sparkcraftbackend.ai.node;

import com.lucius.sparkcraftbackend.mapper.ChatMemoryMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * PromptEnhancerNode 配置类
 * 用于向静态方法注入 Spring 管理的依赖
 */
@Component
@Slf4j
public class PromptEnhancerNodeConfig {

    @Resource
    private ChatMemoryMapper chatMemoryMapper;

    @Resource
    @Qualifier("promptSummeryChatClient")
    private ChatClient promptSummeryChatClient;

    @Resource
    @Qualifier("keyPointExtractionChatClient")
    private ChatClient keyPointExtractionChatClient;

    /**
     * 在 Bean 初始化后注入依赖到静态方法
     */
    @PostConstruct
    public void init() {
        log.info("初始化 PromptEnhancerNode 依赖注入");
        PromptEnhancerNode.setChatMemoryMapper(chatMemoryMapper);
        PromptEnhancerNode.setPromptSummeryChatClient(promptSummeryChatClient);
        PromptEnhancerNode.setKeyPointExtractionChatClient(keyPointExtractionChatClient);
        log.info("PromptEnhancerNode 依赖注入完成");
    }
}