package com.forum.pojo.dto;

import lombok.Data;
import java.io.Serializable;

/** 用户资料修改 DTO */
@Data
public class UserUpdateDTO implements Serializable {
    private String nickname;
    private String bio;
    private String avatar;
}
