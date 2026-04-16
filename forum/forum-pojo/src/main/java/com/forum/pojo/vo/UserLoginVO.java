package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

/** 用户登录返回 VO */
@Data
@Builder
public class UserLoginVO implements Serializable {
    private String token;
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;
}
