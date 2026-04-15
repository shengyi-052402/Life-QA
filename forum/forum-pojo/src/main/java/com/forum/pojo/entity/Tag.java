package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 标签实体 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("tag")
public class Tag implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer postCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
