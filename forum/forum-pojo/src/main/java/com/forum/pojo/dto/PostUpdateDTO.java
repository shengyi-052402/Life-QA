package com.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
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
    @NotBlank(message = "必须提供一段基本介绍介绍")
    private String summary;
    @NotBlank(message = "必须上传背景封面图片")
    private String coverImage;
    @NotNull(message = "请选择分类")
    private Long categoryId;
    private List<Long> tagIds;
    private List<String> newTags;

    /** 发帖地区名称（可选，如"北京"、"Tokyo"） */
    private String locationName;

    /** 精确地址（可选） */
    private String address;

    /** Google Place ID（可选） */
    private String placeId;

    /** 纬度（可选） */
    private BigDecimal latitude;

    /** 经度（可选） */
    private BigDecimal longitude;
}
