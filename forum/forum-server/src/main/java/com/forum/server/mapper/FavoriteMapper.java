package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
