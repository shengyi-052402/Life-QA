package com.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/** 编辑帖子 DTO */
@Data
public class PostUpdateDTO implements Serializable {
    @NotNull
    private Long id;
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    @NotNull(message = "请选择分类")
    private Long categoryId;
    private List<Long> tagIds;
    private List<String> newTags;
}
