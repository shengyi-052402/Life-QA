package com.forum.pojo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AdminUserPageQueryDTO implements Serializable {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private Integer role;
    private Integer status;
}
