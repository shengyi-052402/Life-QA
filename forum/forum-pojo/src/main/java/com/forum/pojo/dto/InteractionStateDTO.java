package com.forum.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InteractionStateDTO {
    @NotNull(message = "active is required")
    private Boolean active;
}
