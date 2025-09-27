package com.lucius.sparkcraftbackend.ai.node;

import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Data
public class PromptEnhancerNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: prompt增强");

            // TODO: 实际执行，先查询对应appid的，使用SpringAI的AIConfig，然后获取生成好的 prompt
            String productionProcess = "prompt增强";
            // 更新状态
            context.setCurrentStep("prompt增强完成");

            context.setProductionProcess(productionProcess);
            log.info("prompt增强完成");
            return WorkflowContext.saveContext(context);
        });
    }

}
