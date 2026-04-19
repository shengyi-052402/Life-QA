package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forum.common.constant.MessageConstant;
import com.forum.common.context.BaseContext;
import com.forum.common.exception.BaseException;
import com.forum.common.exception.ForbiddenException;
import com.forum.common.result.PageResult;
import com.forum.common.utils.HtmlUtil;
import com.forum.pojo.dto.CommentCreateDTO;
import com.forum.pojo.dto.CommentPageQueryDTO;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.entity.CommentLike;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.CommentVO;
import com.forum.pojo.vo.UserVO;
import com.forum.server.mapper.CommentLikeMapper;
import com.forum.server.mapper.CommentMapper;
import com.forum.server.mapper.PostMapper;
import com.forum.server.service.CommentService;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final UserService userService;
    private final PostMapper postMapper;
    private final CommentLikeMapper commentLikeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentCreateDTO dto) {
        Long userId = BaseContext.getCurrentId();
        
        // 校验帖子
        Post post = postMapper.selectById(dto.getPostId());
        if (post == null || post.getStatus() != 1) {
            throw new BaseException("帖子不存在或已被查封");
        }

        // 校验父评论
        if (dto.getParentId() != 0L) {
            Comment parentComment = getById(dto.getParentId());
            if (parentComment == null || parentComment.getStatus() != 1) {
                throw new BaseException("回复的评论不存在");
            }
            // 确保顶级关系正确 (B站模式：所有回复均挂在根级下，或者挂在其顶级评论下)
            // 简单处理：我们允许无限盖楼，但展示时展平关联到 parentId
        }

        // 防 XSS 净化评论内容 (仅保留纯文本或少数安全标签)
        String safeContent = HtmlUtil.clean(dto.getContent());

        Comment comment = Comment.builder()
                .postId(dto.getPostId())
                .parentId(dto.getParentId())
                .userId(userId)
                .replyToUserId(dto.getReplyToUserId())
                .content(safeContent)
                .likeCount(0)
                .status(1)
                .build();
        
        save(comment);

        // 更新帖子的评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        Long userId = BaseContext.getCurrentId();
        Comment comment = getById(id);
        if (comment == null || comment.getStatus() != 1) {
            throw new BaseException("评论不存在");
        }

        User currentUser = userService.getById(userId);
        // 仅作者、由于是B站模式，甚至可以允许帖子作者删评论（预留判断此处仅允许超管和自己删）
        if (!comment.getUserId().equals(userId) && currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        // 逻辑删除
        comment.setStatus(0);
        updateById(comment);

        // 如果是根评论，其子评论也应该不予显示（或者前端递归处理），这里帖子总评论数 -1
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null) {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            postMapper.updateById(post);
        }
    }

    @Override
    public PageResult<CommentVO> getCommentPage(CommentPageQueryDTO dto) {
        Long currentUserId = BaseContext.getCurrentId(); // 可能为null

        Page<Comment> pageParam = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, dto.getPostId())
               .eq(Comment::getParentId, dto.getParentId()) // 只查对应层级
               .eq(Comment::getStatus, 1);

        if ("hot".equals(dto.getSort())) {
            wrapper.orderByDesc(Comment::getLikeCount).orderByDesc(Comment::getCreatedAt);
        } else {
            wrapper.orderByDesc(Comment::getCreatedAt);
        }

        page(pageParam, wrapper);

        List<CommentVO> voList = pageParam.getRecords().stream().map(c -> {
            CommentVO vo = buildCommentVO(c, currentUserId);
            // 如果是查询顶级评论，并且需要带出前2条子回复预览
            if (dto.getParentId() == 0L) {
                // 查出该评论下的所有子回复数量
                long replyCount = count(new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getParentId, c.getId())
                        .eq(Comment::getStatus, 1));
                vo.setReplyCount((int) replyCount);

                if (replyCount > 0) {
                    List<Comment> recentReplies = baseMapper.selectRecentReplies(dto.getPostId(), c.getId(), 2);
                    List<CommentVO> replyVOs = recentReplies.stream()
                            .map(r -> buildCommentVO(r, currentUserId))
                            .collect(Collectors.toList());
                    vo.setReplies(replyVOs);
                } else {
                    vo.setReplies(new ArrayList<>());
                }
            } else {
                vo.setReplyCount(0);
                vo.setReplies(new ArrayList<>());
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(pageParam.getTotal(), voList);
    }

    /**
     * 辅助方法：将实体转VO并映射用户、点赞等状态
     */
    private CommentVO buildCommentVO(Comment comment, Long currentUserId) {
        CommentVO vo = CommentVO.builder().build();
        BeanUtils.copyProperties(comment, vo);
        
        // 作者
        User author = userService.getById(comment.getUserId());
        if (author != null) {
            vo.setAuthor(UserVO.builder()
                    .id(author.getId())
                    .nickname(author.getNickname())
                    .avatar(author.getAvatar())
                    .build());
        }

        // 被回复的人
        if (comment.getReplyToUserId() != null) {
            User replyUser = userService.getById(comment.getReplyToUserId());
            if (replyUser != null) {
                vo.setReplyToUser(UserVO.builder()
                        .id(replyUser.getId())
                        .nickname(replyUser.getNickname())
                        .avatar(replyUser.getAvatar())
                        .build());
            }
        }

        // 当前用户是否点赞
        vo.setIsLiked(false);
        if (currentUserId != null) {
            long count = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getCommentId, comment.getId())
                    .eq(CommentLike::getUserId, currentUserId));
            vo.setIsLiked(count > 0);
        }

        return vo;
    }
}
