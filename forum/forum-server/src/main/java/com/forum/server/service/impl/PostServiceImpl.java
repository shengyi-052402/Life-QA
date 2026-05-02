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
import com.forum.pojo.dto.AdminPostPageQueryDTO;
import com.forum.pojo.dto.AdminPostUpdateDTO;
import com.forum.pojo.dto.PostCreateDTO;
import com.forum.pojo.dto.PostPageQueryDTO;
import com.forum.pojo.dto.PostUpdateDTO;
import com.forum.pojo.vo.AdminPostVO;
import com.forum.pojo.entity.Category;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.entity.Favorite;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.PostLike;
import com.forum.pojo.entity.PostTag;
import com.forum.pojo.entity.Tag;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.PostDetailVO;
import com.forum.pojo.vo.PostGlobeVO;
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
import com.forum.server.service.SearchService;
import com.forum.server.service.TagService;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {
    private static final String HIGHLIGHT_PRE_TAG = "<em>";
    private static final String HIGHLIGHT_POST_TAG = "</em>";

    private final TagService tagService;
    private final PostTagMapper postTagMapper;
    private final UserService userService;
    private final CategoryService categoryService;
    private final SearchService searchService;
    private final PostLikeMapper postLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(PostCreateDTO dto) {
        Long userId = BaseContext.getCurrentId();

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(HtmlUtil.cleanRichText(dto.getContent()))
                .summary(dto.getSummary())
                .coverImage(dto.getCoverImage())
                .userId(userId)
                .categoryId(dto.getCategoryId())
                .locationName(dto.getLocationName())
                .address(dto.getAddress())
                .placeId(dto.getPlaceId())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
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
        searchService.syncPost(postId);
        return postId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(PostUpdateDTO dto) {
        Post post = getById(dto.getId());
        if (post == null) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }

        assertCanManagePost(post);
        List<Long> oldTagIds = getTagIdsByPostId(post.getId());

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
        post.setContent(HtmlUtil.cleanRichText(dto.getContent()));
        post.setSummary(dto.getSummary());
        post.setCoverImage(dto.getCoverImage());
        post.setCategoryId(dto.getCategoryId());
        post.setLocationName(dto.getLocationName());
        post.setAddress(dto.getAddress());
        post.setPlaceId(dto.getPlaceId());
        post.setLatitude(dto.getLatitude());
        post.setLongitude(dto.getLongitude());
        updateById(post);

        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, post.getId()));
        decrementTagCounts(oldTagIds);
        handleTags(post.getId(), dto.getTagIds(), dto.getNewTags());
        searchService.syncPost(post.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        Post post = getById(id);
        if (post == null) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }

        assertCanManagePost(post);

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

        List<Long> tagIds = getTagIdsByPostId(id);
        removeById(id);
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, id));
        decrementTagCounts(tagIds);
        postLikeMapper.delete(new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, id));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getPostId, id));
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, id));
        searchService.deletePost(id);
    }

    @Override
    public PostDetailVO getPostDetail(Long id) {
        Post post = getById(id);
        if (post == null || post.getStatus() != 1) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }

        baseMapper.incrementViewCount(id);
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        PostDetailVO vo = buildPostDetailVO(post);

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
    public PostDetailVO getPostEditDetail(Long id) {
        Post post = getById(id);
        if (post == null || post.getStatus() != 1) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }

        assertCanManagePost(post);
        return buildPostDetailVO(post);
    }

    @Override
    public PageResult<PostListVO> getPostPage(PostPageQueryDTO queryDTO) {
        Page<Post> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1);

        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Post::getCategoryId, queryDTO.getCategoryId());
        }

        if (queryDTO.getTagId() != null) {
            List<Long> postIds = postTagMapper.selectList(new LambdaQueryWrapper<PostTag>()
                            .eq(PostTag::getTagId, queryDTO.getTagId()))
                    .stream()
                    .map(PostTag::getPostId)
                    .distinct()
                    .collect(Collectors.toList());
            if (postIds.isEmpty()) {
                return new PageResult<>(0L, Collections.emptyList());
            }
            wrapper.in(Post::getId, postIds);
        }

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            List<Long> matchedTagIds = tagService.list(new LambdaQueryWrapper<Tag>()
                            .like(Tag::getName, queryDTO.getKeyword()))
                    .stream()
                    .map(Tag::getId)
                    .collect(Collectors.toList());
            List<Long> postIdsByTag = matchedTagIds.isEmpty()
                    ? Collections.emptyList()
                    : postTagMapper.selectList(new LambdaQueryWrapper<PostTag>().in(PostTag::getTagId, matchedTagIds))
                    .stream()
                    .map(PostTag::getPostId)
                    .distinct()
                    .collect(Collectors.toList());

            wrapper.and(wq -> {
                wq.like(Post::getTitle, queryDTO.getKeyword())
                        .or()
                        .like(Post::getSummary, queryDTO.getKeyword())
                        .or()
                        .like(Post::getContent, queryDTO.getKeyword());
                if (!postIdsByTag.isEmpty()) {
                    wq.or().in(Post::getId, postIdsByTag);
                }
            });
        }

        if ("hot".equals(queryDTO.getSort())) {
            wrapper.orderByDesc(Post::getIsTop).orderByDesc(Post::getViewCount);
        } else if ("most_liked".equals(queryDTO.getSort())) {
            wrapper.orderByDesc(Post::getIsTop).orderByDesc(Post::getLikeCount);
        } else if ("most_commented".equals(queryDTO.getSort())) {
            wrapper.orderByDesc(Post::getIsTop).orderByDesc(Post::getCommentCount);
        } else {
            wrapper.orderByDesc(Post::getIsTop).orderByDesc(Post::getCreatedAt);
        }

        page(pageParam, wrapper);

        List<PostListVO> records = pageParam.getRecords().stream().map(post -> {
            PostListVO vo = PostListVO.builder().build();
            BeanUtils.copyProperties(post, vo);
            vo.setIsTop(post.getIsTop() == 1);
            vo.setIsEssence(post.getIsEssence() == 1);
            vo.setTags(getTagVOsByPostId(post.getId()));

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

            if (StringUtils.hasText(queryDTO.getKeyword())) {
                vo.setTitleHighlight(highlightText(post.getTitle(), queryDTO.getKeyword()));
                vo.setSummaryHighlight(buildSummaryHighlight(post, queryDTO.getKeyword(), vo.getTags()));
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(pageParam.getTotal(), records);
    }

    @Override
    public PageResult<AdminPostVO> getAdminPostPage(AdminPostPageQueryDTO queryDTO) {
        assertAdmin();

        Page<Post> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Post::getCategoryId, queryDTO.getCategoryId());
        }
        if (queryDTO.getUserId() != null) {
            wrapper.eq(Post::getUserId, queryDTO.getUserId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Post::getStatus, queryDTO.getStatus());
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(q -> q.like(Post::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(Post::getSummary, queryDTO.getKeyword()));
        }
        wrapper.orderByDesc(Post::getIsTop).orderByDesc(Post::getCreatedAt);

        page(pageParam, wrapper);

        List<AdminPostVO> records = pageParam.getRecords().stream()
                .map(this::buildAdminPostVO)
                .collect(Collectors.toList());

        return new PageResult<>(pageParam.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdatePost(Long id, AdminPostUpdateDTO dto) {
        assertAdmin();

        if (dto.getStatus() == null && dto.getIsTop() == null && dto.getIsEssence() == null) {
            throw new BaseException("至少提供一个更新项");
        }

        Post post = getById(id);
        if (post == null) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }

        if (dto.getStatus() != null) {
            post.setStatus(dto.getStatus());
        }
        if (dto.getIsTop() != null) {
            post.setIsTop(dto.getIsTop());
        }
        if (dto.getIsEssence() != null) {
            post.setIsEssence(dto.getIsEssence());
        }
        updateById(post);
        if (post.getStatus() != null && post.getStatus() == 1) {
            searchService.syncPost(id);
        } else {
            searchService.deletePost(id);
        }
    }

    @Override
    public List<PostGlobeVO> getGlobePosts() {
        // 只查询有地理位置信息的帖子，最多返回 500 条
        Page<Post> page = new Page<>(1, 500);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .isNotNull(Post::getLatitude)
                .isNotNull(Post::getLongitude)
                .orderByDesc(Post::getCreatedAt);

        page(page, wrapper);

        return page.getRecords().stream().map(post -> {
            User author = userService.getById(post.getUserId());
            return PostGlobeVO.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .coverImage(post.getCoverImage())
                    .locationName(post.getLocationName())
                    .address(post.getAddress())
                    .placeId(post.getPlaceId())
                    .latitude(post.getLatitude())
                    .longitude(post.getLongitude())
                    .likeCount(post.getLikeCount())
                    .authorNickname(author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : "匿名")
                    .authorAvatar(author != null ? author.getAvatar() : null)
                    .build();
        }).collect(Collectors.toList());
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

    private void assertCanManagePost(Post post) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        User currentUser = userService.getById(userId);
        if (currentUser == null || (!post.getUserId().equals(userId) && currentUser.getRole() != 1)) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }
    }

    private void assertAdmin() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        User currentUser = userService.getById(userId);
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }
    }

    private PostDetailVO buildPostDetailVO(Post post) {
        PostDetailVO vo = PostDetailVO.builder().build();
        BeanUtils.copyProperties(post, vo);
        vo.setIsTop(post.getIsTop() == 1);
        vo.setIsEssence(post.getIsEssence() == 1);
        vo.setTags(getTagVOsByPostId(post.getId()));

        User user = userService.getById(post.getUserId());
        if (user != null) {
            vo.setAuthor(UserVO.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .bio(user.getBio())
                    .postCount(user.getPostCount())
                    .build());
        }

        Category category = categoryService.getById(post.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        return vo;
    }

    private List<Long> getTagIdsByPostId(Long postId) {
        return postTagMapper.selectList(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId))
                .stream()
                .map(PostTag::getTagId)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<TagVO> getTagVOsByPostId(Long postId) {
        List<Long> tagIds = getTagIdsByPostId(postId);
        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }

        return tagService.listByIds(tagIds).stream().map(tag -> {
            TagVO vo = TagVO.builder().build();
            BeanUtils.copyProperties(tag, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private void decrementTagCounts(List<Long> tagIds) {
        for (Long tagId : tagIds) {
            Tag tag = tagService.getById(tagId);
            if (tag != null) {
                tag.setPostCount(Math.max(0, (tag.getPostCount() == null ? 0 : tag.getPostCount()) - 1));
                tagService.updateById(tag);
            }
        }
    }

    private AdminPostVO buildAdminPostVO(Post post) {
        User author = userService.getById(post.getUserId());
        Category category = categoryService.getById(post.getCategoryId());
        return AdminPostVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .summary(post.getSummary())
                .status(post.getStatus())
                .isTop(post.getIsTop() != null && post.getIsTop() == 1)
                .isEssence(post.getIsEssence() != null && post.getIsEssence() == 1)
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .favoriteCount(post.getFavoriteCount())
                .categoryId(post.getCategoryId())
                .categoryName(category != null ? category.getName() : null)
                .author(author == null ? null : UserVO.builder()
                        .id(author.getId())
                        .username(author.getUsername())
                        .nickname(author.getNickname())
                        .avatar(author.getAvatar())
                        .role(author.getRole())
                        .build())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private String highlightText(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) {
            return text;
        }

        return Pattern.compile("(?i)" + Pattern.quote(keyword))
                .matcher(text)
                .replaceAll(matchResult -> HIGHLIGHT_PRE_TAG + matchResult.group() + HIGHLIGHT_POST_TAG);
    }

    private String buildSummaryHighlight(Post post, String keyword, List<TagVO> tags) {
        String summary = StringUtils.hasText(post.getSummary()) ? post.getSummary() : HtmlUtil.getSummary(post.getContent(), 160);
        if (StringUtils.hasText(summary) && summary.toLowerCase().contains(keyword.toLowerCase())) {
            return highlightText(summary, keyword);
        }

        String plainContent = HtmlUtil.removeHtmlTags(post.getContent());
        String excerpt = extractExcerpt(plainContent, keyword);
        if (StringUtils.hasText(excerpt)) {
            return highlightText(excerpt, keyword);
        }

        String matchedTags = tags.stream()
                .map(TagVO::getName)
                .filter(StringUtils::hasText)
                .filter(name -> name.toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.joining(" / "));
        if (StringUtils.hasText(matchedTags)) {
            return "匹配标签：" + highlightText(matchedTags, keyword);
        }

        return highlightText(summary, keyword);
    }

    private String extractExcerpt(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) {
            return null;
        }

        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        int index = lowerText.indexOf(lowerKeyword);
        if (index < 0) {
            return null;
        }

        int start = Math.max(0, index - 40);
        int end = Math.min(text.length(), index + keyword.length() + 80);
        String excerpt = text.substring(start, end).trim();
        if (start > 0) {
            excerpt = "..." + excerpt;
        }
        if (end < text.length()) {
            excerpt = excerpt + "...";
        }
        return excerpt;
    }
}
