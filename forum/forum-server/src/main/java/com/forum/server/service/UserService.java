package com.forum.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forum.common.result.PageResult;
import com.forum.pojo.dto.UserRegisterDTO;
import com.forum.pojo.dto.UserUpdateDTO;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.PostListVO;
import com.forum.pojo.vo.UserActivityVO;
import com.forum.pojo.vo.UserVO;

public interface UserService extends IService<User> {

    void register(UserRegisterDTO registerDTO);

    User getByUsername(String username);

    UserVO getUserProfile(Long id);

    void updateProfile(Long userId, UserUpdateDTO dto);

    PageResult<PostListVO> getUserPosts(Long userId, Integer page, Integer size);

    PageResult<PostListVO> getMyFavorites(Integer page, Integer size);

    PageResult<UserActivityVO> getUserActivities(Long userId, Integer page, Integer size);
}
