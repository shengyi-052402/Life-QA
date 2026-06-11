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
import com.forum.server.service.cache.PostInteractionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final PostInteractionCacheService postInteractionCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean togglePostLike(Long postId) {
        Long userId = BaseContext.getCurrentId();
        Post post = checkPost(postId);
        ensurePostLikeCacheInitialized(postId);

        Optional<PostInteractionCacheService.ToggleResult> cacheResult =
                postInteractionCacheService.togglePostLike(postId, userId);
        if (cacheResult.isPresent()) {
            return syncPostLikeByCacheResult(post, userId, cacheResult.get());
        }

        // Redis is an acceleration layer here. If it is unavailable, keep the user action working with the DB path.
        return togglePostLikeByDatabase(post, userId);
    }

    private boolean syncPostLikeByCacheResult(Post post, Long userId, PostInteractionCacheService.ToggleResult result) {
        Long postId = post.getId();
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId);

        if (result.isActive()) {
            if (postLikeMapper.selectCount(wrapper) == 0) {
                postLikeMapper.insert(PostLike.builder().postId(postId).userId(userId).build());
            }
            updatePostLikeCount(post, result.getCount());
            postCacheInvalidationEventPublisher.publishEvictDetail(postId);
            notificationEventPublisher.publish(post.getUserId(), userId, "post_like", postId, null, "liked your post");
            return true;
        }

        postLikeMapper.delete(wrapper);
        updatePostLikeCount(post, result.getCount());
        postCacheInvalidationEventPublisher.publishEvictDetail(postId);
        return false;
    }

    private boolean togglePostLikeByDatabase(Post post, Long userId) {
        Long postId = post.getId();
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId);

        PostLike exist = postLikeMapper.selectOne(wrapper);
        if (exist == null) {
            postLikeMapper.insert(PostLike.builder().postId(postId).userId(userId).build());
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            postCacheInvalidationEventPublisher.publishEvictDetail(postId);
            notificationEventPublisher.publish(post.getUserId(), userId, "post_like", postId, null, "liked your post");
            return true;
        }

        postLikeMapper.delete(wrapper);
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postMapper.updateById(post);
        postCacheInvalidationEventPublisher.publishEvictDetail(postId);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleCommentLike(Long commentId) {
        Long userId = BaseContext.getCurrentId();
        Comment comment = checkComment(commentId);

        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId);

        Post post = postMapper.selectById(comment.getPostId());
        CommentLike exist = commentLikeMapper.selectOne(wrapper);
        if (exist == null) {
            commentLikeMapper.insert(CommentLike.builder().commentId(commentId).userId(userId).build());
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
            if (post != null) {
                postCacheInvalidationEventPublisher.publishEvictDetail(post.getId());
            }
            notificationEventPublisher.publish(comment.getUserId(), userId, "comment_like", comment.getPostId(), commentId, "liked your comment");
            return true;
        }

        commentLikeMapper.delete(wrapper);
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentMapper.updateById(comment);
        if (post != null) {
            postCacheInvalidationEventPublisher.publishEvictDetail(post.getId());
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean togglePostFavorite(Long postId) {
        Long userId = BaseContext.getCurrentId();
        Post post = checkPost(postId);
        ensurePostFavoriteCacheInitialized(postId);

        Optional<PostInteractionCacheService.ToggleResult> cacheResult =
                postInteractionCacheService.togglePostFavorite(postId, userId);
        if (cacheResult.isPresent()) {
            return syncPostFavoriteByCacheResult(post, userId, cacheResult.get());
        }

        // The favorite table primary key still protects idempotency when Redis falls back.
        return togglePostFavoriteByDatabase(post, userId);
    }

    private boolean syncPostFavoriteByCacheResult(Post post, Long userId, PostInteractionCacheService.ToggleResult result) {
        Long postId = post.getId();
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getPostId, postId)
                .eq(Favorite::getUserId, userId);

        if (result.isActive()) {
            if (favoriteMapper.selectCount(wrapper) == 0) {
                favoriteMapper.insert(Favorite.builder().postId(postId).userId(userId).build());
            }
            updatePostFavoriteCount(post, result.getCount());
            postCacheInvalidationEventPublisher.publishEvictDetail(postId);
            notificationEventPublisher.publish(post.getUserId(), userId, "post_favorite", postId, null, "favorited your post");
            return true;
        }

        favoriteMapper.delete(wrapper);
        updatePostFavoriteCount(post, result.getCount());
        postCacheInvalidationEventPublisher.publishEvictDetail(postId);
        return false;
    }

    private boolean togglePostFavoriteByDatabase(Post post, Long userId) {
        Long postId = post.getId();
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getPostId, postId)
                .eq(Favorite::getUserId, userId);

        Favorite exist = favoriteMapper.selectOne(wrapper);
        if (exist == null) {
            favoriteMapper.insert(Favorite.builder().postId(postId).userId(userId).build());
            post.setFavoriteCount(post.getFavoriteCount() + 1);
            postMapper.updateById(post);
            postCacheInvalidationEventPublisher.publishEvictDetail(postId);
            notificationEventPublisher.publish(post.getUserId(), userId, "post_favorite", postId, null, "favorited your post");
            return true;
        }

        favoriteMapper.delete(wrapper);
        post.setFavoriteCount(Math.max(0, post.getFavoriteCount() - 1));
        postMapper.updateById(post);
        postCacheInvalidationEventPublisher.publishEvictDetail(postId);
        return false;
    }

    private void ensurePostLikeCacheInitialized(Long postId) {
        if (postInteractionCacheService.isPostLikeInitialized(postId)) {
            return;
        }

        // Redis Set stores users who liked this post. The first access rebuilds it from MySQL.
        List<Long> userIds = postLikeMapper.selectList(new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, postId))
                .stream()
                .map(PostLike::getUserId)
                .collect(Collectors.toList());
        postInteractionCacheService.initializePostLikeUsers(postId, userIds);
    }

    private void ensurePostFavoriteCacheInitialized(Long postId) {
        if (postInteractionCacheService.isPostFavoriteInitialized(postId)) {
            return;
        }

        // Favorites use the same Set model as likes, so toggle and count stay O(1) in Redis.
        List<Long> userIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getPostId, postId))
                .stream()
                .map(Favorite::getUserId)
                .collect(Collectors.toList());
        postInteractionCacheService.initializePostFavoriteUsers(postId, userIds);
    }

    private void updatePostLikeCount(Post post, long count) {
        post.setLikeCount((int) Math.max(0, count));
        postMapper.updateById(post);
    }

    private void updatePostFavoriteCount(Post post, long count) {
        post.setFavoriteCount((int) Math.max(0, count));
        postMapper.updateById(post);
    }

    private Post checkPost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BaseException("Post does not exist or is hidden");
        }
        return post;
    }

    private Comment checkComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BaseException("Comment does not exist");
        }
        return comment;
    }
}
