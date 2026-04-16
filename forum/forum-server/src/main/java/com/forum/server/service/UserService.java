package com.forum.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forum.pojo.dto.UserRegisterDTO;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.UserVO;

public interface UserService extends IService<User> {
    
    /**
     * 注册用户
     */
    void register(UserRegisterDTO registerDTO);

    /**
     * 根据用户名获取用户
     */
    User getByUsername(String username);

    /**
     * 获取用户公开信息
     */
    UserVO getUserProfile(Long id);
}
