package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.NotificationOutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface NotificationOutboxEventMapper extends BaseMapper<NotificationOutboxEvent> {

    @Update("""
            UPDATE notification_outbox_event
            SET status = 1, lock_token = #{token}, locked_until = #{lockedUntil}
            WHERE id = #{id}
              AND ((status = 0 AND next_retry_at <= NOW())
                OR (status = 1 AND locked_until < NOW()))
            """)
    int claim(@Param("id") Long id, @Param("token") String token,
              @Param("lockedUntil") LocalDateTime lockedUntil);

    @Update("""
            UPDATE notification_outbox_event
            SET status = 2, sent_at = NOW(), last_error = NULL,
                lock_token = NULL, locked_until = NULL
            WHERE id = #{id} AND status = 1 AND lock_token = #{token}
            """)
    int markSent(@Param("id") Long id, @Param("token") String token);

    @Update("""
            UPDATE notification_outbox_event
            SET status = #{status}, retry_count = #{retryCount}, next_retry_at = #{nextRetryAt},
                last_error = #{lastError}, lock_token = NULL, locked_until = NULL
            WHERE id = #{id} AND status = 1 AND lock_token = #{token}
            """)
    int markFailedAttempt(@Param("id") Long id, @Param("token") String token,
                          @Param("status") Integer status, @Param("retryCount") Integer retryCount,
                          @Param("nextRetryAt") LocalDateTime nextRetryAt,
                          @Param("lastError") String lastError);

    @Update("""
            UPDATE notification_outbox_event
            SET status = 0, retry_count = 0, next_retry_at = NOW(), last_error = NULL,
                lock_token = NULL, locked_until = NULL
            WHERE id = #{id} AND status = 3
            """)
    int resetFailed(@Param("id") Long id);
}
