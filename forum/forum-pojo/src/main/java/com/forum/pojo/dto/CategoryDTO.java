package com.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/** 分类 DTO */
@Data
public class CategoryDTO implements Serializable {
    private Long id;
    @NotBlank(message = "分类名称不能为空")
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
}
