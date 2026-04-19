package com.forum.server.controller;

import com.forum.common.result.Result;
import com.forum.server.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
@Tag(name = "互动模块")
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/posts/{postId}/like")
    @Operation(summary = "点赞/取消点赞 帖子")
    public Result<Boolean> togglePostLike(@PathVariable Long postId) {
        return Result.success("操作成功", interactionService.togglePostLike(postId));
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "点赞/取消点赞 评论")
    public Result<Boolean> toggleCommentLike(@PathVariable Long commentId) {
        return Result.success("操作成功", interactionService.toggleCommentLike(commentId));
    }

    @PostMapping("/posts/{postId}/favorite")
    @Operation(summary = "收藏/取消收藏 帖子")
    public Result<Boolean> togglePostFavorite(@PathVariable Long postId) {
        return Result.success("操作成功", interactionService.togglePostFavorite(postId));
    }
}
