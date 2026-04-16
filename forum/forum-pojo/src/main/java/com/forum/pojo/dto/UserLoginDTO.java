package com.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/** 用户登录 DTO */
@Data
public class UserLoginDTO implements Serializable {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
