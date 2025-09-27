package com.lucius.sparkcraftbackend.mapper;


import com.lucius.sparkcraftbackend.entity.ChatMemoryEntity;
import org.apache.ibatis.annotations.*;
import org.springframework.data.redis.connection.Message;

import java.util.List;

@Mapper
public interface ChatMemoryMapper {

    @Insert("INSERT INTO chat_memory (conversation_id, message_type, content, metadata) " +
            "VALUES (#{conversationId}, #{messageType}, #{content}, #{metadata})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMessage(ChatMemoryEntity entity);

    @Select("SELECT * FROM (" +
            // 子查询：获取最近20条（按created_at降序）
            "SELECT * FROM chat_memory " +
            "WHERE conversation_id = #{conversationId} " +
            "ORDER BY created_at DESC LIMIT #{limit}" +
            ") AS temp " +
            // 外层查询：对临时结果按created_at升序（反转顺序）
            "ORDER BY created_at ASC")
    List<ChatMemoryEntity> getMessages(@Param("conversationId") String conversationId,
                                       @Param("limit") int limit);
    
    @Delete("DELETE FROM chat_memory WHERE conversation_id = #{conversationId} " +
            "AND id NOT IN (SELECT id FROM (SELECT id FROM chat_memory " +
            "WHERE conversation_id = #{conversationId} ORDER BY created_at DESC LIMIT #{keepCount}) t)")
    void cleanOldMessages(@Param("conversationId") String conversationId, 
                         @Param("keepCount") int keepCount);
    @Update("UPDATE chat_memory SET image_url = #{finalFilePath} WHERE id = #{chatMemoryId}")
    Boolean setImageUrl(Long chatMemoryId, String finalFilePath);
    @Select("SELECT message_type as role, content, created_at FROM chat_memory WHERE conversation_id = #{chatId} ORDER BY created_at ASC limit #{i}")
    List<Message> getMessagesWithFile(String chatId, int i);
}