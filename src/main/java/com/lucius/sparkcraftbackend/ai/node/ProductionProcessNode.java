package com.lucius.sparkcraftbackend.ai.node;

import com.lucius.sparkcraftbackend.ai.WorkflowContext;
import com.lucius.sparkcraftbackend.entity.ImageResource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ProductionProcessNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 生成制造工艺");
            
            // TODO: 实际执行
            String productionProcess = "文创生成工艺";
            // 更新状态
            context.setCurrentStep("文创生成工艺");

            context.setProductionProcess(productionProcess);
            log.info("生成图片完成");
            return WorkflowContext.saveContext(context);
        });
    }
}
