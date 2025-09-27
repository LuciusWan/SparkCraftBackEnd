package com.lucius.sparkcraftbackend.service;

import com.lucius.sparkcraftbackend.dto.ImageProjectQueryRequest;
import com.lucius.sparkcraftbackend.entity.User;
import com.lucius.sparkcraftbackend.vo.ImageProjectVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.lucius.sparkcraftbackend.entity.ImageProject;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
public interface ImageProjectService extends IService<ImageProject> {

    ImageProjectVO getImageVO(ImageProject imageProject);

    QueryWrapper getQueryWrapper(ImageProjectQueryRequest imageProjectQueryRequest);

    List<ImageProjectVO> getImageVOList(List<ImageProject> records);

    Flux<String> chatToGetIdea(Long imageProjectId ,String  message, User loginUser);
}
