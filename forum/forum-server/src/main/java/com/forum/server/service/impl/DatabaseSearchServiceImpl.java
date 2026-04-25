package com.forum.server.service.impl;

import com.forum.common.result.PageResult;
import com.forum.pojo.vo.PostListVO;
import com.forum.server.service.SearchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "forum.search.es", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DatabaseSearchServiceImpl implements SearchService {

    private final SearchSupport searchSupport;

    public DatabaseSearchServiceImpl(SearchSupport searchSupport) {
        this.searchSupport = searchSupport;
    }

    @Override
    public PageResult<PostListVO> searchPosts(String keyword, Integer page, Integer size) {
        return searchSupport.searchFromDatabase(keyword, page, size);
    }

    @Override
    public List<String> suggest(String keyword) {
        return searchSupport.suggestFromDatabase(keyword, 8);
    }

    @Override
    public void syncPost(Long postId) {
    }

    @Override
    public void deletePost(Long postId) {
    }

    @Override
    public void reindexAll() {
    }
}
