package com.forum.server.controller.admin;

import com.forum.common.result.PageResult;
import com.forum.common.result.Result;
import com.forum.pojo.dto.DeliveryFailureReplayDTO;
import com.forum.pojo.vo.DeliveryFailureVO;
import com.forum.server.service.NotificationDeliveryFailureService;
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
@RequestMapping("/api/admin/notification-delivery/failures")
@RequiredArgsConstructor
@Tag(name = "通知投递失败管理")
public class AdminNotificationDeliveryController {
    private final NotificationDeliveryFailureService failureService;

    @GetMapping
    @Operation(summary = "分页查询通知投递失败记录")
    public Result<PageResult<DeliveryFailureVO>> list(
            @RequestParam(defaultValue = "outbox") String source,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(failureService.list(source, page, size));
    }

    @PostMapping("/{source}/{id}/replay")
    @Operation(summary = "重放单条通知投递失败记录")
    public Result<Void> replay(@PathVariable String source, @PathVariable Long id) {
        failureService.replay(source, id);
        return Result.success("已提交重放", null);
    }

    @PostMapping("/replay")
    @Operation(summary = "批量重放通知投递失败记录")
    public Result<Void> replayBatch(@Valid @RequestBody DeliveryFailureReplayDTO dto) {
        failureService.replayBatch(dto.getSource(), dto.getIds());
        return Result.success("已提交批量重放", null);
    }
}
