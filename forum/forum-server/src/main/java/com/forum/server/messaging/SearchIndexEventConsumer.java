package com.forum.server.messaging;

import com.forum.server.messaging.event.SearchIndexEvent;
import com.forum.server.service.SearchIndexWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexEventConsumer {

    private final SearchIndexWriter searchIndexWriter;

    @KafkaListener(
            topics = "${forum.kafka.topics.search-index:forum.search-index.events}",
            groupId = "${spring.kafka.consumer.group-id:forum-notification-consumer}"
    )
    public void consume(SearchIndexEvent event) {
        if (event == null || event.getPostId() == null) {
            return;
        }

        // ES index writes are idempotent: indexing the same post overwrites the same document id, delete is also safe to retry.
        if (SearchIndexEvent.ACTION_DELETE.equals(event.getAction())) {
            searchIndexWriter.deletePostIndex(event.getPostId());
        } else {
            searchIndexWriter.syncPostIndex(event.getPostId());
        }
    }
}
