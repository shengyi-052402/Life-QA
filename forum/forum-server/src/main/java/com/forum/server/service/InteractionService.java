package com.forum.server.service;

public interface InteractionService {
    
    /**
     * 点赞/取消点赞 帖子
     * @return true表示点赞成功，false表示取消点赞
     */
    boolean togglePostLike(Long postId);

    /**
     * 点赞/取消点赞 评论
     */
    boolean toggleCommentLike(Long commentId);

    /**
     * 收藏/取消收藏 帖子
     */
    boolean togglePostFavorite(Long postId);
}
