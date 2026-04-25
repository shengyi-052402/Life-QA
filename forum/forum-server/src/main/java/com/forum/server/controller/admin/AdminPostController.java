package com.forum.server.controller.admin;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.dto.AdminPostPageQueryDTO;
import com.forum.pojo.dto.AdminPostUpdateDTO;
import com.forum.pojo.vo.AdminPostVO;
import com.forum.server.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@Tag(name = "帖子模块(管理后台)")
public class AdminPostController {

    private final PostService postService;

    @GetMapping
    @Operation(summary = "分页查询帖子列表")
    public Result<PageResult<AdminPostVO>> getPostPage(AdminPostPageQueryDTO dto) {
        return Result.success(postService.getAdminPostPage(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新帖子状态/置顶/精华")
    public Result<Void> updatePost(@PathVariable Long id, @Valid @RequestBody AdminPostUpdateDTO dto) {
        postService.adminUpdatePost(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除帖子")
    public Result<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success();
    }
}
