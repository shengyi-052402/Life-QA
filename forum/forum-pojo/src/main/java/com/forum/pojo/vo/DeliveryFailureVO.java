package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryFailureVO {
    private Long id;
    private String source;
    private String eventId;
    private Integer status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
}
