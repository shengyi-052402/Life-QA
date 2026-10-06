package com.forum.server.controller;

import com.forum.common.result.Result;
import com.forum.pojo.dto.InteractionStateDTO;
import jakarta.validation.Valid;
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

    @PutMapping("/posts/{postId}/like")
    @Operation(summary = "点赞/取消点赞 帖子")
    public Result<Boolean> setPostLike(@PathVariable Long postId, @Valid @RequestBody InteractionStateDTO state) {
        return Result.success("操作成功", interactionService.setPostLike(postId, state.getActive()));
    }

    @PutMapping("/comments/{commentId}/like")
    @Operation(summary = "点赞/取消点赞 评论")
    public Result<Boolean> setCommentLike(@PathVariable Long commentId, @Valid @RequestBody InteractionStateDTO state) {
        return Result.success("操作成功", interactionService.setCommentLike(commentId, state.getActive()));
    }

    @PutMapping("/posts/{postId}/favorite")
    @Operation(summary = "收藏/取消收藏 帖子")
    public Result<Boolean> setPostFavorite(@PathVariable Long postId, @Valid @RequestBody InteractionStateDTO state) {
        return Result.success("操作成功", interactionService.setPostFavorite(postId, state.getActive()));
    }
}
