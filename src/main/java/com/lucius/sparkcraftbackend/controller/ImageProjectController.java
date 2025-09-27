package com.lucius.sparkcraftbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.lucius.sparkcraftbackend.annotation.AuthCheck;
import com.lucius.sparkcraftbackend.common.BaseResponse;
import com.lucius.sparkcraftbackend.common.DeleteRequest;
import com.lucius.sparkcraftbackend.common.ResultUtils;
import com.lucius.sparkcraftbackend.constant.ImageProjectConstant;
import com.lucius.sparkcraftbackend.constant.UserConstant;
import com.lucius.sparkcraftbackend.dto.ImageProjectAddRequest;
import com.lucius.sparkcraftbackend.dto.ImageProjectQueryRequest;
import com.lucius.sparkcraftbackend.dto.ImageProjectUpdateRequest;
import com.lucius.sparkcraftbackend.dto.WorkflowExecuteRequest;
import com.lucius.sparkcraftbackend.entity.User;
import com.lucius.sparkcraftbackend.exception.BusinessException;
import com.lucius.sparkcraftbackend.exception.ErrorCode;
import com.lucius.sparkcraftbackend.exception.ThrowUtils;
import com.lucius.sparkcraftbackend.service.UserService;
import com.lucius.sparkcraftbackend.service.WorkflowExecutionService;
import com.lucius.sparkcraftbackend.vo.ImageProjectVO;
import com.lucius.sparkcraftbackend.vo.WorkflowExecuteVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import com.lucius.sparkcraftbackend.entity.ImageProject;
import com.lucius.sparkcraftbackend.service.ImageProjectService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.wsa.v20250508.WsaClient;
import com.tencentcloudapi.wsa.v20250508.models.*;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@RestController
@RequestMapping("/imageProject")
public class ImageProjectController {

    @Resource
    private ImageProjectService imageProjectService;

    @Resource
    private UserService userService;

    @Resource
    private WorkflowExecutionService workflowExecutionService;


