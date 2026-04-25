package com.forum.pojo.es;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostDocument implements Serializable {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private List<String> tagNames;
    private String authorName;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
