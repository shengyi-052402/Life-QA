package com.forum.server.controller;

import com.forum.common.context.BaseContext;
import com.forum.common.result.Result;
import com.forum.pojo.dto.UserUpdateDTO;
import com.forum.pojo.vo.UserVO;
import com.forum.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户模块")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息")
    public Result<UserVO> getCurrentUserInfo() {
        Long currentId = BaseContext.getCurrentId();
        UserVO userVO = userService.getUserProfile(currentId);
        return Result.success(userVO);
    }

    @PutMapping("/me")
    @Operation(summary = "更新当前登录用户资料")
    public Result<Void> updateCurrentUserInfo(@Valid @RequestBody UserUpdateDTO dto) {
        Long currentId = BaseContext.getCurrentId();
        userService.updateProfile(currentId, dto);
        return Result.success("资料修改成功", null);
    }
}
