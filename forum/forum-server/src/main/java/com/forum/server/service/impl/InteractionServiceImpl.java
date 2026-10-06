package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forum.common.context.BaseContext;
import com.forum.common.exception.BaseException;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.entity.CommentLike;
import com.forum.pojo.entity.Favorite;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.PostLike;
import com.forum.server.mapper.CommentLikeMapper;
import com.forum.server.mapper.CommentMapper;
import com.forum.server.mapper.FavoriteMapper;
import com.forum.server.mapper.PostLikeMapper;
import com.forum.server.mapper.PostMapper;
import com.forum.server.messaging.NotificationEventPublisher;
import com.forum.server.messaging.PostCacheInvalidationEventPublisher;
import com.forum.server.service.InteractionService;
import com.forum.server.service.distribution.ContentRankingService;
import com.forum.server.service.distribution.UserInterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final NotificationEventPublisher notificationEventPublisher;
    private final PostCacheInvalidationEventPublisher postCacheInvalidationEventPublisher;
    private final ContentRankingService contentRankingService;
    private final UserInterestService userInterestService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean togglePostLike(Long postId) {
        Long userId = BaseContext.getCurrentId();
        // The parent row serializes interactions on this post across service instances.
        // Read membership after locking; Redis never determines database writes.
        Post post = checkPostForUpdate(postId);
        boolean active = postLikeMapper.selectForUpdate(postId, userId) == null;
        if (active) {
            postLikeMapper.insert(PostLike.builder().postId(postId).userId(userId).build());
            postMapper.adjustLikeCount(postId, 1);
            notificationEventPublisher.publish(post.getUserId(), userId, "post_like", postId, null, "liked your post");
        } else {
            int deleted = postLikeMapper.delete(new LambdaQueryWrapper<PostLike>()
                    .eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId));
            postMapper.adjustLikeCount(postId, -deleted);
        }
        postCacheInvalidationEventPublisher.publishEvictAll(postId);
        contentRankingService.refreshPost(postId);
        userInterestService.recordInteraction(userId, postId, active ? 2.0 : -2.0);
        return active;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleCommentLike(Long commentId) {
        Long userId = BaseContext.getCurrentId();
        // Match the post -> comment lock order used by deletion.
        Comment initial = commentMapper.selectById(commentId);
        if (initial == null) {
            throw new BaseException("Comment does not exist");
        }
        checkPostForUpdate(initial.getPostId());
        Comment comment = commentMapper.selectByIdForUpdate(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BaseException("Comment does not exist");
        }

        boolean active = commentLikeMapper.selectForUpdate(commentId, userId) == null;
        if (active) {
            commentLikeMapper.insert(CommentLike.builder().commentId(commentId).userId(userId).build());
            commentMapper.adjustLikeCount(commentId, 1);
            notificationEventPublisher.publish(comment.getUserId(), userId, "comment_like", comment.getPostId(), commentId, "liked your comment");
        } else {
            int deleted = commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getCommentId, commentId).eq(CommentLike::getUserId, userId));
            commentMapper.adjustLikeCount(commentId, -deleted);
        }
        postCacheInvalidationEventPublisher.publishEvictDetail(comment.getPostId());
        return active;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean togglePostFavorite(Long postId) {
        Long userId = BaseContext.getCurrentId();
        Post post = checkPostForUpdate(postId);
        boolean active = favoriteMapper.selectForUpdate(postId, userId) == null;
        if (active) {
            favoriteMapper.insert(Favorite.builder().postId(postId).userId(userId).build());
            postMapper.adjustFavoriteCount(postId, 1);
            notificationEventPublisher.publish(post.getUserId(), userId, "post_favorite", postId, null, "favorited your post");
        } else {
            int deleted = favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getPostId, postId).eq(Favorite::getUserId, userId));
            postMapper.adjustFavoriteCount(postId, -deleted);
        }
        postCacheInvalidationEventPublisher.publishEvictAll(postId);
        contentRankingService.refreshPost(postId);
        userInterestService.recordInteraction(userId, postId, active ? 4.0 : -4.0);
        return active;
    }

    private Post checkPostForUpdate(Long postId) {
        Post post = postMapper.selectByIdForUpdate(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BaseException("Post does not exist or is hidden");
        }
        return post;
    }
}
