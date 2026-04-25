package com.forum.server.controller;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.dto.PostCreateDTO;
import com.forum.pojo.dto.PostPageQueryDTO;
import com.forum.pojo.dto.PostUpdateDTO;
import com.forum.pojo.vo.PostDetailVO;
import com.forum.pojo.vo.PostGlobeVO;
import com.forum.pojo.vo.PostListVO;
import com.forum.server.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "帖子模块(前台)")
public class PostController {

    private final PostService postService;

    @GetMapping
    @Operation(summary = "分页查询帖子列表")
    public Result<PageResult<PostListVO>> getPostPage(PostPageQueryDTO queryDTO) {
        return Result.success(postService.getPostPage(queryDTO));
    }

    @GetMapping("/globe")
    @Operation(summary = "获取有地理位置的帖子（供3D地球渲染）")
    public Result<List<PostGlobeVO>> getGlobePosts() {
        return Result.success(postService.getGlobePosts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取帖子详情")
    public Result<PostDetailVO> getPostDetail(@PathVariable Long id) {
        return Result.success(postService.getPostDetail(id));
    }

    @GetMapping("/{id}/edit")
    @Operation(summary = "获取帖子编辑信息")
    public Result<PostDetailVO> getPostEditDetail(@PathVariable Long id) {
        return Result.success(postService.getPostEditDetail(id));
    }

    @PostMapping
    @Operation(summary = "发布帖子")
    public Result<Long> createPost(@Valid @RequestBody PostCreateDTO dto) {
        Long postId = postService.createPost(dto);
        return Result.success("发布成功", postId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改帖子")
    public Result<Void> updatePost(@PathVariable Long id, @Valid @RequestBody PostUpdateDTO dto) {
        dto.setId(id);
        postService.updatePost(dto);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除帖子")
    public Result<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success("删除成功", null);
    }
}
