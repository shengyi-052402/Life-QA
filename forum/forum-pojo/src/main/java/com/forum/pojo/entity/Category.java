package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("category")
public class Category implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Integer postCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
