package com.forum.server.controller;

import com.forum.common.result.Result;
import com.forum.pojo.vo.TagVO;
import com.forum.server.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "标签模块")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "获取标签列表")
    public Result<List<TagVO>> getTags(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        return Result.success(tagService.getTagList(page, size));
    }

    @GetMapping("/hot")
    @Operation(summary = "获取热门标签")
    public Result<List<TagVO>> getHotTags(@RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(tagService.getHotTags(limit));
    }
}
