package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import java.io.Serializable;

/** 帖子-标签关联实体 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("post_tag")
public class PostTag implements Serializable {
    private Long postId;
    private Long tagId;
}
