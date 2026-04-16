package com.forum.server.controller;

import com.forum.common.context.BaseContext;
import com.forum.common.result.Result;
import com.forum.pojo.dto.UserLoginDTO;
import com.forum.pojo.dto.UserRegisterDTO;
import com.forum.pojo.vo.UserLoginVO;
import com.forum.server.service.AuthService;
import com.forum.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证模块")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        UserLoginVO userLoginVO = authService.login(loginDTO);
        return Result.success("登录成功", userLoginVO);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功", null);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token")
    public Result<UserLoginVO> refresh() {
        // 在拦截器中已经校验过 Token 并设置了 BaseContext
        // 这个接口仅在真正需要时实现更复杂的长短token逻辑
        // 目前返回成功即可，如果需要刷新机制可在此派发新token
        return Result.success();
    }
}
