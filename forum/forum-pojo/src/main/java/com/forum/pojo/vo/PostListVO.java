package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 帖子列表 VO */
@Data
@Builder
public class PostListVO implements Serializable {
    private Long id;
    private String title;
    private String titleHighlight;
    private String summary;
    private String summaryHighlight;
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
    /** 发帖地区名称 */
    private String locationName;
    /** 纬度 */
    private BigDecimal latitude;
    /** 经度 */
    private BigDecimal longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
