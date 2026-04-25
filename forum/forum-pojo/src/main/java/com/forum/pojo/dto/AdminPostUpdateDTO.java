package com.forum.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
public class AdminPostUpdateDTO implements Serializable {

    @Min(value = 0, message = "帖子状态参数错误")
    @Max(value = 2, message = "帖子状态参数错误")
    private Integer status;

    @Min(value = 0, message = "置顶参数错误")
    @Max(value = 1, message = "置顶参数错误")
    private Integer isTop;

    @Min(value = 0, message = "精华参数错误")
    @Max(value = 1, message = "精华参数错误")
    private Integer isEssence;
}
