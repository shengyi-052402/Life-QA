package com.forum.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forum.common.result.PageResult;
import com.forum.pojo.dto.AdminPostPageQueryDTO;
import com.forum.pojo.dto.AdminPostUpdateDTO;
import com.forum.pojo.dto.PostCreateDTO;
import com.forum.pojo.dto.PostPageQueryDTO;
import com.forum.pojo.dto.PostUpdateDTO;
import com.forum.pojo.entity.Post;
import com.forum.pojo.vo.AdminPostVO;
import com.forum.pojo.vo.PostDetailVO;
import com.forum.pojo.vo.PostGlobeVO;
import com.forum.pojo.vo.PostListVO;

import java.util.List;

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
     * 获取帖子编辑信息
     */
    PostDetailVO getPostEditDetail(Long id);

    /**
     * 分页查询帖子列表
     */
    PageResult<PostListVO> getPostPage(PostPageQueryDTO queryDTO);

    PageResult<PostListVO> getRecommendationPage(Integer page, Integer size, Long categoryId);

    PageResult<AdminPostVO> getAdminPostPage(AdminPostPageQueryDTO queryDTO);

    void adminUpdatePost(Long id, AdminPostUpdateDTO dto);

    /**
     * 获取有地理位置的帖子列表（供 3D 地球渲染使用）
     */
    List<PostGlobeVO> getGlobePosts();
}
