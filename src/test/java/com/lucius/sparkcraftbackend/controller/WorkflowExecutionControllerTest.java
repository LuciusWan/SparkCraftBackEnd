package com.lucius.sparkcraftbackend.controller;

import com.alibaba.fastjson.JSON;
import com.lucius.sparkcraftbackend.dto.WorkflowExecuteRequest;
import com.lucius.sparkcraftbackend.entity.ImageProject;
import com.lucius.sparkcraftbackend.entity.User;
import com.lucius.sparkcraftbackend.service.ImageProjectService;
import com.lucius.sparkcraftbackend.service.UserService;
import com.lucius.sparkcraftbackend.service.WorkflowExecutionService;
import com.lucius.sparkcraftbackend.vo.WorkflowExecuteVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 工作流执行接口测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WorkflowExecutionControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private ImageProjectService imageProjectService;

    @MockBean
    private UserService userService;

    @MockBean
    private WorkflowExecutionService workflowExecutionService;

    private User testUser;
    private ImageProject testProject;
    private WorkflowExecuteRequest testRequest;
    private WorkflowExecuteVO testResponse;

    @BeforeEach
    void setUp() {
        // 准备测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("test_user");
        testUser.setUserRole("user");

        // 准备测试项目
        testProject = new ImageProject();
        testProject.setId(179826962019328L);
        testProject.setUserId(1L);
        testProject.setProjectName("测试项目");
        testProject.setProjectDesc("测试项目描述");

        // 准备测试请求
        testRequest = new WorkflowExecuteRequest();
        testRequest.setImageProjectId(179826962019328L);
        testRequest.setOriginalPrompt("设计一个以熊猫为主题的茶杯，风格现代简约，颜色以黑白为主");
        testRequest.setAsync(false);

        // 准备测试响应
        testResponse = new WorkflowExecuteVO();
        testResponse.setExecutionId("exec_test_001");
        testResponse.setJobId("job_test_001");
        testResponse.setImageProjectId(179826962019328L);
        testResponse.setStatus("COMPLETED");
        testResponse.setOriginalPrompt("设计一个以熊猫为主题的茶杯，风格现代简约，颜色以黑白为主");
        testResponse.setEnhancedPrompt("Modern minimalist panda-themed tea cup design with black and white color scheme");
        testResponse.setKeyPoint("熊猫,茶杯,现代简约,黑白,陶瓷");
        testResponse.setStartTime(LocalDateTime.now());
        testResponse.setEndTime(LocalDateTime.now());
        testResponse.setDuration(135000L);
    }

    @Test
    void testExecuteWorkflow_Success() throws Exception {
        // 模拟依赖服务的行为
        when(userService.getLoginUser(any())).thenReturn(testUser);
        when(imageProjectService.getById(179826962019328L)).thenReturn(testProject);
        when(workflowExecutionService.executeWorkflow(eq(179826962019328L), anyString(), eq(testUser)))
                .thenReturn(testResponse);

        // 执行请求并验证响应
        mockMvc.perform(post("/imageProject/workflow/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.executionId").value("exec_test_001"))
                .andExpect(jsonPath("$.data.jobId").value("job_test_001"))
                .andExpect(jsonPath("$.data.imageProjectId").value(179826962019328L))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.originalPrompt").value("设计一个以熊猫为主题的茶杯，风格现代简约，颜色以黑白为主"))
                .andExpect(jsonPath("$.data.enhancedPrompt").value("Modern minimalist panda-themed tea cup design with black and white color scheme"))
                .andExpect(jsonPath("$.data.keyPoint").value("熊猫,茶杯,现代简约,黑白,陶瓷"))
                .andExpect(jsonPath("$.data.duration").value(135000L));
    }

    @Test
    void testExecuteWorkflow_InvalidProjectId() {
        // 准备无效项目ID的请求
        WorkflowExecuteRequest invalidRequest = new WorkflowExecuteRequest();
        invalidRequest.setImageProjectId(null);
        invalidRequest.setOriginalPrompt("测试提示词");

        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WorkflowExecuteRequest> entity = new HttpEntity<>(invalidRequest, headers);

        // 执行请求
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/imageProject/workflow/execute",
                entity,
                String.class);

        // 验证响应
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("40000");
        assertThat(response.getBody()).contains("项目ID无效");
    }

    @Test
    void testExecuteWorkflow_EmptyPrompt() {
        // 准备空提示词的请求
        WorkflowExecuteRequest emptyPromptRequest = new WorkflowExecuteRequest();
        emptyPromptRequest.setImageProjectId(179826962019328L);
        emptyPromptRequest.setOriginalPrompt("");

        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WorkflowExecuteRequest> entity = new HttpEntity<>(emptyPromptRequest, headers);

        // 执行请求
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/imageProject/workflow/execute",
                entity,
                String.class);

        // 验证响应
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("40000");
        assertThat(response.getBody()).contains("原始提示词不能为空");
    }

    @Test
    void testExecuteWorkflow_ProjectNotFound() {
        // 模拟用户登录但项目不存在
        when(userService.getLoginUser(any())).thenReturn(testUser);
        when(imageProjectService.getById(179826962019328L)).thenReturn(null);

        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WorkflowExecuteRequest> entity = new HttpEntity<>(testRequest, headers);

        // 执行请求
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/imageProject/workflow/execute",
                entity,
                String.class);

        // 验证响应
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("40400");
        assertThat(response.getBody()).contains("项目不存在");
    }

    @Test
    void testExecuteWorkflow_NoPermission() {
        // 准备另一个用户
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUserName("another_user");

        // 模拟用户登录但没有项目权限
        when(userService.getLoginUser(any())).thenReturn(anotherUser);
        when(imageProjectService.getById(179826962019328L)).thenReturn(testProject);

        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WorkflowExecuteRequest> entity = new HttpEntity<>(testRequest, headers);

        // 执行请求
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/imageProject/workflow/execute",
                entity,
                String.class);

        // 验证响应
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("40301");
        assertThat(response.getBody()).contains("无权限访问该项目");
    }

    @Test
    void testExecuteWorkflow_WorkflowFailed() throws Exception {
        // 准备失败的工作流响应
        WorkflowExecuteVO failedResponse = new WorkflowExecuteVO();
        failedResponse.setExecutionId("exec_test_002");
        failedResponse.setJobId("job_test_002");
        failedResponse.setImageProjectId(179826962019328L);
        failedResponse.setStatus("FAILED");
        failedResponse.setOriginalPrompt("设计一个以熊猫为主题的茶杯");
        failedResponse.setStartTime(LocalDateTime.now());
        failedResponse.setEndTime(LocalDateTime.now());
        failedResponse.setDuration(30000L);
        failedResponse.setErrorMessage("AI服务连接失败，请稍后重试");

        // 模拟依赖服务的行为
        when(userService.getLoginUser(any())).thenReturn(testUser);
        when(imageProjectService.getById(179826962019328L)).thenReturn(testProject);
        when(workflowExecutionService.executeWorkflow(eq(179826962019328L), anyString(), eq(testUser)))
                .thenReturn(failedResponse);

        // 执行请求并验证响应
        mockMvc.perform(post("/imageProject/workflow/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorMessage").value("AI服务连接失败，请稍后重试"));
    }

    @Test
    void testExecuteWorkflow_AsyncMode() {
        // 准备异步请求
        WorkflowExecuteRequest asyncRequest = new WorkflowExecuteRequest();
        asyncRequest.setImageProjectId(179826962019328L);
        asyncRequest.setOriginalPrompt("设计一个以熊猫为主题的茶杯");
        asyncRequest.setAsync(true);

        // 准备异步响应（只有基本信息）
        WorkflowExecuteVO asyncResponse = new WorkflowExecuteVO();
        asyncResponse.setExecutionId("exec_async_001");
        asyncResponse.setJobId("job_async_001");
        asyncResponse.setImageProjectId(179826962019328L);
        asyncResponse.setStatus("PENDING");
        asyncResponse.setOriginalPrompt("设计一个以熊猫为主题的茶杯");
        asyncResponse.setStartTime(LocalDateTime.now());

        // 模拟依赖服务的行为
        when(userService.getLoginUser(any())).thenReturn(testUser);
        when(imageProjectService.getById(179826962019328L)).thenReturn(testProject);
        when(workflowExecutionService.executeWorkflow(eq(179826962019328L), anyString(), eq(testUser)))
                .thenReturn(asyncResponse);

        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WorkflowExecuteRequest> entity = new HttpEntity<>(asyncRequest, headers);

        // 执行请求
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/imageProject/workflow/execute",
                entity,
                String.class);

        // 验证响应
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("PENDING");
        assertThat(response.getBody()).contains("job_async_001");
    }
}