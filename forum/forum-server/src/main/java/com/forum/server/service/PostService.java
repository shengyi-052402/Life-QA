package com.forum.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forum.common.result.PageResult;
import com.forum.pojo.dto.PostCreateDTO;
import com.forum.pojo.dto.PostPageQueryDTO;
import com.forum.pojo.dto.PostUpdateDTO;
import com.forum.pojo.entity.Post;
import com.forum.pojo.vo.PostDetailVO;
import com.forum.pojo.vo.PostListVO;

public interface PostService extends IService<Post> {

    /**
     * 发帖
     */
    Long createPost(PostCreateDTO postCreateDTO);

    /**
     * 修改帖子
     */
    void updatePost(PostUpdateDTO postUpdateDTO);

    /**
     * 删除帖子
     */
    void deletePost(Long id);

    /**
     * 获取帖子详情
     */
    PostDetailVO getPostDetail(Long id);

    /**
     * 分页查询帖子列表
     */
    PageResult<PostListVO> getPostPage(PostPageQueryDTO queryDTO);
}
