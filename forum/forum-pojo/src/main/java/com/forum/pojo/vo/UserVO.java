package com.forum.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 用户公开信息 VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO implements Serializable {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private Integer role;
    private Integer postCount;
    private LocalDateTime createdAt;
}
