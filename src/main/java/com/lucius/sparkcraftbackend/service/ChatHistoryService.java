package com.lucius.sparkcraftbackend.service;

import com.lucius.sparkcraftbackend.dto.ChatHistoryQueryRequest;
import com.lucius.sparkcraftbackend.entity.ChatHistory;
import com.lucius.sparkcraftbackend.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {
    /**
     *  添加对话消息
     * @param appId 应用ID
     * @param message 发送的信息
     * @param messageType 信息的类型
     * @param userId 用户的ID
     * @return 是否添加成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);
    /**
     * 通过应用ID删除对话消息
     * @param appId 应用ID
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);
    /**
     * 获取查询条件
     * @param chatHistoryQueryRequest 查询条件
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 加载应用下的对话消息到内存中
     * @param appId 应用ID
     * @param chatMemory 对话内存
     * @param maxCount 最大数量
     * @return 加载的数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
