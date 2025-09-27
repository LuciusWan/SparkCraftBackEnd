package com.lucius.sparkcraftbackend.service;

import com.lucius.sparkcraftbackend.entity.ImageResource;

import java.util.List;

/**
 * 图片搜索服务接口
 */
public interface ImageSearchService {

    /**
     * 根据关键词搜索图片
     *
     * @param keywords 搜索关键词
     * @param count    搜索数量
     * @return 图片资源列表
     */
    List<ImageResource> searchImages(String keywords, int count);

    /**
     * 下载图片并上传到阿里云 OSS
     *
     * @param imageUrl 图片 URL
     * @param fileName 文件名
     * @return OSS 中的图片 URL
     */
    String downloadAndUploadToOss(String imageUrl, String fileName);
}