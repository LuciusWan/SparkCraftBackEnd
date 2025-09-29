package com.lucius.sparkcraftbackend.service;

import com.lucius.sparkcraftbackend.dto.WorkflowProgressEvent;
import com.lucius.sparkcraftbackend.service.impl.WorkflowProgressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowProgressService测试类
 */
@ExtendWith(MockitoExtension.class)
class WorkflowProgressServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WorkflowProgressServiceImpl workflowProgressService;

    private static final String TEST_IMAGE_PROJECT_ID = "123";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_JOB_ID = "job-456";

    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }

    @Test
    void testCreateConnection_Success() {
        // 测试SSE连接创建
        SseEmitter emitter = workflowProgressService.createConnection(TEST_IMAGE_PROJECT_ID, TEST_USER_ID);
        
        assertNotNull(emitter);
        assertTrue(workflowProgressService.hasActiveConnection(TEST_IMAGE_PROJECT_ID));
        assertEquals(1, workflowProgressService.getActiveConnectionCount());
    }

    @Test
    void testCreateConnection_ReplaceExisting() {
        // 测试替换现有连接
        SseEmitter emitter1 = workflowProgressService.createConnection(TEST_IMAGE_PROJECT_ID, TEST_USER_ID);
        SseEmitter emitter2 = workflowProgressService.createConnection(TEST_IMAGE_PROJECT_ID, TEST_USER_ID);
        
        assertNotNull(emitter1);
        assertNotNull(emitter2);
        assertNotSame(emitter1, emitter2);
        assertEquals(1, workflowProgressService.getActiveConnectionCount());
    }

    @Test
    void testSendProgressEvent_ConnectionExists() throws Exception {
        // 创建连接
        workflowProgressService.createConnection(TEST_IMAGE_PROJECT_ID, TEST_USER_ID);
        
        // 模拟事件数据序列化
        WorkflowProgressEvent event = WorkflowProgressEvent.workflowStarted(TEST_JOB_ID, Long.valueOf(TEST_IMAGE_PROJECT_ID), "测试提示词");
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"eventType\":\"WORKFLOW_STARTED\"}");
        
        // 发送事件
        assertDoesNotThrow(() -> workflowProgressService.sendProgressEvent(event));
    }

    @Test
    void testSendProgressEvent_NoConnection() {
        // 没有连接的情况下发送事件
        WorkflowProgressEvent event = WorkflowProgressEvent.workflowStarted(TEST_JOB_ID, Long.valueOf(TEST_IMAGE_PROJECT_ID), "测试提示词");
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> workflowProgressService.sendProgressEvent(event));
    }

    @Test
    void testCloseConnection() {
        // 创建连接
        workflowProgressService.createConnection(TEST_IMAGE_PROJECT_ID, TEST_USER_ID);
        assertTrue(workflowProgressService.hasActiveConnection(TEST_IMAGE_PROJECT_ID));
        
        // 关闭连接
        workflowProgressService.closeConnection(TEST_IMAGE_PROJECT_ID);
        assertFalse(workflowProgressService.hasActiveConnection(TEST_IMAGE_PROJECT_ID));
        assertEquals(0, workflowProgressService.getActiveConnectionCount());
    }

    @Test
    void testCleanupExpiredConnections() {
        // 创建连接
        workflowProgressService.createConnection(TEST_IMAGE_PROJECT_ID, TEST_USER_ID);
        assertEquals(1, workflowProgressService.getActiveConnectionCount());
        
        // 执行清理
        workflowProgressService.cleanupExpiredConnections();
        
        // 由于连接是新创建的，应该不会被清理
        assertEquals(1, workflowProgressService.getActiveConnectionCount());
    }

    @Test
    void testGetConnectionStats() {
        // 创建连接
        workflowProgressService.createConnection(TEST_IMAGE_PROJECT_ID, TEST_USER_ID);
        
        // 获取统计信息
        var stats = workflowProgressService.getConnectionStats();
        
        assertNotNull(stats);
        assertEquals(1, stats.get("totalConnections"));
        assertEquals(0, stats.get("broadcastGroups"));
        assertEquals(0, stats.get("mappings"));
    }

    @Test
    void testBroadcastMode() {
        // 创建连接
        SseEmitter emitter = workflowProgressService.createConnection(TEST_IMAGE_PROJECT_ID, TEST_USER_ID);
        
        // 添加到广播模式
        workflowProgressService.addConnectionToBroadcast(TEST_JOB_ID, emitter);
        
        // 检查统计信息
        var stats = workflowProgressService.getConnectionStats();
        assertEquals(1, stats.get("broadcastGroups"));
        assertEquals(1, stats.get("totalBroadcastConnections"));
        
        // 从广播模式移除
        workflowProgressService.removeConnectionFromBroadcast(TEST_JOB_ID, emitter);
        
        stats = workflowProgressService.getConnectionStats();
        assertEquals(0, stats.get("broadcastGroups"));
        assertEquals(0, stats.get("totalBroadcastConnections"));
    }

    @Test
    void testValidateAndCleanupConnections() {
        // 创建连接
        workflowProgressService.createConnection(TEST_IMAGE_PROJECT_ID, TEST_USER_ID);
        assertEquals(1, workflowProgressService.getActiveConnectionCount());
        
        // 验证和清理连接
        assertDoesNotThrow(() -> workflowProgressService.validateAndCleanupConnections());
    }

    @Test
    void testEventTypes() {
        // 测试不同类型的事件创建
        WorkflowProgressEvent startEvent = WorkflowProgressEvent.workflowStarted(TEST_JOB_ID, Long.valueOf(TEST_IMAGE_PROJECT_ID), "测试");
        assertEquals(WorkflowProgressEvent.EventType.WORKFLOW_STARTED, startEvent.getEventType());

        WorkflowProgressEvent nodeStartEvent = WorkflowProgressEvent.nodeStarted(TEST_JOB_ID, Long.valueOf(TEST_IMAGE_PROJECT_ID), "test_node", "测试节点", 1, 5);
        assertEquals(WorkflowProgressEvent.EventType.NODE_STARTED, nodeStartEvent.getEventType());

        WorkflowProgressEvent nodeCompleteEvent = WorkflowProgressEvent.nodeCompleted(TEST_JOB_ID, Long.valueOf(TEST_IMAGE_PROJECT_ID), "test_node", "测试节点", "结果", 1, 5);
        assertEquals(WorkflowProgressEvent.EventType.NODE_COMPLETED, nodeCompleteEvent.getEventType());

        WorkflowProgressEvent nodeFailEvent = WorkflowProgressEvent.nodeFailed(TEST_JOB_ID, Long.valueOf(TEST_IMAGE_PROJECT_ID), "test_node", "测试节点", "错误", 1, 5);
        assertEquals(WorkflowProgressEvent.EventType.NODE_FAILED, nodeFailEvent.getEventType());

        WorkflowProgressEvent completeEvent = WorkflowProgressEvent.workflowCompleted(TEST_JOB_ID, Long.valueOf(TEST_IMAGE_PROJECT_ID), "结果");
        assertEquals(WorkflowProgressEvent.EventType.WORKFLOW_COMPLETED, completeEvent.getEventType());

        WorkflowProgressEvent failEvent = WorkflowProgressEvent.workflowFailed(TEST_JOB_ID, Long.valueOf(TEST_IMAGE_PROJECT_ID), "错误");
        assertEquals(WorkflowProgressEvent.EventType.WORKFLOW_FAILED, failEvent.getEventType());
    }
}