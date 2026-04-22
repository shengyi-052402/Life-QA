package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forum.common.context.BaseContext;
import com.forum.common.exception.BaseException;
import com.forum.common.result.PageResult;
import com.forum.pojo.entity.Notification;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.NotificationVO;
import com.forum.pojo.vo.UserVO;
import com.forum.server.mapper.NotificationMapper;
import com.forum.server.service.NotificationService;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final UserService userService;

    @Override
    public void createNotification(Long receiverUserId, Long senderUserId, String type, Long postId, Long commentId, String content) {
        if (receiverUserId == null || senderUserId == null || receiverUserId.equals(senderUserId)) {
            return;
        }

        save(Notification.builder()
                .receiverUserId(receiverUserId)
                .senderUserId(senderUserId)
                .type(type)
                .postId(postId)
                .commentId(commentId)
                .content(content)
                .isRead(0)
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishSystemNotification(String content) {
        Long currentUserId = BaseContext.getCurrentId();
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() != 1) {
            throw new BaseException("只有管理员可以发布系统通知");
        }

        List<User> users = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .select(User::getId));
        if (users.isEmpty()) {
            return;
        }

        List<Notification> notifications = new ArrayList<>(users.size());
        for (User user : users) {
            notifications.add(Notification.builder()
                    .receiverUserId(user.getId())
                    .senderUserId(currentUserId)
                    .type("system_notice")
                    .content(content)
                    .isRead(0)
                    .build());
        }
        saveBatch(notifications);
    }

    @Override
    public PageResult<NotificationVO> getMyNotifications(Integer page, Integer size, String type) {
        Long currentUserId = BaseContext.getCurrentId();
        Page<Notification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverUserId, currentUserId)
                .orderByDesc(Notification::getCreatedAt);
        if (StringUtils.hasText(type)) {
            queryWrapper.eq(Notification::getType, type.trim());
        }
        page(pageParam, queryWrapper);

        List<NotificationVO> records = pageParam.getRecords().stream()
                .map(this::buildNotificationVO)
                .collect(Collectors.toList());
        return new PageResult<>(pageParam.getTotal(), records);
    }

    @Override
    public Integer getUnreadCount() {
        Long currentUserId = BaseContext.getCurrentId();
        return Math.toIntExact(count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverUserId, currentUserId)
                .eq(Notification::getIsRead, 0)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId) {
        Long currentUserId = BaseContext.getCurrentId();
        Notification notification = getById(notificationId);
        if (notification == null || !notification.getReceiverUserId().equals(currentUserId)) {
            throw new BaseException("通知不存在");
        }

        if (notification.getIsRead() == 0) {
            notification.setIsRead(1);
            updateById(notification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead() {
        Long currentUserId = BaseContext.getCurrentId();
        List<Notification> notifications = list(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverUserId, currentUserId)
                .eq(Notification::getIsRead, 0));
        if (notifications.isEmpty()) {
            return;
        }

        notifications.forEach(item -> item.setIsRead(1));
        updateBatchById(notifications);
    }

    private NotificationVO buildNotificationVO(Notification notification) {
        User sender = userService.getById(notification.getSenderUserId());
        return NotificationVO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .postId(notification.getPostId())
                .commentId(notification.getCommentId())
                .content(notification.getContent())
                .isRead(notification.getIsRead() != null && notification.getIsRead() == 1)
                .createdAt(notification.getCreatedAt())
                .sender(buildUserVO(sender))
                .build();
    }

    private UserVO buildUserVO(User user) {
        if (user == null) {
            return null;
        }

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .role(user.getRole())
                .postCount(user.getPostCount())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
