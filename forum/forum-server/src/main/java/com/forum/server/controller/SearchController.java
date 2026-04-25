package com.forum.server.controller;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.vo.PostListVO;
import com.forum.server.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索模块")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "搜索帖子")
    public Result<PageResult<PostListVO>> searchPosts(@RequestParam String keyword,
                                                      @RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "15") Integer size) {
        return Result.success(searchService.searchPosts(keyword, page, size));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "获取搜索建议")
    public Result<List<String>> suggest(@RequestParam String keyword) {
        return Result.success(searchService.suggest(keyword));
    }
}
