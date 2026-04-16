package com.forum.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** User Mapper 接口 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
