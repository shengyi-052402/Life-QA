package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forum.common.constant.MessageConstant;
import com.forum.common.context.BaseContext;
import com.forum.common.exception.ForbiddenException;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.StatVO;
import com.forum.server.service.AdminService;
import com.forum.server.service.CommentService;
import com.forum.server.service.PostService;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    @Override
    public void assertAdmin() {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        User currentUser = userService.getById(currentId);
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }
    }

    @Override
    public StatVO getStats() {
        assertAdmin();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return StatVO.builder()
                .totalUsers(userService.count())
                .totalPosts(postService.count())
                .totalComments(commentService.count())
                .todayNewUsers(userService.count(new LambdaQueryWrapper<User>()
                        .ge(User::getCreatedAt, todayStart)))
                .todayNewPosts(postService.count(new LambdaQueryWrapper<Post>()
                        .ge(Post::getCreatedAt, todayStart)))
                .todayNewComments(commentService.count(new LambdaQueryWrapper<Comment>()
                        .ge(Comment::getCreatedAt, todayStart)))
                .build();
    }
}
