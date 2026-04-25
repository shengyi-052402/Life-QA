package com.forum.server.controller.admin;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.dto.AdminCommentPageQueryDTO;
import com.forum.pojo.vo.AdminCommentVO;
import com.forum.server.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@Tag(name = "评论模块(管理后台)")
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "分页查询评论列表")
    public Result<PageResult<AdminCommentVO>> getCommentPage(AdminCommentPageQueryDTO dto) {
        return Result.success(commentService.getAdminCommentPage(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }
}
