package com.forum.server.service;

public interface InteractionService {
    
    /**
     * 设置帖子点赞状态；重复设置同一状态不会再次产生事件。
     * @return 本次操作完成后的个人状态
     */
    boolean setPostLike(Long postId, boolean active);

    /**
     * 点赞/取消点赞 评论
     */
    boolean setCommentLike(Long commentId, boolean active);

    /**
     * 收藏/取消收藏 帖子
     */
    boolean setPostFavorite(Long postId, boolean active);
}
