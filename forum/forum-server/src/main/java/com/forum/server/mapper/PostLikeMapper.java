package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {
    // Current read: do not reuse an older REPEATABLE READ snapshot after waiting for the post lock.
    @Select("SELECT * FROM post_like WHERE post_id = #{postId} AND user_id = #{userId} FOR UPDATE")
    PostLike selectForUpdate(@Param("postId") Long postId, @Param("userId") Long userId);
}
