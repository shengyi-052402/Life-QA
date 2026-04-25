package com.forum.server.controller.admin;

import com.forum.common.result.Result;
import com.forum.pojo.dto.CategoryDTO;
import com.forum.server.service.AdminService;
import com.forum.server.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Tag(name = "分类模块(管理后台)")
public class AdminCategoryController {

    private final AdminService adminService;
    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "创建分类")
    public Result<Void> addCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        adminService.assertAdmin();
        categoryService.addCategory(categoryDTO);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑分类")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDTO categoryDTO) {
        adminService.assertAdmin();
        categoryDTO.setId(id);
        categoryService.updateCategory(categoryDTO);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminService.assertAdmin();
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
