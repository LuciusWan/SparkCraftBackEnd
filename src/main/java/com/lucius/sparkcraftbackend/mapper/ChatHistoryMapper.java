package com.lucius.sparkcraftbackend.mapper;


import com.lucius.sparkcraftbackend.entity.ChatHistory;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话历史 映射层。
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

}
