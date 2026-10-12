package com.forum.server.service.distribution;

import com.forum.pojo.entity.Post;
import com.forum.server.mapper.PostMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContentRankingServiceTest {

    private final ContentRankingService service = new ContentRankingService(
            mock(StringRedisTemplate.class), mock(PostMapper.class), mock(InteractionProjectionQueue.class));

    @Test
    void favoriteHasMoreHeatThanLikeAndTimeDecayReducesScore() {
        LocalDateTime now = LocalDateTime.now();
        Post liked = post(now, 0, 1, 0, 0);
        Post favorited = post(now, 0, 0, 0, 1);
        Post oldFavorite = post(now.minusHours(12), 0, 0, 0, 1);

        assertThat(service.hotScore(favorited)).isGreaterThan(service.hotScore(liked));
        assertThat(service.hotScore(favorited)).isGreaterThan(service.hotScore(oldFavorite));
    }

    private Post post(LocalDateTime createdAt, int views, int likes, int comments, int favorites) {
        return Post.builder().createdAt(createdAt).viewCount(views).likeCount(likes)
                .commentCount(comments).favoriteCount(favorites).build();
    }
}
