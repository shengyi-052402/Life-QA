package com.forum.server.service.distribution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.PostTag;
import com.forum.server.mapper.PostMapper;
import com.forum.server.mapper.PostTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Explainable recommendation ordered by interest, current hotness and recency. */
@Service
@RequiredArgsConstructor
public class ContentRecommendationService {
    private static final int CANDIDATE_LIMIT = 500;

    private final UserInterestService userInterestService;
    private final ContentRankingService contentRankingService;
    private final PostMapper postMapper;
    private final PostTagMapper postTagMapper;

    public List<Long> recommend(Long userId, Long requestedCategoryId) {
        if (userId == null) return fallbackHot(null, requestedCategoryId);
        UserInterestService.InterestProfile profile = userInterestService.getProfile(userId);
        if (profile.isEmpty()) return fallbackHot(userId, requestedCategoryId);

        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .ne(Post::getUserId, userId)
                .orderByDesc(Post::getCreatedAt)
                .last("LIMIT " + CANDIDATE_LIMIT);
        if (requestedCategoryId != null) query.eq(Post::getCategoryId, requestedCategoryId);
        List<Post> candidates = new ArrayList<>(postMapper.selectList(query));
        if (candidates.isEmpty()) return Collections.emptyList();

        List<Long> postIds = candidates.stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, Set<Long>> tagsByPost = new HashMap<>();
        for (PostTag relation : postTagMapper.selectList(new LambdaQueryWrapper<PostTag>().in(PostTag::getPostId, postIds))) {
            tagsByPost.computeIfAbsent(relation.getPostId(), ignored -> new HashSet<>()).add(relation.getTagId());
        }

        candidates.sort(Comparator
                .comparingDouble((Post post) -> interestScore(post, profile,
                        tagsByPost.getOrDefault(post.getId(), Collections.emptySet()))).reversed()
                .thenComparing(Comparator.comparingDouble(contentRankingService::hotScore).reversed())
                .thenComparing(Post::getCreatedAt, Comparator.reverseOrder()));
        return candidates.stream().map(Post::getId).collect(Collectors.toList());
    }

    private List<Long> fallbackHot(Long userId, Long categoryId) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .and(item -> item.eq(Post::getIsTop, 1).or()
                        .ge(Post::getCreatedAt, LocalDateTime.now().minusHours(24)));
        if (userId != null) query.ne(Post::getUserId, userId);
        if (categoryId != null) query.eq(Post::getCategoryId, categoryId);
        List<Post> posts = new ArrayList<>(postMapper.selectList(query));

        List<Long> rankedIds = contentRankingService.getAllHotPostIds(categoryId);
        Map<Long, Integer> rank = new HashMap<>();
        for (int i = 0; i < rankedIds.size(); i++) rank.put(rankedIds.get(i), i);
        posts.sort(Comparator
                .comparing((Post post) -> post.getIsTop() != null && post.getIsTop() == 1).reversed()
                .thenComparingInt(post -> rank.getOrDefault(post.getId(), Integer.MAX_VALUE))
                .thenComparing(Comparator.comparingDouble(contentRankingService::hotScore).reversed())
                .thenComparing(Post::getCreatedAt, Comparator.reverseOrder()));
        return posts.stream().limit(CANDIDATE_LIMIT).map(Post::getId).collect(Collectors.toList());
    }

    private double interestScore(Post post, UserInterestService.InterestProfile profile, Set<Long> postTags) {
        double score = profile.categoryScores().getOrDefault(post.getCategoryId(), 0.0);
        for (Long tagId : postTags) score += profile.tagScores().getOrDefault(tagId, 0.0);
        return score;
    }
}
