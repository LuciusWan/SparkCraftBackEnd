package com.lucius.sparkcraftbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.lucius.sparkcraftbackend.ai.AiCodeGeneratorFacade;
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
import org.intellij.lang.annotations.RegExp;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Service
public class ImageProjectServiceImpl extends ServiceImpl<ImageProjectMapper, ImageProject>  implements ImageProjectService{
    @Resource
    private UserService userService;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
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
    public Flux<String> chatToGetIdea(Long imageProjectId,String message, User loginUser) {
        if (imageProjectId == null||imageProjectId<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "imageProjectId为空");
        }
        if (StrUtil.isBlank(message)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提示词不能为空");
        }
        if (loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        ImageProject imageProject=this.mapper.selectOneById(imageProjectId);
        if (!Objects.equals(imageProject.getUserId(), loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"非本人的app");
        }

        // 5. 通过校验后，添加用户消息到对话历史
        chatHistoryService.addChatMessage(imageProjectId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 调用 AI 生成流式回复
        Flux<String> codeStream = aiCodeGeneratorFacade.generateStreamAnswer(message, imageProjectId);
        // 7. 收集 AI 响应内容并在完成后记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, imageProjectId, loginUser);

    }
}
