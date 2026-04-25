package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forum.common.result.PageResult;
import com.forum.common.utils.HtmlUtil;
import com.forum.pojo.entity.Category;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.PostTag;
import com.forum.pojo.entity.Tag;
import com.forum.pojo.entity.User;
import com.forum.pojo.es.PostDocument;
import com.forum.pojo.vo.PostListVO;
import com.forum.pojo.vo.TagVO;
import com.forum.pojo.vo.UserVO;
import com.forum.server.mapper.PostMapper;
import com.forum.server.mapper.PostTagMapper;
import com.forum.server.service.CategoryService;
import com.forum.server.service.TagService;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchSupport {
    private static final String HIGHLIGHT_PRE_TAG = "<em>";
    private static final String HIGHLIGHT_POST_TAG = "</em>";

    private final PostMapper postMapper;
    private final PostTagMapper postTagMapper;
    private final UserService userService;
    private final CategoryService categoryService;
    private final TagService tagService;

    public PageResult<PostListVO> searchFromDatabase(String keyword, Integer page, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.max(size == null ? 15 : size, 1);

        List<Long> matchedTagIds = tagService.list(new LambdaQueryWrapper<Tag>()
                        .like(Tag::getName, keyword))
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

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .and(q -> {
                    q.like(Post::getTitle, keyword)
                            .or()
                            .like(Post::getSummary, keyword)
                            .or()
                            .like(Post::getContent, keyword);
                    if (!postIdsByTag.isEmpty()) {
                        q.or().in(Post::getId, postIdsByTag);
                    }
                })
                .orderByDesc(Post::getIsTop)
                .orderByDesc(Post::getCreatedAt);

        List<Post> allPosts = postMapper.selectList(wrapper);
        int fromIndex = Math.min((safePage - 1) * safeSize, allPosts.size());
        int toIndex = Math.min(fromIndex + safeSize, allPosts.size());
        List<PostListVO> records = allPosts.subList(fromIndex, toIndex).stream()
                .map(post -> {
                    PostListVO vo = buildPostListVO(post);
                    vo.setTitleHighlight(highlightText(post.getTitle(), keyword));
                    vo.setSummaryHighlight(buildSummaryHighlight(post, keyword, vo.getTags()));
                    return vo;
                })
                .collect(Collectors.toList());

        return new PageResult<>((long) allPosts.size(), records);
    }

    public List<String> suggestFromDatabase(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        postMapper.selectList(new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .and(q -> q.likeRight(Post::getTitle, keyword).or().like(Post::getTitle, keyword))
                        .orderByDesc(Post::getCreatedAt)
                        .last("LIMIT " + limit))
                .stream()
                .map(Post::getTitle)
                .filter(StringUtils::hasText)
                .forEach(suggestions::add);

        tagService.list(new LambdaQueryWrapper<Tag>()
                        .and(q -> q.likeRight(Tag::getName, keyword).or().like(Tag::getName, keyword))
                        .orderByDesc(Tag::getPostCount)
                        .last("LIMIT " + limit))
                .stream()
                .map(Tag::getName)
                .filter(StringUtils::hasText)
                .forEach(suggestions::add);

        return suggestions.stream().limit(limit).collect(Collectors.toList());
    }

    public PostDocument buildDocument(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            return null;
        }

        List<TagVO> tags = getTagVOsByPostId(postId);
        User author = userService.getById(post.getUserId());
        Category category = categoryService.getById(post.getCategoryId());

        return PostDocument.builder()
                .id(post.getId())
                .title(post.getTitle())
                .summary(StringUtils.hasText(post.getSummary()) ? post.getSummary() : HtmlUtil.getSummary(post.getContent(), 180))
                .content(HtmlUtil.removeHtmlTags(post.getContent()))
                .tagNames(tags.stream().map(TagVO::getName).filter(StringUtils::hasText).collect(Collectors.toList()))
                .authorName(author == null ? null : (StringUtils.hasText(author.getNickname()) ? author.getNickname() : author.getUsername()))
                .categoryName(category == null ? null : category.getName())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public List<PostDocument> buildAllDocuments() {
        return postMapper.selectList(new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getCreatedAt))
                .stream()
                .map(Post::getId)
                .map(this::buildDocument)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<PostListVO> buildSearchResultVOs(List<Long> orderedPostIds, Map<Long, Map<String, List<String>>> highlightMap, String keyword) {
        if (orderedPostIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Post> postMap = postMapper.selectBatchIds(orderedPostIds).stream()
                .filter(post -> post.getStatus() != null && post.getStatus() == 1)
                .collect(Collectors.toMap(Post::getId, post -> post));

        List<PostListVO> records = new ArrayList<>();
        for (Long postId : orderedPostIds) {
            Post post = postMap.get(postId);
            if (post == null) {
                continue;
            }

            PostListVO vo = buildPostListVO(post);
            Map<String, List<String>> highlights = highlightMap.getOrDefault(postId, Collections.emptyMap());
            vo.setTitleHighlight(firstHighlight(highlights, "title", post.getTitle(), keyword));
            String summaryHighlight = firstHighlight(highlights, "summary", post.getSummary(), keyword);
            String contentHighlight = firstHighlight(highlights, "content", null, keyword);
            String tagHighlight = firstHighlight(highlights, "tagNames", null, keyword);

            if (StringUtils.hasText(summaryHighlight)) {
                vo.setSummaryHighlight(summaryHighlight);
            } else if (StringUtils.hasText(contentHighlight)) {
                vo.setSummaryHighlight(contentHighlight);
            } else if (StringUtils.hasText(tagHighlight)) {
                vo.setSummaryHighlight("匹配标签：" + tagHighlight);
            } else {
                vo.setSummaryHighlight(buildSummaryHighlight(post, keyword, vo.getTags()));
            }
            records.add(vo);
        }
        return records;
    }

    private PostListVO buildPostListVO(Post post) {
        PostListVO vo = PostListVO.builder().build();
        BeanUtils.copyProperties(post, vo);
        vo.setIsTop(post.getIsTop() != null && post.getIsTop() == 1);
        vo.setIsEssence(post.getIsEssence() != null && post.getIsEssence() == 1);
        vo.setTags(getTagVOsByPostId(post.getId()));

        User user = userService.getById(post.getUserId());
        if (user != null) {
            vo.setAuthor(UserVO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .build());
        }

        Category category = categoryService.getById(post.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        return vo;
    }

    private List<TagVO> getTagVOsByPostId(Long postId) {
        List<Long> tagIds = postTagMapper.selectList(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId))
                .stream()
                .map(PostTag::getTagId)
                .distinct()
                .collect(Collectors.toList());
        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }

        return tagService.listByIds(tagIds).stream().map(tag -> {
            TagVO vo = TagVO.builder().build();
            BeanUtils.copyProperties(tag, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private String firstHighlight(Map<String, List<String>> highlights, String fieldName, String fallback, String keyword) {
        List<String> fragments = highlights.get(fieldName);
        if (fragments != null && !fragments.isEmpty()) {
            return fragments.get(0);
        }
        if (fieldName.equals("title")) {
            return highlightText(fallback, keyword);
        }
        return fallback;
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
