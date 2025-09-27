package com.lucius.sparkcraftbackend.vo;

import com.lucius.sparkcraftbackend.entity.User;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ImageProjectVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator,value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 项目名称
     */
    @Column("projectName")
    private String projectName;

    /**
     * 项目描述
     */
    @Column("projectDesc")
    private String projectDesc;

    /**
     * 生成图片地址
     */
    @Column("projectImageUrl")
    private String projectImageUrl;

    /**
     * 制造流程
     */
    private String productionprocess;

    /**
     * 3D模型地址
     */
    @Column("3DModelUrl")
    private String ThreeDModelUrl;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 用户信息
     */
    @Column("user")
    private UserVO user;
}
