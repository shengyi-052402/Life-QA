package com.forum.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 操作日志实体 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("operation_log")
public class OperationLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String ip;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
