package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forum.common.constant.MessageConstant;
import com.forum.common.context.BaseContext;
import com.forum.common.exception.BaseException;
import com.forum.common.exception.ForbiddenException;
import com.forum.common.result.PageResult;
import com.forum.pojo.dto.AdminUserPageQueryDTO;
import com.forum.pojo.dto.AdminUserUpdateDTO;
import com.forum.pojo.dto.PasswordUpdateDTO;
import com.forum.pojo.dto.UserRegisterDTO;
import com.forum.pojo.dto.UserUpdateDTO;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.entity.Favorite;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.AdminUserVO;
import com.forum.pojo.vo.PostListVO;
import com.forum.pojo.vo.UserActivityVO;
import com.forum.pojo.vo.UserVO;
import com.forum.server.mapper.CommentMapper;
import com.forum.server.mapper.FavoriteMapper;
import com.forum.server.mapper.PostMapper;
import com.forum.server.mapper.UserMapper;
import com.forum.server.service.CategoryService;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final PostMapper postMapper;
    private final FavoriteMapper favoriteMapper;
    private final CommentMapper commentMapper;
    private final CategoryService categoryService;

/**
 * 注册账号
*/
    @Override
    public void register(UserRegisterDTO registerDTO) {
        long countByUsername = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (countByUsername > 0) {
            throw new BaseException(MessageConstant.USERNAME_EXISTS);
        }

        long countByEmail = count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, registerDTO.getEmail()));
        if (countByEmail > 0) {
            throw new BaseException(MessageConstant.EMAIL_EXISTS);
        }

        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .nickname(registerDTO.getUsername())
                .role(0)
                .status(1)
                .postCount(0)
                .build();

        save(user);
    }

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Override
    public UserVO getUserProfile(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return buildUserVO(user);
    }

    @Override
    public void updateProfile(Long userId, UserUpdateDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        user.setNickname(dto.getNickname());
        user.setBio(dto.getBio());
        user.setAvatar(dto.getAvatar());
        updateById(user);
    }

    @Override
    public void updatePassword(Long userId, PasswordUpdateDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BaseException(MessageConstant.PASSWORD_ERROR);
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BaseException("新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(user);
    }

    @Override
    public PageResult<PostListVO> getUserPosts(Long userId, Integer page, Integer size) {
        ensureUserExists(userId);

        Page<Post> pageParam = new Page<>(page, size);
        postMapper.selectPage(pageParam, new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, 1)
                .orderByDesc(Post::getCreatedAt));

        List<PostListVO> records = pageParam.getRecords().stream()
                .map(this::buildPostListVO)
                .collect(Collectors.toList());
        return new PageResult<>(pageParam.getTotal(), records);
    }

    @Override
    public PageResult<PostListVO> getMyFavorites(Integer page, Integer size) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            throw new BaseException("请先登录");
        }

        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, currentUserId)
                .orderByDesc(Favorite::getCreatedAt));

        List<PostListVO> allRecords = favorites.stream()
                .map(Favorite::getPostId)
                .map(postMapper::selectById)
                .filter(Objects::nonNull)
                .filter(post -> post.getStatus() == 1)
                .map(this::buildPostListVO)
                .collect(Collectors.toList());

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, allRecords.size());
        int toIndex = Math.min(fromIndex + safeSize, allRecords.size());

        return new PageResult<>(
                (long) allRecords.size(),
                new ArrayList<>(allRecords.subList(fromIndex, toIndex))
        );
    }

    @Override
    public PageResult<UserActivityVO> getUserActivities(Long userId, Integer page, Integer size) {
        ensureUserExists(userId);

        List<UserActivityVO> activities = new ArrayList<>();

        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, 1));
        posts.forEach(post -> activities.add(UserActivityVO.builder()
                .type("post")
                .targetId(post.getId())
                .postId(post.getId())
                .postTitle(post.getTitle())
                .title(post.getTitle())
                .summary(post.getSummary())
                .createdAt(post.getCreatedAt())
                .build()));

        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .eq(Comment::getStatus, 1));

        if (!comments.isEmpty()) {
            List<Long> postIds = comments.stream()
                    .map(Comment::getPostId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, Post> postMap = postMapper.selectBatchIds(postIds).stream()
                    .filter(post -> post.getStatus() == 1)
                    .collect(Collectors.toMap(Post::getId, post -> post));

            comments.stream()
                    .filter(comment -> postMap.containsKey(comment.getPostId()))
                    .forEach(comment -> {
                        Post post = postMap.get(comment.getPostId());
                        activities.add(UserActivityVO.builder()
                                .type("comment")
                                .targetId(comment.getId())
                                .postId(post.getId())
                                .postTitle(post.getTitle())
                                .content(shortenText(comment.getContent(), 120))
                                .createdAt(comment.getCreatedAt())
                                .build());
                    });
        }

        activities.sort(Comparator.comparing(UserActivityVO::getCreatedAt).reversed());

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, activities.size());
        int toIndex = Math.min(fromIndex + safeSize, activities.size());

        return new PageResult<>(
                (long) activities.size(),
                new ArrayList<>(activities.subList(fromIndex, toIndex))
        );
    }

    @Override
    public PageResult<AdminUserVO> getAdminUserPage(AdminUserPageQueryDTO dto) {
        ensureAdmin();

        Page<User> pageParam = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(q -> q.like(User::getUsername, dto.getKeyword())
                    .or()
                    .like(User::getNickname, dto.getKeyword())
                    .or()
                    .like(User::getEmail, dto.getKeyword()));
        }
        if (dto.getRole() != null) {
            wrapper.eq(User::getRole, dto.getRole());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(User::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(User::getCreatedAt);

        page(pageParam, wrapper);

        List<AdminUserVO> records = pageParam.getRecords().stream()
                .map(user -> AdminUserVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .bio(user.getBio())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .postCount(user.getPostCount())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return new PageResult<>(pageParam.getTotal(), records);
    }

    @Override
    public void adminUpdateUser(Long id, AdminUserUpdateDTO dto) {
        ensureAdmin();

        if (dto.getRole() == null && dto.getStatus() == null) {
            throw new BaseException("至少提供一个更新项");
        }

        User user = getById(id);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId != null && currentUserId.equals(id) && dto.getStatus() != null && dto.getStatus() == 0) {
            throw new BaseException("不能禁用当前登录管理员");
        }

        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        updateById(user);
    }

    private void ensureUserExists(Long userId) {
        if (getById(userId) == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
    }

    private void ensureAdmin() {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        User currentUser = getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }
    }

    private UserVO buildUserVO(User user) {
        Integer postCount = Math.toIntExact(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, user.getId())
                .eq(Post::getStatus, 1)));

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .role(user.getRole())
                .postCount(postCount)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private PostListVO buildPostListVO(Post post) {
        PostListVO vo = PostListVO.builder().build();
        BeanUtils.copyProperties(post, vo);
        vo.setIsTop(post.getIsTop() != null && post.getIsTop() == 1);
        vo.setIsEssence(post.getIsEssence() != null && post.getIsEssence() == 1);

        User author = getById(post.getUserId());
        if (author != null) {
            vo.setAuthor(buildUserVO(author));
        }

        if (post.getCategoryId() != null) {
            var category = categoryService.getById(post.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        return vo;
    }

    private String shortenText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
