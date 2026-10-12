package com.forum.pojo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryFailureReplayDTO {
    @NotBlank(message = "source cannot be blank")
    @Pattern(regexp = "outbox|consumer", message = "source must be outbox or consumer")
    private String source;

    @NotEmpty(message = "ids cannot be empty")
    private List<Long> ids;
}
