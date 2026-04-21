package com.forum.server.controller;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.dto.CommentCreateDTO;
import com.forum.pojo.dto.CommentPageQueryDTO;
import com.forum.pojo.vo.CommentLocationVO;
import com.forum.pojo.vo.CommentVO;
import com.forum.server.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{id}/location")
    @Operation(summary = "获取评论定位信息")
    public Result<CommentLocationVO> getCommentLocation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") Integer topSize,
            @RequestParam(defaultValue = "10") Integer replySize) {
        return Result.success(commentService.getCommentLocation(id, topSize, replySize));
    }

    @PostMapping
    @Operation(summary = "发表评论")
    public Result<Long> createComment(@Valid @RequestBody CommentCreateDTO dto) {
        return Result.success("评论成功", commentService.createComment(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success("删除成功", null);
    }
}
