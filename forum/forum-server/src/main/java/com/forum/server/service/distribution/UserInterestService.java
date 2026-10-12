package com.forum.server.service.distribution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forum.pojo.entity.Comment;
import com.forum.pojo.entity.Favorite;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.PostLike;
import com.forum.pojo.entity.PostTag;
import com.forum.server.mapper.CommentMapper;
import com.forum.server.mapper.FavoriteMapper;
import com.forum.server.mapper.PostLikeMapper;
import com.forum.server.mapper.PostMapper;
import com.forum.server.mapper.PostTagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds an explainable profile from the user's explicit interactions. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInterestService {
    private static final String TAG_KEY_PREFIX = "forum:distribution:interest:tag:";
    private static final String CATEGORY_KEY_PREFIX = "forum:distribution:interest:category:";
    private static final String PROFILE_MARKER_PREFIX = "forum:distribution:interest:ready:";
    private static final Duration TTL = Duration.ofDays(90);
    private static final double LIKE_WEIGHT = 2.0;
    private static final double COMMENT_WEIGHT = 3.0;
    private static final double FAVORITE_WEIGHT = 4.0;
    private static final DefaultRedisScript<Long> SAVE_PROFILE = new DefaultRedisScript<>("""
            local version = tonumber(redis.call('GET', KEYS[4]) or '0')
            if version > tonumber(ARGV[1]) then return 0 end
            redis.call('DEL', KEYS[1], KEYS[2])
            local n = tonumber(ARGV[3])
            local pos = 4
            for i = 1, n do
                redis.call('ZADD', KEYS[1], ARGV[pos + 1], ARGV[pos])
                pos = pos + 2
            end
            local m = tonumber(ARGV[pos])
            pos = pos + 1
            for i = 1, m do
                redis.call('ZADD', KEYS[2], ARGV[pos + 1], ARGV[pos])
                pos = pos + 2
            end
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            redis.call('EXPIRE', KEYS[2], ARGV[2])
            redis.call('SET', KEYS[3], '1', 'EX', ARGV[2])
            redis.call('SET', KEYS[4], ARGV[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final PostMapper postMapper;
    private final PostTagMapper postTagMapper;
    private final PostLikeMapper postLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final CommentMapper commentMapper;
    private final InteractionProjectionQueue projectionQueue;

    /**
     * 更新画像
     * @param userId
     * @param postId
     * @param weight
     */
    public void recordInteraction(Long userId, Long postId, double weight) {
        if (userId == null || postId == null || weight == 0) return;
        projectionQueue.user(userId);
    }

    public InterestProfile getProfile(Long userId) {
        if (userId == null) return InterestProfile.empty();
        try {
            boolean ready = Boolean.TRUE.equals(stringRedisTemplate.hasKey(markerKey(userId)));
            InterestProfile cached = readProfile(userId);
            if (ready || !cached.isEmpty()) return cached;
        } catch (RuntimeException ex) {
            log.warn("Read interest profile from Redis failed, userId={}", userId, ex);
        }
        // Read-side rebuilds return database data; only the durable worker writes Redis snapshots.
        projectionQueue.user(userId);
        return buildFromRecentInteractions(userId);
    }

    public List<Long> topTagIds(Long userId, int limit) {
        return topIds(getProfile(userId).tagScores(), limit);
    }

    public List<Long> topCategoryIds(Long userId, int limit) {
        return topIds(getProfile(userId).categoryScores(), limit);
    }

    private InterestProfile buildFromRecentInteractions(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(90);
        Map<Long, Double> weightsByPost = new HashMap<>();
        postLikeMapper.selectList(new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId).ge(PostLike::getCreatedAt, since))
                .forEach(item -> weightsByPost.merge(item.getPostId(), LIKE_WEIGHT, Double::sum));
        favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId).ge(Favorite::getCreatedAt, since))
                .forEach(item -> weightsByPost.merge(item.getPostId(), FAVORITE_WEIGHT, Double::sum));
        commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, userId).eq(Comment::getStatus, 1).ge(Comment::getCreatedAt, since))
                .forEach(item -> weightsByPost.merge(item.getPostId(), COMMENT_WEIGHT, Double::sum));

        if (weightsByPost.isEmpty()) {

            return InterestProfile.empty();
        }

        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .in(Post::getId, weightsByPost.keySet()).eq(Post::getStatus, 1));
        Map<Long, Double> categoryScores = new HashMap<>();
        Set<Long> activePostIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        for (Post post : posts) {
            if (post.getCategoryId() != null) categoryScores.merge(post.getCategoryId(), weightsByPost.get(post.getId()), Double::sum);
        }

        Map<Long, Double> tagScores = new HashMap<>();
        if (!activePostIds.isEmpty()) {
            for (PostTag relation : postTagMapper.selectList(new LambdaQueryWrapper<PostTag>().in(PostTag::getPostId, activePostIds))) {
                tagScores.merge(relation.getTagId(), weightsByPost.get(relation.getPostId()), Double::sum);
            }
        }
        InterestProfile profile = new InterestProfile(tagScores, categoryScores);

        return profile;
    }

    private InterestProfile readProfile(Long userId) {
        return new InterestProfile(readScores(tagKey(userId)), readScores(categoryKey(userId)));
    }

    private Map<Long, Double> readScores(String key) {
        Set<ZSetOperations.TypedTuple<String>> values = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);
        if (values == null || values.isEmpty()) return Collections.emptyMap();
        Map<Long, Double> scores = new HashMap<>();
        for (ZSetOperations.TypedTuple<String> value : values) {
            if (value.getValue() != null && value.getScore() != null && value.getScore() > 0) {
                scores.put(Long.valueOf(value.getValue()), value.getScore());
            }
        }
        return scores;
    }

    /**
     * 重建用户兴趣画像
     * @param userId
     * @param revision
     */
    public void rebuildStrict(Long userId, long revision) {
        InterestProfile profile = buildFromRecentInteractions(userId);
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(revision));
        args.add(String.valueOf(TTL.toSeconds()));
        args.add(String.valueOf(profile.tagScores().size()));
        profile.tagScores().forEach((id, score) -> {
            args.add(String.valueOf(id));
            args.add(String.valueOf(score));
        });
        args.add(String.valueOf(profile.categoryScores().size()));
        profile.categoryScores().forEach((id, score) -> {
            args.add(String.valueOf(id));
            args.add(String.valueOf(score));
        });
        Long result = stringRedisTemplate.execute(SAVE_PROFILE,
                List.of(tagKey(userId), categoryKey(userId), markerKey(userId),
                        "forum:distribution:interest:version:" + userId), args.toArray());
        if (result == null) throw new IllegalStateException("Redis did not acknowledge profile rebuild");
    }
    private List<Long> topIds(Map<Long, Double> scores, int limit) {
        if (limit <= 0 || scores.isEmpty()) return Collections.emptyList();
        return scores.entrySet().stream().sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit).map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
    }

    private String tagKey(Long userId) { return TAG_KEY_PREFIX + userId; }
    private String categoryKey(Long userId) { return CATEGORY_KEY_PREFIX + userId; }
    private String markerKey(Long userId) { return PROFILE_MARKER_PREFIX + userId; }

    public record InterestProfile(Map<Long, Double> tagScores, Map<Long, Double> categoryScores) {
        public static InterestProfile empty() { return new InterestProfile(Collections.emptyMap(), Collections.emptyMap()); }
        public boolean isEmpty() { return tagScores.isEmpty() && categoryScores.isEmpty(); }
    }
}
