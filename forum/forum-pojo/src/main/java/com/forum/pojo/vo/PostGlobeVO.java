package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 地球光点数据 VO
 * 专为首页 3D 地球渲染使用，仅包含必要字段，不做分页
 */
@Data
@Builder
public class PostGlobeVO implements Serializable {
    /** 帖子ID */
    private Long id;
    /** 帖子标题 */
    private String title;
    /** 封面图片（用于光点弹窗预览） */
    private String coverImage;
    /** 地区名称 */
    private String locationName;
    /** 精确地址 */
    private String address;
    /** Google Place ID */
    private String placeId;
    /** 纬度 */
    private BigDecimal latitude;
    /** 经度 */
    private BigDecimal longitude;
    /** 点赞数（用于控制光点亮度/大小） */
    private Integer likeCount;
    /** 作者昵称 */
    private String authorNickname;
    /** 作者头像 */
    private String authorAvatar;
}
