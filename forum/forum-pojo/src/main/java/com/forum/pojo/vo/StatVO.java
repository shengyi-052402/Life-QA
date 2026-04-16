package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

/** 后台统计数据 VO */
@Data
@Builder
public class StatVO implements Serializable {
    private Long totalUsers;
    private Long totalPosts;
    private Long totalComments;
    private Long todayNewPosts;
    private Long todayNewUsers;
    private Long todayNewComments;
}
