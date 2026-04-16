package com.forum.pojo.dto;

import lombok.Data;
import java.io.Serializable;

/** 帖子分页查询 DTO */
@Data
public class PostPageQueryDTO implements Serializable {
    private Integer page = 1;
    private Integer size = 20;
    /** 排序: latest/hot/most_liked/most_commented */
    private String sort = "latest";
    private Long categoryId;
    private Long tagId;
    private String keyword;
}
