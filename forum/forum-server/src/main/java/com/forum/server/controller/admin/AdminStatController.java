package com.forum.server.controller.admin;

import com.forum.common.result.Result;
import com.forum.pojo.vo.StatVO;
import com.forum.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@Tag(name = "统计模块(管理后台)")
public class AdminStatController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "获取后台统计数据")
    public Result<StatVO> getStats() {
        return Result.success(adminService.getStats());
    }
}
