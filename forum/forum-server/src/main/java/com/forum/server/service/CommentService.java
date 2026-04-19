package com.forum.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forum.common.result.PageResult;
import com.forum.pojo.dto.CommentCreateDTO;
import com.forum.pojo.dto.CommentPageQueryDTO;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.vo.CommentVO;

public interface CommentService extends IService<Comment> {

    /**
     * 发表评论
     */
    Long createComment(CommentCreateDTO dto);

    /**
     * 删除评论
     */
    void deleteComment(Long id);

    /**
     * 分页查询评论树
     */
    PageResult<CommentVO> getCommentPage(CommentPageQueryDTO dto);
}
