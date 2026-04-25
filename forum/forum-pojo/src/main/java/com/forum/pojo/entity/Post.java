package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 帖子实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("post")
public class Post implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String summary;

    private String coverImage;

    private Long userId;

    private Long categoryId;

    /** 发帖地区名称（城市/地区） */
    private String locationName;

    /** 纬度 (-90 to 90) */
    private BigDecimal latitude;

    /** 经度 (-180 to 180) */
    private BigDecimal longitude;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer favoriteCount;

    /** 是否置顶: 0=否, 1=是 */
    private Integer isTop;

    /** 是否精华: 0=否, 1=是 */
    private Integer isEssence;

    /** 0=待审核, 1=正常, 2=隐藏 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