    /**
     * 创建应用
     *
     * @param imageProjectAddRequest 创建应用请求
     * @param request       请求
     * @return 应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody ImageProjectAddRequest imageProjectAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(imageProjectAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 参数校验
        String initPrompt = imageProjectAddRequest.getProjectDesc();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 构造入库对象
        ImageProject imageProject = new ImageProject();
        BeanUtil.copyProperties(imageProjectAddRequest, imageProject);
        imageProject.setUserId(loginUser.getId());
        imageProject.setProjectName(imageProjectAddRequest.getProjectName());
        // 插入数据库
        boolean result = imageProjectService.save(imageProject);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(imageProject.getId());
    }

    /**
     * 更新应用（用户只能更新自己的应用名称）
     *
     * @param imageProjectUpdateRequest 更新请求
     * @param request          请求
     * @return 更新结果
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody ImageProjectUpdateRequest imageProjectUpdateRequest, HttpServletRequest request) {
        if (imageProjectUpdateRequest == null || imageProjectUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = imageProjectUpdateRequest.getId();
        // 判断是否存在
        ImageProject oldImageProject = imageProjectService.getById(id);
        ThrowUtils.throwIf(oldImageProject == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可更新
        if (!oldImageProject.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        ImageProject imageProject = new ImageProject();
        imageProject.setId(id);
        imageProject.setProjectName(imageProject.getProjectName());
        // 设置编辑时间
        imageProject.setUpdateTime(LocalDateTime.now());
        boolean result = imageProjectService.updateById(imageProject);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 删除应用（用户只能删除自己的应用）
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        ImageProject oldApp = imageProjectService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = imageProjectService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用详情
     *
     * @param id      应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<ImageProjectVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        ImageProject imageProject = imageProjectService.getById(id);
        ThrowUtils.throwIf(imageProject == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类（包含用户信息）
        return ResultUtils.success(imageProjectService.getImageVO(imageProject));
    }


    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param imageProjectQueryRequest 查询请求
     * @param request         请求
     * @return 应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<ImageProjectVO>> listMyAppVOByPage(@RequestBody ImageProjectQueryRequest imageProjectQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(imageProjectQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 限制每页最多 20 个
        long pageSize = imageProjectQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个工程");
        long pageNum = imageProjectQueryRequest.getPageNum();
        // 只查询当前用户的应用
        imageProjectQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = imageProjectService .getQueryWrapper(imageProjectQueryRequest);
        Page<ImageProject> appPage = imageProjectService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<ImageProjectVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<ImageProjectVO> appVOList = imageProjectService.getImageVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页获取精选应用列表
     *
     * @param imageProjectQueryRequest 查询请求
     * @return 精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<ImageProjectVO>> listGoodAppVOByPage(@RequestBody ImageProjectQueryRequest imageProjectQueryRequest) {
        ThrowUtils.throwIf(imageProjectQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = imageProjectQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = imageProjectQueryRequest.getPageNum();
        // 只查询精选的应用
        imageProjectQueryRequest.setPriority(ImageProjectConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = imageProjectService.getQueryWrapper(imageProjectQueryRequest);
        // 分页查询
        Page<ImageProject> appPage = imageProjectService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<ImageProjectVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<ImageProjectVO> appVOList = imageProjectService.getImageVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员删除应用
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        ImageProject oldApp = imageProjectService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = imageProjectService.removeById(id);
        return ResultUtils.success(result);
    }



    /**
     * 执行工作流（同步）
     *
     * @param workflowExecuteRequest 工作流执行请求
     * @param request               HTTP请求
     * @return 工作流执行结果
     */
    @PostMapping("/workflow/execute")
    public BaseResponse<WorkflowExecuteVO> executeWorkflow(@RequestBody WorkflowExecuteRequest workflowExecuteRequest, 
                                                          HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(workflowExecuteRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(workflowExecuteRequest.getImageProjectId() == null || workflowExecuteRequest.getImageProjectId() <= 0, 
                          ErrorCode.PARAMS_ERROR, "项目ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(workflowExecuteRequest.getOriginalPrompt()), 
                          ErrorCode.PARAMS_ERROR, "原始提示词不能为空");
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 验证项目是否存在且用户有权限
        ImageProject imageProject = imageProjectService.getById(workflowExecuteRequest.getImageProjectId());
        ThrowUtils.throwIf(imageProject == null, ErrorCode.NOT_FOUND_ERROR, "项目不存在");
        ThrowUtils.throwIf(!imageProject.getUserId().equals(loginUser.getId()), 
                          ErrorCode.NO_AUTH_ERROR, "无权限访问该项目");
        
        // 执行工作流
        WorkflowExecuteVO result = workflowExecutionService.executeWorkflow(
                workflowExecuteRequest.getImageProjectId(),
                workflowExecuteRequest.getOriginalPrompt(),
                loginUser
        );
        
        return ResultUtils.success(result);
    }

    /**
     * 执行工作流（流式）
     *
     * @param workflowExecuteRequest 工作流执行请求
     * @param request               HTTP请求
     * @return 工作流执行状态流
     */
    @PostMapping(value = "/workflow/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<WorkflowExecuteVO>> executeWorkflowStream(@RequestBody WorkflowExecuteRequest workflowExecuteRequest,
                                                                          HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(workflowExecuteRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(workflowExecuteRequest.getImageProjectId() == null || workflowExecuteRequest.getImageProjectId() <= 0,
                          ErrorCode.PARAMS_ERROR, "项目ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(workflowExecuteRequest.getOriginalPrompt()),
                          ErrorCode.PARAMS_ERROR, "原始提示词不能为空");

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 验证项目是否存在且用户有权限
        ImageProject imageProject = imageProjectService.getById(workflowExecuteRequest.getImageProjectId());
        ThrowUtils.throwIf(imageProject == null, ErrorCode.NOT_FOUND_ERROR, "项目不存在");
        ThrowUtils.throwIf(!imageProject.getUserId().equals(loginUser.getId()),
                          ErrorCode.NO_AUTH_ERROR, "无权限访问该项目");

        // 执行流式工作流
        Flux<WorkflowExecuteVO> workflowStream = workflowExecutionService.executeWorkflowStream(
                workflowExecuteRequest.getImageProjectId(),
                workflowExecuteRequest.getOriginalPrompt(),
                loginUser
        );

        // 转换为 ServerSentEvent 格式
        return workflowStream
                .map(result -> ServerSentEvent.<WorkflowExecuteVO>builder()
                        .data(result)
                        .build())
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<WorkflowExecuteVO>builder()
                                .event("done")
                                .data(null)
                                .build()
                ));
    }

