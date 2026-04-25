package com.forum.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forum.common.result.PageResult;
import com.forum.pojo.dto.AdminCommentPageQueryDTO;
import com.forum.pojo.dto.CommentCreateDTO;
import com.forum.pojo.dto.CommentPageQueryDTO;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.vo.AdminCommentVO;
import com.forum.pojo.vo.CommentLocationVO;
import com.forum.pojo.vo.CommentVO;

public interface CommentService extends IService<Comment> {

    Long createComment(CommentCreateDTO dto);

    void deleteComment(Long id);

    PageResult<CommentVO> getCommentPage(CommentPageQueryDTO dto);

    PageResult<AdminCommentVO> getAdminCommentPage(AdminCommentPageQueryDTO dto);

    CommentLocationVO getCommentLocation(Long commentId, Integer topSize, Integer replySize);
}
