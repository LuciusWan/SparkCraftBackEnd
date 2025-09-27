package com.lucius.sparkcraftbackend.controller;

import com.lucius.sparkcraftbackend.ai.SimpleWorkflowService;
import com.lucius.sparkcraftbackend.common.BaseResponse;
import com.lucius.sparkcraftbackend.common.ResultUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工作流测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/workflow/test")
public class WorkflowTestController {

    @Resource
    private SimpleWorkflowService simpleWorkflowService;

}