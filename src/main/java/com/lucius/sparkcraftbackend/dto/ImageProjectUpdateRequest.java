package com.lucius.sparkcraftbackend.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
public class ImageProjectUpdateRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 工程名称
     */
    private String projectName;

    @Serial
    private static final long serialVersionUID = 1L;
}
