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
import com.forum.pojo.dto.AdminCommentPageQueryDTO;
import com.forum.pojo.dto.CommentCreateDTO;
import com.forum.pojo.dto.CommentPageQueryDTO;
import com.forum.pojo.vo.AdminCommentVO;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.entity.CommentLike;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.CommentLocationVO;
import com.forum.pojo.vo.CommentVO;
import com.forum.pojo.vo.UserVO;
import com.forum.server.mapper.CommentLikeMapper;
import com.forum.server.mapper.CommentMapper;
import com.forum.server.mapper.PostMapper;
import com.forum.server.messaging.NotificationEventPublisher;
import com.forum.server.messaging.PostCacheInvalidationEventPublisher;
import com.forum.server.service.CommentService;
import com.forum.server.service.SearchService;
import com.forum.server.service.UserService;
import com.forum.server.service.distribution.ContentRankingService;
import com.forum.server.service.distribution.UserInterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final UserService userService;
    private final PostMapper postMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final NotificationEventPublisher notificationEventPublisher;
    private final SearchService searchService;
    private final PostCacheInvalidationEventPublisher postCacheInvalidationEventPublisher;
    private final ContentRankingService contentRankingService;
    private final UserInterestService userInterestService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentCreateDTO dto) {
        Long userId = BaseContext.getCurrentId();

        Post post = postMapper.selectById(dto.getPostId());
        if (post == null || post.getStatus() != 1) {
            throw new BaseException("帖子不存在或已被隐藏");
        }

        if (dto.getParentId() != 0L) {
            Comment parentComment = getById(dto.getParentId());
            if (parentComment == null || parentComment.getStatus() != 1) {
                throw new BaseException("回复的评论不存在");
            }
        }

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

        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);
        postCacheInvalidationEventPublisher.publishEvictDetail(dto.getPostId());
        contentRankingService.refreshPost(dto.getPostId());
        userInterestService.recordInteraction(userId, dto.getPostId(), 3.0);

        if (dto.getParentId() != 0L && dto.getReplyToUserId() != null) {
            notificationEventPublisher.publish(
                    dto.getReplyToUserId(),
                    userId,
                    "comment_reply",
                    dto.getPostId(),
                    comment.getId(),
                    "回复了你的评论"
            );
        } else {
            notificationEventPublisher.publish(
                    post.getUserId(),
                    userId,
                    "post_comment",
                    dto.getPostId(),
                    comment.getId(),
                    "评论了你的帖子"
            );
        }

        searchService.syncPost(dto.getPostId());
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
        if (!comment.getUserId().equals(userId) && currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        comment.setStatus(0);
        updateById(comment);

        Post post = postMapper.selectById(comment.getPostId());
        if (post != null) {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            postMapper.updateById(post);
            postCacheInvalidationEventPublisher.publishEvictDetail(post.getId());
            searchService.syncPost(post.getId());
            contentRankingService.refreshPost(post.getId());
            userInterestService.recordInteraction(comment.getUserId(), post.getId(), -3.0);
        }
    }

    @Override
    public PageResult<CommentVO> getCommentPage(CommentPageQueryDTO dto) {
        Long currentUserId = BaseContext.getCurrentId();

        Page<Comment> pageParam = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, dto.getPostId())
                .eq(Comment::getParentId, dto.getParentId())
                .eq(Comment::getStatus, 1);

        if ("hot".equals(dto.getSort())) {
            wrapper.orderByDesc(Comment::getLikeCount).orderByDesc(Comment::getCreatedAt);
        } else {
            wrapper.orderByDesc(Comment::getCreatedAt);
        }

        page(pageParam, wrapper);

        List<CommentVO> voList = pageParam.getRecords().stream().map(comment -> {
            CommentVO vo = buildCommentVO(comment, currentUserId);
            if (dto.getParentId() == 0L) {
                long replyCount = count(new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getParentId, comment.getId())
                        .eq(Comment::getStatus, 1));
                vo.setReplyCount((int) replyCount);

                if (replyCount > 0) {
                    List<Comment> recentReplies = baseMapper.selectRecentReplies(dto.getPostId(), comment.getId(), 2);
                    List<CommentVO> replyVOs = recentReplies.stream()
                            .map(reply -> buildCommentVO(reply, currentUserId))
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

    @Override
    public PageResult<AdminCommentVO> getAdminCommentPage(AdminCommentPageQueryDTO dto) {
        ensureAdmin();

        Page<Comment> pageParam = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();

        if (dto.getPostId() != null) {
            wrapper.eq(Comment::getPostId, dto.getPostId());
        }
        if (dto.getUserId() != null) {
            wrapper.eq(Comment::getUserId, dto.getUserId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(Comment::getStatus, dto.getStatus());
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.like(Comment::getContent, dto.getKeyword());
        }
        wrapper.orderByDesc(Comment::getCreatedAt);
        page(pageParam, wrapper);

        List<AdminCommentVO> records = pageParam.getRecords().stream()
                .map(this::buildAdminCommentVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageParam.getTotal(), records);
    }

    @Override
    public CommentLocationVO getCommentLocation(Long commentId, Integer topSize, Integer replySize) {
        Comment comment = getById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BaseException("评论不存在");
        }

        int safeTopSize = Math.max(topSize, 1);
        int safeReplySize = Math.max(replySize, 1);
        Comment rootComment = comment.getParentId() == 0 ? comment : getById(comment.getParentId());
        if (rootComment == null || rootComment.getStatus() != 1) {
            throw new BaseException("评论不存在");
        }

        int topPage = calculatePage(rootComment.getPostId(), 0L, rootComment.getCreatedAt(), safeTopSize);
        int replyPage = comment.getParentId() == 0
                ? 1
                : calculatePage(rootComment.getPostId(), rootComment.getId(), comment.getCreatedAt(), safeReplySize);

        return CommentLocationVO.builder()
                .postId(comment.getPostId())
                .rootCommentId(rootComment.getId())
                .commentId(comment.getId())
                .topPage(topPage)
                .replyPage(replyPage)
                .build();
    }

    private int calculatePage(Long postId, Long parentId, LocalDateTime createdAt, int pageSize) {
        long beforeCount = count(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getParentId, parentId)
                .eq(Comment::getStatus, 1)
                .gt(Comment::getCreatedAt, createdAt));
        return (int) (beforeCount / pageSize) + 1;
    }

    private CommentVO buildCommentVO(Comment comment, Long currentUserId) {
        CommentVO vo = CommentVO.builder().build();
        BeanUtils.copyProperties(comment, vo);

        User author = userService.getById(comment.getUserId());
        if (author != null) {
            vo.setAuthor(UserVO.builder()
                    .id(author.getId())
                    .nickname(author.getNickname())
                    .avatar(author.getAvatar())
                    .build());
        }

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

        vo.setIsLiked(false);
        if (currentUserId != null) {
            long count = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getCommentId, comment.getId())
                    .eq(CommentLike::getUserId, currentUserId));
            vo.setIsLiked(count > 0);
        }

        return vo;
    }

    private AdminCommentVO buildAdminCommentVO(Comment comment) {
        User author = userService.getById(comment.getUserId());
        User replyUser = comment.getReplyToUserId() == null ? null : userService.getById(comment.getReplyToUserId());
        Post post = postMapper.selectById(comment.getPostId());

        return AdminCommentVO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .postId(comment.getPostId())
                .postTitle(post != null ? post.getTitle() : null)
                .parentId(comment.getParentId())
                .likeCount(comment.getLikeCount())
                .author(author == null ? null : UserVO.builder()
                        .id(author.getId())
                        .username(author.getUsername())
                        .nickname(author.getNickname())
                        .avatar(author.getAvatar())
                        .build())
                .replyToUser(replyUser == null ? null : UserVO.builder()
                        .id(replyUser.getId())
                        .username(replyUser.getUsername())
                        .nickname(replyUser.getNickname())
                        .avatar(replyUser.getAvatar())
                        .build())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private void ensureAdmin() {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }
    }
}
