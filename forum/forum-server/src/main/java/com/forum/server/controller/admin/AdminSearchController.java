package com.forum.server.controller.admin;

import com.forum.common.result.Result;
import com.forum.server.service.AdminService;
import com.forum.server.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
@Tag(name = "搜索模块(管理后台)")
public class AdminSearchController {

    private final AdminService adminService;
    private final SearchService searchService;

    @PostMapping("/reindex")
    @Operation(summary = "全量重建搜索索引")
    public Result<Void> reindex() {
        adminService.assertAdmin();
        searchService.reindexAll();
        return Result.success();
    }
}
