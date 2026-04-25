package com.forum.server.controller.admin;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.dto.AdminUserPageQueryDTO;
import com.forum.pojo.dto.AdminUserUpdateDTO;
import com.forum.pojo.vo.AdminUserVO;
import com.forum.server.service.AdminService;
import com.forum.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "用户模块(管理后台)")
public class AdminUserController {

    private final AdminService adminService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "分页查询用户列表")
    public Result<PageResult<AdminUserVO>> getUserPage(AdminUserPageQueryDTO dto) {
        adminService.assertAdmin();
        return Result.success(userService.getAdminUserPage(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户状态或角色")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateDTO dto) {
        userService.adminUpdateUser(id, dto);
        return Result.success();
    }
}
