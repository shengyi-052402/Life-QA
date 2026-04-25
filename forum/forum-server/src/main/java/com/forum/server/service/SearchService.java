package com.forum.server.service;

import com.forum.common.result.PageResult;
import com.forum.pojo.vo.PostListVO;

import java.util.List;

public interface SearchService {

    PageResult<PostListVO> searchPosts(String keyword, Integer page, Integer size);

    List<String> suggest(String keyword);

    void syncPost(Long postId);

    void deletePost(Long postId);

    void reindexAll();
}
