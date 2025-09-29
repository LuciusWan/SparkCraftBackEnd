package com.lucius.sparkcraftbackend.controller;

import com.lucius.sparkcraftbackend.dto.WorkflowExecuteRequest;
import com.lucius.sparkcraftbackend.entity.ImageProject;
import com.lucius.sparkcraftbackend.entity.User;
import com.lucius.sparkcraftbackend.service.ImageProjectService;
import com.lucius.sparkcraftbackend.service.UserService;
import com.lucius.sparkcraftbackend.service.WorkflowExecutionService;
import com.lucius.sparkcraftbackend.service.WorkflowProgressService;
import com.lucius.sparkcraftbackend.vo.WorkflowExecuteVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ImageProjectController SSE功能测试类
 */
@ExtendWith(MockitoExtension.class)
class ImageProjectControllerSseTest {

    @Mock
    private ImageProjectService imageProjectService;

    @Mock
    private UserService userService;

    @Mock
    private WorkflowExecutionService workflowExecutionService;

    @Mock
    private WorkflowProgressService workflowProgressService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private ImageProjectController imageProjectController;

    private User testUser;
    private ImageProject testProject;
    private WorkflowExecuteRequest testRequest;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");

        // 创建测试项目
        testProject = new ImageProject();
        testProject.setId(123L);
        testProject.setUserId(1L);
        testProject.setProjectName("测试项目");

