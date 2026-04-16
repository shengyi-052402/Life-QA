package com.forum.server.service.impl;

import com.forum.common.constant.MessageConstant;
import com.forum.common.constant.StatusConstant;
import com.forum.common.exception.AccountNotFoundException;
import com.forum.common.exception.BaseException;
import com.forum.common.exception.PasswordErrorException;
import com.forum.common.utils.JwtUtil;
import com.forum.pojo.dto.UserLoginDTO;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.UserLoginVO;
import com.forum.server.service.AuthService;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
        User user = userService.getByUsername(loginDTO.getUsername());
        
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (user.getStatus() == StatusConstant.USER_DISABLED) {
            throw new BaseException(MessageConstant.ACCOUNT_DISABLED);
        }

        // 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String token = JwtUtil.generateToken(claims);

        return UserLoginVO.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }
}
