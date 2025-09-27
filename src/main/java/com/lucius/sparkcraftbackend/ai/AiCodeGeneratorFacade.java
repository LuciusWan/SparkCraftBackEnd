package com.lucius.sparkcraftbackend.ai;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 生成流式回答
     * 
     * @param message 用户消息
     * @param imageProjectId 图片项目ID
     * @return 流式响应
     */
    public Flux<String> generateStreamAnswer(String message, Long imageProjectId) {
        log.info("开始生成流式回答，项目ID: {}, 消息: {}", imageProjectId, message);
        
        try {
            // 获取 AI 代码生成服务实例
            AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(imageProjectId);
            
            // 调用 AI 服务生成流式回答
            return aiCodeGeneratorService.getIdea(imageProjectId, message)
                    .doOnNext(chunk -> log.debug("AI 响应片段: {}", chunk))
                    .doOnComplete(() -> log.info("AI 流式响应完成，项目ID: {}", imageProjectId))
                    .doOnError(error -> log.error("AI 流式响应出错，项目ID: {}, 错误: {}", imageProjectId, error.getMessage(), error));
                    
        } catch (Exception e) {
            log.error("获取 AI 服务失败，项目ID: {}, 错误: {}", imageProjectId, e.getMessage(), e);
            // 返回错误信息的流
            return Flux.just("抱歉，AI 服务暂时不可用，请稍后重试。");
        }
    }
}