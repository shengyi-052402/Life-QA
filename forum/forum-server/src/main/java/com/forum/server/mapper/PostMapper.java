package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    @Select("SELECT * FROM post WHERE id = #{id} FOR UPDATE")
    Post selectByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE post SET like_count = GREATEST(0, COALESCE(like_count, 0) + #{delta}), updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int adjustLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE post SET favorite_count = GREATEST(0, COALESCE(favorite_count, 0) + #{delta}), updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int adjustFavoriteCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE post SET comment_count = GREATEST(0, COALESCE(comment_count, 0) + #{delta}), updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int adjustCommentCount(@Param("id") Long id, @Param("delta") int delta);
    
    /**
     * 增加游览量
     */
    @Select("UPDATE post SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(@Param("id") Long id);

    /**
     * 查询当前可访问的帖子 ID，用于启动时预热布隆过滤器。
     */
    @Select("SELECT id FROM post WHERE status = 1")
    List<Long> selectActivePostIds();
}
