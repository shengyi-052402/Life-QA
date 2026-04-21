package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forum.common.constant.MessageConstant;
import com.forum.common.context.BaseContext;
import com.forum.common.exception.BaseException;
import com.forum.common.exception.ForbiddenException;
import com.forum.common.result.PageResult;
import com.forum.pojo.dto.PostCreateDTO;
import com.forum.pojo.dto.PostPageQueryDTO;
import com.forum.pojo.dto.PostUpdateDTO;
import com.forum.pojo.entity.Category;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.entity.Favorite;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.PostLike;
import com.forum.pojo.entity.PostTag;
import com.forum.pojo.entity.Tag;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.PostDetailVO;
import com.forum.pojo.vo.PostListVO;
import com.forum.pojo.vo.TagVO;
import com.forum.pojo.vo.UserVO;
import com.forum.server.mapper.CommentMapper;
import com.forum.server.mapper.FavoriteMapper;
import com.forum.server.mapper.PostLikeMapper;
import com.forum.server.mapper.PostMapper;
import com.forum.server.mapper.PostTagMapper;
import com.forum.server.service.CategoryService;
import com.forum.server.service.PostService;
import com.forum.server.service.TagService;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final TagService tagService;
    private final PostTagMapper postTagMapper;
    private final UserService userService;
    private final CategoryService categoryService;
    private final PostLikeMapper postLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(PostCreateDTO dto) {
        Long userId = BaseContext.getCurrentId();

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .summary(dto.getSummary())
                .coverImage(dto.getCoverImage())
                .userId(userId)
                .categoryId(dto.getCategoryId())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .favoriteCount(0)
                .isTop(0)
                .isEssence(0)
                .status(1)
                .build();

        save(post);
        Long postId = post.getId();

        User user = userService.getById(userId);
        if (user != null) {
            user.setPostCount((user.getPostCount() == null ? 0 : user.getPostCount()) + 1);
            userService.updateById(user);
        }

        Category category = categoryService.getById(dto.getCategoryId());
        if (category != null) {
            category.setPostCount((category.getPostCount() == null ? 0 : category.getPostCount()) + 1);
            categoryService.updateById(category);
        }

        handleTags(postId, dto.getTagIds(), dto.getNewTags());
        return postId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(PostUpdateDTO dto) {
        Post post = getById(dto.getId());
        if (post == null) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }

        Long userId = BaseContext.getCurrentId();
        User currentUser = userService.getById(userId);
        if (!post.getUserId().equals(userId) && currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        if (!post.getCategoryId().equals(dto.getCategoryId())) {
            Category oldCategory = categoryService.getById(post.getCategoryId());
            if (oldCategory != null) {
                oldCategory.setPostCount(Math.max(0, oldCategory.getPostCount() - 1));
                categoryService.updateById(oldCategory);
            }

            Category newCategory = categoryService.getById(dto.getCategoryId());
            if (newCategory != null) {
                newCategory.setPostCount((newCategory.getPostCount() == null ? 0 : newCategory.getPostCount()) + 1);
                categoryService.updateById(newCategory);
            }
        }

        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setSummary(dto.getSummary());
        post.setCoverImage(dto.getCoverImage());
        post.setCategoryId(dto.getCategoryId());
        updateById(post);

        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, post.getId()));
        handleTags(post.getId(), dto.getTagIds(), dto.getNewTags());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        Post post = getById(id);
        if (post == null) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }

        Long userId = BaseContext.getCurrentId();
        User currentUser = userService.getById(userId);
        if (!post.getUserId().equals(userId) && currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        User author = userService.getById(post.getUserId());
        if (author != null) {
            author.setPostCount(Math.max(0, (author.getPostCount() == null ? 0 : author.getPostCount()) - 1));
            userService.updateById(author);
        }

        Category category = categoryService.getById(post.getCategoryId());
        if (category != null) {
            category.setPostCount(Math.max(0, (category.getPostCount() == null ? 0 : category.getPostCount()) - 1));
            categoryService.updateById(category);
        }

        removeById(id);
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, id));
        postLikeMapper.delete(new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, id));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, id));
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, id));
    }

    @Override
    public PostDetailVO getPostDetail(Long id) {
        Post post = getById(id);
        if (post == null || post.getStatus() != 1) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }

        baseMapper.incrementViewCount(id);
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);

        PostDetailVO vo = PostDetailVO.builder().build();
        BeanUtils.copyProperties(post, vo);
        vo.setIsTop(post.getIsTop() == 1);
        vo.setIsEssence(post.getIsEssence() == 1);

        User user = userService.getById(post.getUserId());
        if (user != null) {
            vo.setAuthor(UserVO.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .build());
        }

        Category category = categoryService.getById(post.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        List<PostTag> postTags = postTagMapper.selectList(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, id));
        if (!postTags.isEmpty()) {
            List<Long> tagIds = postTags.stream().map(PostTag::getTagId).collect(Collectors.toList());
            List<Tag> tags = tagService.listByIds(tagIds);
            List<TagVO> tagVOs = tags.stream().map(tag -> {
                TagVO voItem = TagVO.builder().build();
                BeanUtils.copyProperties(tag, voItem);
                return voItem;
            }).collect(Collectors.toList());
            vo.setTags(tagVOs);
        } else {
            vo.setTags(new ArrayList<>());
        }

        vo.setIsLiked(false);
        vo.setIsFavorited(false);

        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId != null) {
            long liked = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                    .eq(PostLike::getPostId, id)
                    .eq(PostLike::getUserId, currentUserId));
            vo.setIsLiked(liked > 0);

            long favorited = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getPostId, id)
                    .eq(Favorite::getUserId, currentUserId));
            vo.setIsFavorited(favorited > 0);
        }

        return vo;
    }

    @Override
    public PageResult<PostListVO> getPostPage(PostPageQueryDTO queryDTO) {
        Page<Post> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1);

        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Post::getCategoryId, queryDTO.getCategoryId());
        }

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(wq -> wq.like(Post::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(Post::getSummary, queryDTO.getKeyword()));
        }

        if ("hot".equals(queryDTO.getSort())) {
            wrapper.orderByDesc(Post::getViewCount);
        } else if ("most_liked".equals(queryDTO.getSort())) {
            wrapper.orderByDesc(Post::getLikeCount);
        } else if ("most_commented".equals(queryDTO.getSort())) {
            wrapper.orderByDesc(Post::getCommentCount);
        } else {
            wrapper.orderByDesc(Post::getIsTop).orderByDesc(Post::getCreatedAt);
        }

        page(pageParam, wrapper);

        List<PostListVO> records = pageParam.getRecords().stream().map(post -> {
            PostListVO vo = PostListVO.builder().build();
            BeanUtils.copyProperties(post, vo);
            vo.setIsTop(post.getIsTop() == 1);
            vo.setIsEssence(post.getIsEssence() == 1);

            User user = userService.getById(post.getUserId());
            if (user != null) {
                vo.setAuthor(UserVO.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .build());
            }

            Category category = categoryService.getById(post.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(pageParam.getTotal(), records);
    }

    private void handleTags(Long postId, List<Long> tagIds, List<String> newTags) {
        if (tagIds == null) {
            tagIds = new ArrayList<>();
        }

        if (newTags != null && !newTags.isEmpty()) {
            for (String tagName : newTags) {
                if (!StringUtils.hasText(tagName)) {
                    continue;
                }

                Tag tag = tagService.getOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagName));
                if (tag == null) {
                    tag = Tag.builder().name(tagName).postCount(0).build();
                    tagService.save(tag);
                }
                tagIds.add(tag.getId());
            }
        }

        tagIds = tagIds.stream().distinct().collect(Collectors.toList());
        for (Long tagId : tagIds) {
            postTagMapper.insert(PostTag.builder().postId(postId).tagId(tagId).build());
            Tag tag = tagService.getById(tagId);
            if (tag != null) {
                tag.setPostCount((tag.getPostCount() == null ? 0 : tag.getPostCount()) + 1);
                tagService.updateById(tag);
            }
        }
    }
}
