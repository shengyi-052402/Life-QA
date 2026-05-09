package com.forum.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 帖子详情 VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailVO implements Serializable {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    private UserVO author;
    private Long categoryId;
    private String categoryName;
    private List<TagVO> tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Boolean isTop;
    private Boolean isEssence;
    private Boolean isLiked;
    private Boolean isFavorited;
    /** 发帖地区名称 */
    private String locationName;
    /** 精确地址 */
    private String address;
    /** Google Place ID */
    private String placeId;
    /** 纬度 */
    private BigDecimal latitude;
    /** 经度 */
    private BigDecimal longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
