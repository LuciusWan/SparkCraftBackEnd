package com.lucius.sparkcraftbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import com.lucius.sparkcraftbackend.ai.handler.StreamHandlerExecutor;
import com.lucius.sparkcraftbackend.dto.ImageProjectQueryRequest;
import com.lucius.sparkcraftbackend.entity.User;
import com.lucius.sparkcraftbackend.enums.ChatHistoryMessageTypeEnum;
import com.lucius.sparkcraftbackend.exception.BusinessException;
import com.lucius.sparkcraftbackend.exception.ErrorCode;
import com.lucius.sparkcraftbackend.service.ChatHistoryService;
import com.lucius.sparkcraftbackend.service.UserService;
import com.lucius.sparkcraftbackend.vo.ImageProjectVO;
import com.lucius.sparkcraftbackend.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.lucius.sparkcraftbackend.entity.ImageProject;
import com.lucius.sparkcraftbackend.mapper.ImageProjectMapper;
import com.lucius.sparkcraftbackend.service.ImageProjectService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Service
@Slf4j
public class ImageProjectServiceImpl extends ServiceImpl<ImageProjectMapper, ImageProject>  implements ImageProjectService{
    @Resource
    private UserService userService;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Autowired
    private ChatClient getIdeaChatClient;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    @Override
    public ImageProjectVO getImageVO(ImageProject imageProject) {
        if (imageProject == null) {
            return null;
        }
        ImageProjectVO imageProjectVO = new ImageProjectVO();
        BeanUtil.copyProperties(imageProject, imageProjectVO);
        // 关联查询用户信息
        Long userId = imageProject.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            imageProjectVO.setUser(userVO);
        }
        return imageProjectVO;
    }


    @Override
    public QueryWrapper getQueryWrapper(ImageProjectQueryRequest imageProjectQueryRequest) {
        if (imageProjectQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = imageProjectQueryRequest.getId();
        String projectName = imageProjectQueryRequest.getProjectName();
        String projectDesc = imageProjectQueryRequest.getProjectDesc();
        Long userId = imageProjectQueryRequest.getUserId();
        String sortOrder = imageProjectQueryRequest.getSortOrder();
        String sortField = imageProjectQueryRequest.getSortField();
        return QueryWrapper.create()
                .eq("id", id)
                .like("projectName", projectName)
                .like("projectDesc", projectDesc)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public List<ImageProjectVO> getImageVOList(List<ImageProject> imageProjectList) {
        if (CollUtil.isEmpty(imageProjectList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = imageProjectList.stream()
                .map(ImageProject::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return imageProjectList.stream().map(app -> {
            ImageProjectVO appVO = getImageVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }


    @Override
    public void chatToGetTheIdea(Long imageProjectId, String message, User loginUser, SseEmitter emitter) {
        StringBuilder stringBuilder=new StringBuilder();
        executorService.submit(() -> {
            getIdeaChatClient.prompt()
                        .user(message)
                        .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, imageProjectId))
                        .stream()
                        .content()
                        .subscribe(
                                chunk -> {
                                    try {
                                        stringBuilder.append(chunk);
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("d", chunk);
                                        emitter.send(jsonObject.toString());
                                    } catch (JSONException | IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                },
                                error -> {throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"回复的时候出错");},
                                () -> {
                                    try {
                                        emitter.send("end");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    log.info("回复完成{}", stringBuilder);
                                    emitter.complete();
                                }
                        );

        });
    }
}
