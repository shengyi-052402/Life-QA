package com.forum.server.service.impl;

import com.forum.common.context.BaseContext;
import com.forum.common.exception.BaseException;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.Comment;
import com.forum.server.mapper.PostMapper;
import com.forum.server.mapper.CommentMapper;
import com.forum.server.service.InteractionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/** Requests own a personal relation and its durable event, never a content counter. */
@Service
public class InteractionServiceImpl implements InteractionService {
    private final PostMapper posts;
    private final CommentMapper comments;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public InteractionServiceImpl(PostMapper posts, CommentMapper comments, JdbcTemplate jdbc,
                                  PlatformTransactionManager manager) {
        this.posts = posts;
        this.comments = comments;
        this.jdbc = jdbc;
        transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(10);
    }

    @Override public boolean setPostLike(Long id, boolean active) {
        return setState("post_like", "post_id", id, active);
    }

    @Override public boolean setPostFavorite(Long id, boolean active) {
        return setState("favorite", "post_id", id, active);
    }

    @Override public boolean setCommentLike(Long id, boolean active) {
        return setState("comment_like", "comment_id", id, active);
    }

    private boolean setState(String table, String column, Long id, boolean active) {
        Long user = BaseContext.getCurrentId();
        if (user == null) throw new BaseException("Please log in");
        if (id == null || id <= 0) throw new BaseException("Invalid target ID");
        for (int attempt = 0; ; attempt++) {
            try {
                return Boolean.TRUE.equals(transaction.execute(status -> {
                    Long postId = id;
                    Long receiver;
                    if (table.equals("comment_like")) {
                        Comment comment = comments.selectById(id);
                        if (comment == null || !Integer.valueOf(1).equals(comment.getStatus())) {
                            throw new BaseException("Comment does not exist or is hidden");
                        }
                        postId = comment.getPostId();
                        receiver = comment.getUserId();
                    } else {
                        receiver = null;
                    }
                    Post post = posts.selectById(postId);
                    if (post == null || !Integer.valueOf(1).equals(post.getStatus())) {
                        throw new BaseException("Post does not exist or is hidden");
                    }
                    if (receiver == null) receiver = post.getUserId();
                    boolean present = !jdbc.queryForList("SELECT user_id FROM " + table
                            + " WHERE user_id = ? AND " + column + " = ? FOR UPDATE", user, id).isEmpty();
                    if (present == active) return active;
                    if (active) {
                        jdbc.update("INSERT INTO " + table + " (user_id, " + column + ") VALUES (?, ?)", user, id);
                    } else {
                        jdbc.update("DELETE FROM " + table + " WHERE user_id = ? AND " + column + " = ?", user, id);
                    }
                    jdbc.update("""
                            INSERT INTO interaction_event
                            (event_id, kind, user_id, target_id, post_id, receiver_id, active)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """, UUID.randomUUID().toString(), table, user, id, postId, receiver, active);
                    return active;
                }));
            } catch (DuplicateKeyException | TransientDataAccessException ex) {
                // Retry the entire transaction after rollback, at most three times.
                if (attempt >= 3) throw ex;
            }
        }
    }
}