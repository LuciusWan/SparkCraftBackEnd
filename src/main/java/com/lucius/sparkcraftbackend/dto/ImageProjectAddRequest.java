package com.lucius.sparkcraftbackend.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ImageProjectAddRequest implements Serializable {
    private String projectDesc;
    private String projectName;

    @Serial
    private static final long serialVersionUID = 1L;
}
