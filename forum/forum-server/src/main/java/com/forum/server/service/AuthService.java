package com.forum.server.service;

import com.forum.pojo.dto.UserLoginDTO;
import com.forum.pojo.vo.UserLoginVO;

public interface AuthService {
    
    /**
     * 用户登录
     */
    UserLoginVO login(UserLoginDTO loginDTO);
}
