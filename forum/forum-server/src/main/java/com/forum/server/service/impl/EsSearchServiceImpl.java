package com.forum.server.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.forum.common.result.PageResult;
import com.forum.pojo.es.PostDocument;
import com.forum.pojo.vo.PostListVO;
import com.forum.server.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forum.search.es", name = "enabled", havingValue = "true")
public class EsSearchServiceImpl implements SearchService {
    private static final String HIGHLIGHT_PRE_TAG = "<em>";
    private static final String HIGHLIGHT_POST_TAG = "</em>";

    private final ElasticsearchClient elasticsearchClient;
    private final SearchSupport searchSupport;

    @Value("${forum.search.es.index-name:forum_posts}")
    private String indexName;

    @Value("${forum.search.es.suggest-size:8}")
    private Integer suggestSize;

    @Override
    public PageResult<PostListVO> searchPosts(String keyword, Integer page, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.max(size == null ? 15 : size, 1);

        try {
            ensureIndex();

            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .from((safePage - 1) * safeSize)
                            .size(safeSize)
                            .query(q -> q.multiMatch(m -> m
                                    .query(keyword)
                                    .fields("title^4", "summary^2", "content", "tagNames^3")
                                    .operator(Operator.Or)))
                            .highlight(h -> h
                                    .preTags(HIGHLIGHT_PRE_TAG)
                                    .postTags(HIGHLIGHT_POST_TAG)
                                    .fields("title", f -> f.numberOfFragments(0))
                                    .fields("summary", f -> f.numberOfFragments(0))
                                    .fields("content", f -> f.fragmentSize(160).numberOfFragments(1))
                                    .fields("tagNames", f -> f.numberOfFragments(0)))
                            .sort(so -> so.score(sc -> sc.order(SortOrder.Desc)))
                            .sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc))),
                    PostDocument.class);

            List<Hit<PostDocument>> hits = response.hits().hits();
            List<Long> orderedIds = hits.stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(PostDocument::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            Map<Long, Map<String, List<String>>> highlightMap = hits.stream()
                    .filter(hit -> hit.source() != null && hit.source().getId() != null)
                    .collect(Collectors.toMap(
                            hit -> hit.source().getId(),
                            hit -> hit.highlight() == null ? Collections.emptyMap() : hit.highlight(),
                            (left, right) -> left
                    ));
            long total = response.hits().total() == null ? orderedIds.size() : response.hits().total().value();
            return new PageResult<>(total, searchSupport.buildSearchResultVOs(orderedIds, highlightMap, keyword));
        } catch (Exception e) {
            log.warn("ES 搜索失败，回退到数据库搜索: {}", e.getMessage());
            return searchSupport.searchFromDatabase(keyword, safePage, safeSize);
        }
    }

    @Override
    public List<String> suggest(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        try {
            ensureIndex();

            SearchResponse<PostDocument> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .size(suggestSize)
                            .query(q -> q.bool(b -> b
                                    .should(sh -> sh.matchPhrasePrefix(m -> m.field("title").query(keyword)))
                                    .should(sh -> sh.matchPhrasePrefix(m -> m.field("tagNames").query(keyword)))
                                    .minimumShouldMatch("1")))
                            .sort(so -> so.score(sc -> sc.order(SortOrder.Desc)))
                            .sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc))),
                    PostDocument.class);

            LinkedHashSet<String> suggestions = new LinkedHashSet<>();
            String lowerKeyword = keyword.toLowerCase();
            for (Hit<PostDocument> hit : response.hits().hits()) {
                PostDocument doc = hit.source();
                if (doc == null) {
                    continue;
                }
                if (StringUtils.hasText(doc.getTitle()) && doc.getTitle().toLowerCase().contains(lowerKeyword)) {
                    suggestions.add(doc.getTitle());
                }
                if (doc.getTagNames() != null) {
                    doc.getTagNames().stream()
                            .filter(StringUtils::hasText)
                            .filter(tag -> tag.toLowerCase().contains(lowerKeyword))
                            .forEach(suggestions::add);
                }
                if (suggestions.size() >= suggestSize) {
                    break;
                }
            }
            if (!suggestions.isEmpty()) {
                return suggestions.stream().limit(suggestSize).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("ES 搜索建议失败，回退到数据库建议: {}", e.getMessage());
        }

        return searchSupport.suggestFromDatabase(keyword, suggestSize);
    }

    @Override
    public void syncPost(Long postId) {
        try {
            PostDocument document = searchSupport.buildDocument(postId);
            ensureIndex();
            if (document == null) {
                deletePost(postId);
                return;
            }
            IndexRequest<PostDocument> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(String.valueOf(postId))
                    .document(document));
            elasticsearchClient.index(request);
        } catch (Exception e) {
            log.warn("同步帖子到 ES 失败, postId={}: {}", postId, e.getMessage());
        }
    }

    @Override
    public void deletePost(Long postId) {
        try {
            ensureIndex();
            DeleteRequest request = DeleteRequest.of(d -> d.index(indexName).id(String.valueOf(postId)));
            elasticsearchClient.delete(request);
        } catch (Exception e) {
            log.warn("从 ES 删除帖子失败, postId={}: {}", postId, e.getMessage());
        }
    }

    @Override
    public void reindexAll() {
        try {
            ensureIndex();
            List<PostDocument> documents = searchSupport.buildAllDocuments();
            elasticsearchClient.deleteByQuery(d -> d.index(indexName).query(q -> q.matchAll(m -> m)));
            if (documents.isEmpty()) {
                return;
            }

            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (PostDocument document : documents) {
                builder.operations(op -> op.index(idx -> idx
                        .index(indexName)
                        .id(String.valueOf(document.getId()))
                        .document(document)));
            }
            elasticsearchClient.bulk(builder.build());
        } catch (Exception e) {
            log.warn("全量重建 ES 索引失败: {}", e.getMessage());
        }
    }

    private void ensureIndex() throws IOException {
        boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
        if (exists) {
            return;
        }

        elasticsearchClient.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("title", p -> p.text(t -> t))
                        .properties("summary", p -> p.text(t -> t))
                        .properties("content", p -> p.text(t -> t))
                        .properties("tagNames", p -> p.text(t -> t))
                        .properties("authorName", p -> p.text(t -> t))
                        .properties("categoryName", p -> p.keyword(k -> k))
                        .properties("createdAt", p -> p.date(d -> d))
                        .properties("updatedAt", p -> p.date(d -> d))));
    }
}
