package com.lucius.sparkcraftbackend.config;

import com.lucius.sparkcraftbackend.ai.node.ImageThreeDModelNode;
import com.lucius.sparkcraftbackend.service.ImageProjectService;
import com.lucius.sparkcraftbackend.service.ThreeDResultService;
import com.tencentcloudapi.ai3d.v20250513.Ai3dClient;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Component
@Slf4j
public class TencentCloudConfig {
    
    @Value("${tencent.cloud.secret-id}")
    private String secretId;
    
    @Value("${tencent.cloud.secret-key}")
    private String secretKey;
    
    @Value("${tencent.cloud.region:ap-guangzhou}")
    private String region;
    
    @Value("${tencent.cloud.ai3d.endpoint:ai3d.tencentcloudapi.com}")
    private String endpoint;
    
    @Resource
    private ImageProjectService imageProjectService;

    @Resource
    private ThreeDResultService threeDResultService;
    
    private Ai3dClient ai3dClientInstance;
    
    @Bean
    @Lazy
    public Ai3dClient ai3dClient() {
        if (ai3dClientInstance == null) {
            try {
                // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey
                Credential cred = new Credential(secretId, secretKey);
                
                // 实例化一个http选项，可选的，没有特殊需求可以跳过
                HttpProfile httpProfile = new HttpProfile();
                httpProfile.setEndpoint(endpoint);
                
                // 实例化一个client选项，可选的，没有特殊需求可以跳过
                ClientProfile clientProfile = new ClientProfile();
                clientProfile.setHttpProfile(httpProfile);
                
                // 实例化要请求产品的client对象，clientProfile是可选的
                ai3dClientInstance = new Ai3dClient(cred, region, clientProfile);
                
                log.info("✅ 腾讯云AI 3D客户端初始化成功，区域: {}, 端点: {}", region, endpoint);
                
            } catch (Exception e) {
                log.error("❌ 腾讯云AI 3D客户端初始化失败", e);
                throw new RuntimeException("腾讯云AI 3D客户端初始化失败", e);
            }
        }
        return ai3dClientInstance;
    }
    
    /**
     * 初始化完成后，将依赖注入到ImageThreeDModelNode
     */
    @PostConstruct
    public void initImageThreeDModelNode() {
        log.info("🔧 开始初始化ImageThreeDModelNode依赖注入...");
        try {
            // 先初始化AI 3D客户端实例
            log.info("📡 正在初始化腾讯云AI 3D客户端...");
            ai3dClient(); // 这会初始化ai3dClientInstance
            
            // 注入腾讯云AI 3D客户端
            ImageThreeDModelNode.setAi3dClient(ai3dClientInstance);
            log.info("✅ 腾讯云AI 3D客户端注入成功");
            
            // 注入ImageProject服务
            log.info("🗂️ 正在注入ImageProjectService...");
            ImageThreeDModelNode.setImageProjectService(imageProjectService);
            log.info("✅ ImageProjectService注入成功");
            
            // 注入ThreeDResult服务
            log.info("💾 正在注入ThreeDResultService...");
            ImageThreeDModelNode.setThreeDResultService(threeDResultService);
            log.info("✅ ThreeDResultService注入成功");
            
            log.info("🎉 ImageThreeDModelNode依赖注入完成");
            
        } catch (Exception e) {
            log.error("❌ ImageThreeDModelNode依赖注入失败", e);
        }
    }
}