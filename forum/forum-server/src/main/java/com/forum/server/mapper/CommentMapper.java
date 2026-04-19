package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    
    /**
     * 根据所属帖子ID和父评论ID，获取最近的回复用于一级评论自带的两条子回复展示
     */
    @Select("SELECT * FROM comment WHERE post_id = #{postId} AND parent_id = #{parentId} AND status = 1 ORDER BY created_at ASC LIMIT #{limit}")
    List<Comment> selectRecentReplies(@Param("postId") Long postId, @Param("parentId") Long parentId, @Param("limit") int limit);
}
