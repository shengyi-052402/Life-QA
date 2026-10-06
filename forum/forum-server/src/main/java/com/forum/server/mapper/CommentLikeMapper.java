package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {
    @Select("SELECT * FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId} FOR UPDATE")
    CommentLike selectForUpdate(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
