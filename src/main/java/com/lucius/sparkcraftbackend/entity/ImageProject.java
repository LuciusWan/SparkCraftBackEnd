package com.lucius.sparkcraftbackend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  实体类。
 *
 * @author <a href="https://github.com/LuciusWan">LuciusWan</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("image_project")
public class ImageProject implements Serializable {

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
     * 项目状态
     */
    @Column("projectStatus")
    private String projectStatus;

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
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
