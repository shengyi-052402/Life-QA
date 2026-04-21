package com.forum.server.controller;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.vo.PostListVO;
import com.forum.pojo.vo.UserActivityVO;
import com.forum.pojo.vo.UserVO;
import com.forum.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户中心模块")
public class UserCenterController {

    private final UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "获取用户公开资料")
    public Result<UserVO> getUserProfile(@PathVariable Long id) {
        return Result.success(userService.getUserProfile(id));
    }

    @GetMapping("/{id}/posts")
    @Operation(summary = "获取用户帖子列表")
    public Result<PageResult<PostListVO>> getUserPosts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(userService.getUserPosts(id, page, size));
    }

    @GetMapping("/me/favorites")
    @Operation(summary = "获取我的收藏列表")
    public Result<PageResult<PostListVO>> getMyFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(userService.getMyFavorites(page, size));
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "获取用户动态列表")
    public Result<PageResult<UserActivityVO>> getUserActivities(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(userService.getUserActivities(id, page, size));
    }
}