    /**
     * 获取工作流执行状态（通过执行ID）
     *
     * @param executionId 执行ID
     * @param request     HTTP请求
     * @return 工作流执行状态
     */
    @GetMapping("/workflow/status/{executionId}")
    public BaseResponse<WorkflowExecuteVO> getWorkflowStatus(@PathVariable String executionId, 
                                                            HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // TODO: 这里可以实现从缓存或数据库中查询执行状态的逻辑
        // 目前返回一个示例状态
        WorkflowExecuteVO result = new WorkflowExecuteVO();
        result.setExecutionId(executionId);
        result.setStatus("COMPLETED");
        
        return ResultUtils.success(result);
    }

    /**
     * 测试工作流执行（简化版，用于调试）
     *
     * @param imageProjectId 项目ID
     * @param prompt         提示词
     * @param request        HTTP请求
     * @return 工作流执行结果
     */
    @GetMapping("/workflow/test")
    public BaseResponse<WorkflowExecuteVO> testWorkflow(@RequestParam Long imageProjectId,
                                                       @RequestParam String prompt,
                                                       HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 验证项目是否存在
        ImageProject imageProject = imageProjectService.getById(imageProjectId);
        ThrowUtils.throwIf(imageProject == null, ErrorCode.NOT_FOUND_ERROR, "项目不存在");
        
        // 执行工作流
        WorkflowExecuteVO result = workflowExecutionService.executeWorkflow(imageProjectId, prompt, loginUser);
        
        return ResultUtils.success(result);
    }


    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ImageProjectVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        ImageProject app = imageProjectService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(imageProjectService.getImageVO(app));
    }


    /**
     * 保存。
     *
     * @param imageProject 图片 项目
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody ImageProject imageProject) {
        return imageProjectService.save(imageProject);
    }

    /**
     * 根据主键删除。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return imageProjectService.removeById(id);
    }

    /**
     * 根据主键更新。
     *
     * @param imageProject 图片 项目
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody ImageProject imageProject) {
        return imageProjectService.updateById(imageProject);
    }

    /**
     * 查询所有。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<ImageProject> list() {
        return imageProjectService.list();
    }

    /**
     * 根据主键获取。
     *
     * @param id 主键
     * @return 详情
     */
    @GetMapping("getInfo/{id}")
    public ImageProject getInfo(@PathVariable Long id) {
        return imageProjectService.getById(id);
    }

    /**
     * 分页查询。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<ImageProject> page(Page<ImageProject> page) {
        return imageProjectService.page(page);
    }


    @GetMapping(value = "/chat/get/idea", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatToGetIdea(@RequestParam Long imageProjectId,
                                    @RequestParam String message,
                                    HttpServletRequest request){
        SseEmitter emitter=new SseEmitter(60000000L);

        // 参数校验
        ThrowUtils.throwIf(imageProjectId == null || imageProjectId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        imageProjectService.chatToGetTheIdea(imageProjectId,message, loginUser,emitter);
        return emitter;
    }
}
