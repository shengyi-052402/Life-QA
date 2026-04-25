package com.forum.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserVO implements Serializable {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String bio;
    private Integer role;
    private Integer status;
    private Integer postCount;
    private LocalDateTime createdAt;
}
