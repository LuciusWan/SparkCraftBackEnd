package com.lucius.sparkcraftbackend.dto;

import com.lucius.sparkcraftbackend.common.PageRequest;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
@EqualsAndHashCode(callSuper = true)
@Data
public class ImageProjectQueryRequest extends PageRequest implements Serializable {
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
     * 优先级
     */
    @Column("priority")
    private Integer priority;


    @Serial
    private static final long serialVersionUID = 1L;
}
