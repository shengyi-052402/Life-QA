package com.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/** 发布帖子 DTO */
@Data
public class PostCreateDTO implements Serializable {
    @NotBlank(message = "标题不能为空")
    @Size(min = 5, max = 200, message = "标题长度为5-200字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    /** 已有标签ID列表 */
    private List<Long> tagIds;

    /** 新标签名称列表 */
    private List<String> newTags;
}
