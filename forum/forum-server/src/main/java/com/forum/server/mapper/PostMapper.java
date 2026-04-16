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
}
