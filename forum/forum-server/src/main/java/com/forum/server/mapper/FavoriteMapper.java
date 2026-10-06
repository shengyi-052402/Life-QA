package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
    @Select("SELECT * FROM favorite WHERE post_id = #{postId} AND user_id = #{userId} FOR UPDATE")
    Favorite selectForUpdate(@Param("postId") Long postId, @Param("userId") Long userId);
}
