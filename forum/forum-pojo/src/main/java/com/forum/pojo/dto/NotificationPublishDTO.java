package com.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotificationPublishDTO {

    @NotBlank(message = "通知内容不能为空")
    @Size(max = 255, message = "通知内容不能超过255个字符")
    private String content;
}
