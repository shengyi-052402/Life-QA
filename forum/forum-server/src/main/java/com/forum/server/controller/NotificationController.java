package com.forum.server.controller;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.dto.NotificationPublishDTO;
import com.forum.pojo.vo.NotificationVO;
import com.forum.server.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知中心")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取我的通知列表")
    public Result<PageResult<NotificationVO>> getMyNotifications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type) {
        return Result.success(notificationService.getMyNotifications(page, size, type));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数")
    public Result<Integer> getUnreadCount() {
        return Result.success(notificationService.getUnreadCount());
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "将单条通知标记为已读")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success("操作成功", null);
    }

    @PostMapping("/read-all")
    @Operation(summary = "将所有通知标记为已读")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return Result.success("操作成功", null);
    }

    @PostMapping("/system")
    @Operation(summary = "发布系统通知")
    public Result<Void> publishSystemNotification(@Valid @RequestBody NotificationPublishDTO dto) {
        notificationService.publishSystemNotification(dto.getContent().trim());
        return Result.success("发布成功", null);
    }
}
