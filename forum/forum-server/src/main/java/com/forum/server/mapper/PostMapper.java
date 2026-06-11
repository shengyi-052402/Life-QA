package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    
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
