package com.forum.pojo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AdminPostPageQueryDTO implements Serializable {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private Integer status;
    private Long categoryId;
    private Long userId;
}
