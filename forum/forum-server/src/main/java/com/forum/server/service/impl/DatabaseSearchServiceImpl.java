package com.forum.server.service.impl;

import com.forum.common.result.PageResult;
import com.forum.pojo.vo.PostListVO;
import com.forum.server.messaging.SearchIndexEventPublisher;
import com.forum.server.service.SearchIndexWriter;
import com.forum.server.service.SearchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "forum.search.es", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DatabaseSearchServiceImpl implements SearchService, SearchIndexWriter {

    private final SearchSupport searchSupport;
    private final SearchIndexEventPublisher searchIndexEventPublisher;

    public DatabaseSearchServiceImpl(SearchSupport searchSupport, SearchIndexEventPublisher searchIndexEventPublisher) {
        this.searchSupport = searchSupport;
        this.searchIndexEventPublisher = searchIndexEventPublisher;
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
        searchIndexEventPublisher.publishSync(postId);
    }

    @Override
    public void deletePost(Long postId) {
        searchIndexEventPublisher.publishDelete(postId);
    }

    @Override
    public void reindexAll() {
    }

    @Override
    public void syncPostIndex(Long postId) {
        // Database search has no external index, so consuming the event intentionally does nothing.
    }

    @Override
    public void deletePostIndex(Long postId) {
        // Database search reads committed rows directly; no index document needs to be deleted.
    }
}
