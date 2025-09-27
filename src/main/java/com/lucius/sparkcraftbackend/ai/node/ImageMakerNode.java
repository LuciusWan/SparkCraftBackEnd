package com.lucius.sparkcraftbackend.ai.node;

import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import com.lucius.sparkcraftbackend.entity.ImageResource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageMakerNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 绘制图片");
            
            // TODO: 实际执行提示词增强逻辑
            
            // 简单的假数据
            ImageResource aiImage = new ImageResource("AI图片", "https://www.codefather.cn/logo.png");
            
            // 更新状态
            context.setCurrentStep("生成图片");

            context.setAiImage(aiImage);
            log.info("生成图片完成");
            return WorkflowContext.saveContext(context);
        });
    }
}