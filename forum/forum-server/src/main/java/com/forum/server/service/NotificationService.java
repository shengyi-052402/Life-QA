package com.forum.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forum.common.result.PageResult;
import com.forum.pojo.entity.Notification;
import com.forum.pojo.vo.NotificationVO;

public interface NotificationService extends IService<Notification> {

    void createNotification(Long receiverUserId, Long senderUserId, String type, Long postId, Long commentId, String content);

    void publishSystemNotification(String content);

    PageResult<NotificationVO> getMyNotifications(Integer page, Integer size, String type);

    Integer getUnreadCount();

    void markAsRead(Long notificationId);

    void markAllAsRead();
}
