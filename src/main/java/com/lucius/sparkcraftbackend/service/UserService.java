package com.lucius.sparkcraftbackend.service;


import com.lucius.sparkcraftbackend.dto.UserQueryRequest;
import com.lucius.sparkcraftbackend.vo.LoginUserVO;
import com.lucius.sparkcraftbackend.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.lucius.sparkcraftbackend.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);
    LoginUserVO getLoginUserVO(User user);
    /**
     * 获取当前登录用户
     *
     * @param request 获取session
     * @return 返回结果为用户部分信息
     */
    User getLoginUser(HttpServletRequest request);
    /**
     * 用户注销
     *
     * @param request 获取session
     * @return 登出成功返回true
     */
    boolean userLogout(HttpServletRequest request);
    /**
     * 获取脱敏的用户
     *
     * @param user  用户
     * @return 脱敏的用户
     */
    UserVO getUserVO(User user);
    /**
     * 获取脱敏的用户列表
     *
     * @param userList 用户列表
     * @return 脱敏的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);
    /**
     * 获取查询条件
     *
     * @param userQueryRequest 用户查询条件
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
    String getEncryptPassword(String userPassword);
}
