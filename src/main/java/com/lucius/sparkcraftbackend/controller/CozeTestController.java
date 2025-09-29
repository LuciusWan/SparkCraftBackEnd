package com.lucius.sparkcraftbackend.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lucius.sparkcraftbackend.common.BaseResponse;
import com.lucius.sparkcraftbackend.common.ResultUtils;
import com.lucius.sparkcraftbackend.properties.AiServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coze API 测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/coze/test")
public class CozeTestController {

    private static final String COZE_API_URL = "https://api.coze.cn/v1/workflow/stream_run";
    private static final String COZE_WORKFLOW_ID = "7554768700659515435";
    
    @Resource
    private AiServiceProperties aiServiceProperties;

    /**
     * 测试 Coze API 连接
     */
    @GetMapping("/connection")
    public BaseResponse<Map<String, Object>> testConnection(@RequestParam(defaultValue = "东方之门") String keywords) {
        log.info("测试 Coze API 连接，关键词: {}", keywords);
        
        Map<String, Object> result = new HashMap<>();

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("workflow_id", COZE_WORKFLOW_ID);
            
            JSONObject parameters = new JSONObject();
            parameters.put("mainpotic", keywords);
            requestBody.put("parameters", parameters);
            
            log.info("发送请求到: {}", COZE_API_URL);
            log.info("请求体: {}", requestBody.toString());
            
            // 发送请求
            HttpResponse response = HttpRequest.post(COZE_API_URL)
                    .header("Authorization", "Bearer " + aiServiceProperties.getCoze().getApiToken())
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(15000)
                    .execute();
            
            result.put("statusCode", response.getStatus());
            result.put("responseBody", response.body());
            result.put("requestBody", requestBody.toString());
            result.put("keywords", keywords);
            result.put("responseLength", response.body().length());
            
            if (response.getStatus() == 200) {
                log.info("✅ Coze API 调用成功！响应长度: {} 字符", response.body().length());
                result.put("success", true);
                result.put("message", "API 调用成功");
                
                // 尝试解析响应中的图片
                List<Map<String, String>> parsedImages = parseResponseForImages(response.body(), keywords);
                result.put("parsedImages", parsedImages);
                result.put("imageCount", parsedImages.size());
                
            } else if (response.getStatus() == 401) {
                log.error("❌ 401 Unauthorized - Token 可能无效");
                result.put("success", false);
                result.put("message", "401 Unauthorized - Token 无效");
            } else {
                log.warn("⚠️ API 调用返回状态码: {}", response.getStatus());
                result.put("success", false);
                result.put("message", "API 调用返回状态码: " + response.getStatus());
            }
            
            return ResultUtils.success(result);
            

    }

    /**
     * 解析 SSE 响应中的图片信息
     */
    private List<Map<String, String>> parseResponseForImages(String responseBody, String keywords) {
        List<Map<String, String>> images = new ArrayList<>();
        
        try {
            // 按行解析 SSE 响应
            String[] lines = responseBody.split("\n");
            log.info("SSE 响应包含 {} 行", lines.length);
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                
                // 处理 SSE 格式的 data 行
                if (line.startsWith("data: ")) {
                    String dataContent = line.substring(6); // 移除 "data: " 前缀
                    
                    try {
                        JSONObject dataJson = JSONUtil.parseObj(dataContent);
                        
                        if (dataJson.containsKey("content")) {
                            String content = dataJson.getStr("content");
                            log.info("第 {} 行包含 content，长度: {}", i + 1, content.length());
                            
                            // 尝试解析 content 中的图片
                            JSONObject contentJson = JSONUtil.parseObj(content);
                            
                            if (contentJson.containsKey("imageurl")) {
                                JSONArray imageArray = contentJson.getJSONArray("imageurl");
                                log.info("找到 imageurl 数组，包含 {} 个图片", imageArray.size());
                                
                                for (int j = 0; j < imageArray.size(); j++) {
                                    JSONObject imageObj = imageArray.getJSONObject(j);
                                    
                                    if (imageObj.containsKey("picture_info")) {
                                        JSONObject pictureInfo = imageObj.getJSONObject("picture_info");
                                        
                                        String displayUrl = pictureInfo.getStr("display_url");
                                        String title = pictureInfo.getStr("title", "");
                                        
                                        if (displayUrl != null && !displayUrl.isEmpty()) {
                                            Map<String, String> imageInfo = new HashMap<>();
                                            imageInfo.put("url", displayUrl);
                                            imageInfo.put("title", title);
                                            imageInfo.put("description", title + " - " + keywords);
                                            
                                            images.add(imageInfo);
                                            log.info("✅ 解析到图片: {} - {}", title, displayUrl);
                                        }
                                    }
                                }
                            }
                        }
                        
                    } catch (Exception e) {
                        log.debug("解析第 {} 行的 data 内容失败: {}", i + 1, e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("解析 SSE 响应图片失败", e);
        }
        
        return images;
    }
}