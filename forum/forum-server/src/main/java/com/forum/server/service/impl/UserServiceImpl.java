package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forum.common.constant.MessageConstant;
import com.forum.common.exception.BaseException;
import com.forum.pojo.dto.UserRegisterDTO;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.UserVO;
import com.forum.server.mapper.UserMapper;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(UserRegisterDTO registerDTO) {
        // 校验用户名是否存在
        long countByUsername = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (countByUsername > 0) {
            throw new BaseException(MessageConstant.USERNAME_EXISTS);
        }

        // 校验邮箱是否存在
        long countByEmail = count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, registerDTO.getEmail()));
        if (countByEmail > 0) {
            throw new BaseException(MessageConstant.EMAIL_EXISTS);
        }

        // 保存新用户
        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .nickname(registerDTO.getUsername()) // 默认昵称设为用户名
                .role(0) // 普通用户
                .status(1) // 正常状态
                .postCount(0)
                .build();
        
        save(user);
    }

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Override
    public UserVO getUserProfile(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .role(user.getRole())
                .postCount(user.getPostCount())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
