package com.lucius.sparkcraftbackend.ai.config;


import com.lucius.sparkcraftbackend.entity.ChatMemoryEntity;
import com.lucius.sparkcraftbackend.mapper.ChatMemoryMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MysqlChatMemoryImpl implements ChatMemory {

    @Autowired
    private ChatMemoryMapper chatMemoryMapper;

    private static final int MAX_MESSAGES = 20;
    @Override
    public void add(String conversationId, List<Message> messages) {
        for (Message message : messages) {
            ChatMemoryEntity entity = new ChatMemoryEntity();
            entity.setConversationId(conversationId);
            entity.setMessageType(message.getClass().getSimpleName());
            entity.setContent(message.getText());
            entity.setMetadata("{}"); // 可以根据需要添加元数据
            chatMemoryMapper.insertMessage(entity);
        }

        // 清理旧消息，保持最大消息数
        chatMemoryMapper.cleanOldMessages(conversationId, MAX_MESSAGES);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<ChatMemoryEntity> entities = chatMemoryMapper.getMessages(conversationId, 10);

        return entities.stream().map(entity -> {
            if ("UserMessage".equals(entity.getMessageType())) {
                return new UserMessage(entity.getContent());
            } else {
                return new AssistantMessage(entity.getContent());
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        chatMemoryMapper.cleanOldMessages(conversationId, 0);
    }
}