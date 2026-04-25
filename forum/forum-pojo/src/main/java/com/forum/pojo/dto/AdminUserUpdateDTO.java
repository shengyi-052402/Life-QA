package com.forum.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
public class AdminUserUpdateDTO implements Serializable {

    @Min(value = 0, message = "角色参数错误")
    @Max(value = 1, message = "角色参数错误")
    private Integer role;

    @Min(value = 0, message = "状态参数错误")
    @Max(value = 1, message = "状态参数错误")
    private Integer status;
}
