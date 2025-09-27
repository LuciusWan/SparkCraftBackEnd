package com.lucius.sparkcraftbackend.ai;

import com.lucius.sparkcraftbackend.entity.ImageResource;
import lombok.Data;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流上下文
 */
@Data
public class WorkflowContext {
    
    /**
     * 应用ID
     */
    private Long appId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 原始提示词
     */
    private String originalPrompt;
    
    /**
     * 增强后的提示词
     */
    private String enhancedPrompt;

    /**
     * 3D模型URL
     */
    private String threeDModelUrl;

    /**
     * 模型图片URL
     */
    private String modelImageUrl;

    private List<ImageResource> imageList;

    private ImageResource aiImage;

    private String productionProcess;
    /**
     * 当前执行步骤
     */
    private String currentStep;
    
    /**
     * 各节点执行结果
     */
    private Map<String, Object> nodeResults = new HashMap<>();
    
    /**
     * 额外的上下文数据
     */
    private Map<String, Object> extraData = new HashMap<>();
    
    /**
     * 从 MessagesState 中获取 WorkflowContext
     * 由于 MessagesState 没有 getContext 方法，我们使用 messages 的第一个元素来存储上下文信息
     */
    public static WorkflowContext getContext(MessagesState<String> state) {
        if (state == null) {
            return new WorkflowContext();
        }
        
        // 尝试从 messages 中获取上下文信息
        // 这里我们假设第一个消息包含上下文信息的标识
        return new WorkflowContext(); // 简化处理，返回新的上下文
    }
    
    /**
     * 将 WorkflowContext 保存到返回的 Map 中
     * 根据 LangGraph4j 的 API，节点应该返回 Map<String, Object>
     */
    public static Map<String, Object> saveContext(WorkflowContext context) {
        Map<String, Object> result = new HashMap<>();
        result.put("messages", context.getCurrentStep() != null ? context.getCurrentStep() : "");
        return result;
    }
    
    /**
     * 添加节点执行结果
     */
    public void addNodeResult(String nodeName, Object result) {
        this.nodeResults.put(nodeName, result);
    }
    
    /**
     * 获取节点执行结果
     */
    public Object getNodeResult(String nodeName) {
        return this.nodeResults.get(nodeName);
    }
    
    /**
     * 添加额外数据
     */
    public void addExtraData(String key, Object value) {
        this.extraData.put(key, value);
    }
    
    /**
     * 获取额外数据
     */
    public Object getExtraData(String key) {
        return this.extraData.get(key);
    }
}