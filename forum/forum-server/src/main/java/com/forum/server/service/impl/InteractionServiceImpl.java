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
import com.forum.server.service.InteractionService;
import com.forum.server.service.NotificationService;
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
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean togglePostLike(Long postId) {
        Long userId = BaseContext.getCurrentId();
        Post post = checkPost(postId);

        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId);

        PostLike exist = postLikeMapper.selectOne(wrapper);
        if (exist == null) {
            postLikeMapper.insert(PostLike.builder().postId(postId).userId(userId).build());
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            notificationService.createNotification(post.getUserId(), userId, "post_like", postId, null, "赞了你的帖子");
            return true;
        } else {
            postLikeMapper.delete(wrapper);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postMapper.updateById(post);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleCommentLike(Long commentId) {
        Long userId = BaseContext.getCurrentId();
        Comment comment = checkComment(commentId);

        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId);

        CommentLike exist = commentLikeMapper.selectOne(wrapper);
        if (exist == null) {
            commentLikeMapper.insert(CommentLike.builder().commentId(commentId).userId(userId).build());
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
            notificationService.createNotification(comment.getUserId(), userId, "comment_like", comment.getPostId(), commentId, "赞了你的评论");
            return true;
        } else {
            commentLikeMapper.delete(wrapper);
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentMapper.updateById(comment);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean togglePostFavorite(Long postId) {
        Long userId = BaseContext.getCurrentId();
        Post post = checkPost(postId);

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getPostId, postId)
                .eq(Favorite::getUserId, userId);

        Favorite exist = favoriteMapper.selectOne(wrapper);
        if (exist == null) {
            favoriteMapper.insert(Favorite.builder().postId(postId).userId(userId).build());
            post.setFavoriteCount(post.getFavoriteCount() + 1);
            postMapper.updateById(post);
            notificationService.createNotification(post.getUserId(), userId, "post_favorite", postId, null, "收藏了你的帖子");
            return true;
        } else {
            favoriteMapper.delete(wrapper);
            post.setFavoriteCount(Math.max(0, post.getFavoriteCount() - 1));
            postMapper.updateById(post);
            return false;
        }
    }

    private Post checkPost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BaseException("帖子不存在或已被隐藏");
        }
        return post;
    }

    private Comment checkComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BaseException("评论不存在");
        }
        return comment;
    }
}