        // 创建测试请求
        testRequest = new WorkflowExecuteRequest();
        testRequest.setImageProjectId(123L);
        testRequest.setOriginalPrompt("测试提示词");
    }

    @Test
    void testEstablishWorkflowSSE_Success() {
        // Mock 服务调用
        when(userService.getLoginUser(httpRequest)).thenReturn(testUser);
        when(imageProjectService.getById(123L)).thenReturn(testProject);
        when(workflowProgressService.createConnection("123", 1L)).thenReturn(new SseEmitter());

        // 执行测试
        SseEmitter result = imageProjectController.establishWorkflowSSE("123", httpRequest);

        // 验证结果
        assertNotNull(result);
        verify(userService).getLoginUser(httpRequest);
        verify(imageProjectService).getById(123L);
        verify(workflowProgressService).createConnection("123", 1L);
    }

    @Test
    void testEstablishWorkflowSSE_ProjectNotFound() {
        // Mock 服务调用
        when(userService.getLoginUser(httpRequest)).thenReturn(testUser);
        when(imageProjectService.getById(123L)).thenReturn(null);

        // 执行测试并验证异常
        assertThrows(Exception.class, () -> {
            imageProjectController.establishWorkflowSSE("123", httpRequest);
        });

        verify(userService).getLoginUser(httpRequest);
        verify(imageProjectService).getById(123L);
        verify(workflowProgressService, never()).createConnection(anyString(), anyLong());
    }

    @Test
    void testEstablishWorkflowSSE_NoPermission() {
        // 创建其他用户的项目
        testProject.setUserId(999L);

        // Mock 服务调用
        when(userService.getLoginUser(httpRequest)).thenReturn(testUser);
        when(imageProjectService.getById(123L)).thenReturn(testProject);

        // 执行测试并验证异常
        assertThrows(Exception.class, () -> {
            imageProjectController.establishWorkflowSSE("123", httpRequest);
        });

        verify(userService).getLoginUser(httpRequest);
        verify(imageProjectService).getById(123L);
        verify(workflowProgressService, never()).createConnection(anyString(), anyLong());
    }

    @Test
    void testExecuteWorkflowAsync_Success() {
        // 创建预期的返回结果
        WorkflowExecuteVO expectedResult = new WorkflowExecuteVO();
        expectedResult.setExecutionId("exec-123");
        expectedResult.setJobId("job-456");
        expectedResult.setImageProjectId(123L);
        expectedResult.setStatus("PENDING");
        expectedResult.setOriginalPrompt("测试提示词");
        expectedResult.setStartTime(LocalDateTime.now());

        // Mock 服务调用
        when(userService.getLoginUser(httpRequest)).thenReturn(testUser);
        when(imageProjectService.getById(123L)).thenReturn(testProject);
        when(workflowExecutionService.executeWorkflowAsync(eq(123L), eq("测试提示词"), eq(testUser)))
                .thenReturn(expectedResult);

        // 执行测试
        var response = imageProjectController.executeWorkflowAsync(testRequest, httpRequest);

        // 验证结果
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("exec-123", response.getData().getExecutionId());
        assertEquals("job-456", response.getData().getJobId());
        assertEquals("PENDING", response.getData().getStatus());

        verify(userService).getLoginUser(httpRequest);
        verify(imageProjectService).getById(123L);
        verify(workflowExecutionService).executeWorkflowAsync(eq(123L), eq("测试提示词"), eq(testUser));
    }

    @Test
    void testExecuteWorkflowWithSSE_Success() {
        // 创建预期的返回结果
        WorkflowExecuteVO expectedResult = new WorkflowExecuteVO();
        expectedResult.setExecutionId("exec-123");
        expectedResult.setJobId("job-456");
        expectedResult.setImageProjectId(123L);
        expectedResult.setStatus("PENDING");
        expectedResult.setOriginalPrompt("测试提示词");
        expectedResult.setStartTime(LocalDateTime.now());

        // Mock 服务调用
        when(userService.getLoginUser(httpRequest)).thenReturn(testUser);
        when(imageProjectService.getById(123L)).thenReturn(testProject);
        when(workflowExecutionService.executeWorkflowAsyncWithSSE(eq(123L), eq("测试提示词"), eq(testUser)))
                .thenReturn(expectedResult);

        // 执行测试
        var response = imageProjectController.executeWorkflowWithSSE(testRequest, httpRequest);

        // 验证结果
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("exec-123", response.getData().getExecutionId());
        assertEquals("job-456", response.getData().getJobId());
        assertEquals("PENDING", response.getData().getStatus());

        verify(userService).getLoginUser(httpRequest);
        verify(imageProjectService).getById(123L);
        verify(workflowExecutionService).executeWorkflowAsyncWithSSE(eq(123L), eq("测试提示词"), eq(testUser));
    }

    @Test
    void testExecuteWorkflowWithSSE_InvalidParams() {
        // 测试无效参数
        testRequest.setImageProjectId(null);

        // 执行测试并验证异常
        assertThrows(Exception.class, () -> {
            imageProjectController.executeWorkflowWithSSE(testRequest, httpRequest);
        });

        verify(userService, never()).getLoginUser(any());
        verify(imageProjectService, never()).getById(anyLong());
        verify(workflowExecutionService, never()).executeWorkflowAsyncWithSSE(anyLong(), anyString(), any());
    }

    @Test
    void testExecuteWorkflowWithSSE_EmptyPrompt() {
        // 测试空提示词
        testRequest.setOriginalPrompt("");

        // 执行测试并验证异常
        assertThrows(Exception.class, () -> {
            imageProjectController.executeWorkflowWithSSE(testRequest, httpRequest);
        });

        verify(userService, never()).getLoginUser(any());
        verify(imageProjectService, never()).getById(anyLong());
        verify(workflowExecutionService, never()).executeWorkflowAsyncWithSSE(anyLong(), anyString(), any());
    }

    @Test
    void testGetWorkflowProgress_DeprecatedMethod() {
        // Mock 服务调用
        when(userService.getLoginUser(httpRequest)).thenReturn(testUser);
        when(imageProjectService.getById(123L)).thenReturn(testProject);
        when(workflowProgressService.createConnection("123", 1L)).thenReturn(new SseEmitter());

        // 执行测试
        SseEmitter result = imageProjectController.getWorkflowProgress("123", httpRequest);

        // 验证结果（应该调用新的SSE接口）
        assertNotNull(result);
        verify(userService).getLoginUser(httpRequest);
        verify(imageProjectService).getById(123L);
        verify(workflowProgressService).createConnection("123", 1L);
    }
}