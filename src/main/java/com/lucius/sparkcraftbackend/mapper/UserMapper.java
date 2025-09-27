package com.lucius.sparkcraftbackend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.lucius.sparkcraftbackend.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 映射层。
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
