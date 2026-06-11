package com.forum.server.service;

public interface SearchIndexWriter {

    void syncPostIndex(Long postId);

    void deletePostIndex(Long postId);
}
