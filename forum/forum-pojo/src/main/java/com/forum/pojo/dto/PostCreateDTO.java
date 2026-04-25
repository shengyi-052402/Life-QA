package com.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** 发布帖子 DTO */
@Data
public class PostCreateDTO implements Serializable {
    @NotBlank(message = "标题不能为空")
    @Size(min = 5, max = 200, message = "标题长度为5-200字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "必须提供一段基本介绍介绍")
    @Size(max = 500, message = "介绍文字过长")
    private String summary;

    @NotBlank(message = "必须上传背景封面图片")
    private String coverImage;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    /** 已有标签ID列表 */
    private List<Long> tagIds;

    /** 新标签名称列表 */
    private List<String> newTags;

    /** 发帖地区名称（可选，如"北京"、"Tokyo"） */
    private String locationName;

    /** 纬度（可选，与 locationName 配套） */
    private BigDecimal latitude;

    /** 经度（可选，与 locationName 配套） */
    private BigDecimal longitude;
}
