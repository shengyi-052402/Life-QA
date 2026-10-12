package com.forum.server.service.distribution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.PostTag;
import com.forum.server.mapper.PostMapper;
import com.forum.server.mapper.PostTagMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentRecommendationServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void interestMatchWinsBeforeHotness() {
        UserInterestService interests = mock(UserInterestService.class);
        ContentRankingService ranking = mock(ContentRankingService.class);
        PostMapper posts = mock(PostMapper.class);
        PostTagMapper tags = mock(PostTagMapper.class);
        ContentRecommendationService service = new ContentRecommendationService(interests, ranking, posts, tags);

        Post preferred = Post.builder().id(1L).userId(2L).categoryId(10L).status(1)
                .createdAt(LocalDateTime.now()).build();
        Post merelyHot = Post.builder().id(2L).userId(3L).categoryId(20L).status(1)
                .createdAt(LocalDateTime.now()).build();
        when(interests.getProfile(99L)).thenReturn(new UserInterestService.InterestProfile(Map.of(100L, 8.0), Map.of()));
        when(posts.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(preferred, merelyHot));
        when(tags.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(PostTag.builder().postId(1L).tagId(100L).build()));
        when(ranking.hotScore(preferred)).thenReturn(1.0);
        when(ranking.hotScore(merelyHot)).thenReturn(100.0);

        assertThat(service.recommend(99L, null)).containsExactly(1L, 2L);
    }
}
