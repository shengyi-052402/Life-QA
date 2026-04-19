package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {
}
