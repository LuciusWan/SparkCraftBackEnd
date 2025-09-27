package com.lucius.sparkcraftbackend.ai.node;

import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageThreeDModelNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 制作3d模型");
            
            // TODO: 实际执行
            String url = "http://test";
            // 更新状态
            context.setCurrentStep("制作3d模型完成");

            context.setThreeDModelUrl(url);
            log.info("生成图片完成");
            log.info("当前的workflowContext结果是：{}", context);
            System.out.println( context);
            return WorkflowContext.saveContext(context);
        });
    }
}
