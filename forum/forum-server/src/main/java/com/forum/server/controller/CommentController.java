package com.forum.server.controller;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.dto.CommentCreateDTO;
import com.forum.pojo.dto.CommentPageQueryDTO;
import com.forum.pojo.vo.CommentVO;
import com.forum.server.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "评论模块")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "分页查询评论")
    public Result<PageResult<CommentVO>> getCommentPage(CommentPageQueryDTO dto) {
        return Result.success(commentService.getCommentPage(dto));
    }

    @PostMapping
    @Operation(summary = "发表评论")
    public Result<Long> createComment(@Valid @RequestBody CommentCreateDTO dto) {
        return Result.success("评论成功", commentService.createComment(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除某条评论")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success("删除成功", null);
    }
}
