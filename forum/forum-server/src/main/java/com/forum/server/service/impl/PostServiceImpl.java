package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forum.common.constant.MessageConstant;
import com.forum.common.context.BaseContext;
import com.forum.common.exception.BaseException;
import com.forum.common.exception.ForbiddenException;
import com.forum.common.result.PageResult;
import com.forum.common.utils.HtmlUtil;
import com.forum.pojo.dto.PostCreateDTO;
import com.forum.pojo.dto.PostPageQueryDTO;
import com.forum.pojo.dto.PostUpdateDTO;
import com.forum.pojo.entity.Category;
import com.forum.pojo.entity.Post;
import com.forum.pojo.entity.PostTag;
import com.forum.pojo.entity.Tag;
import com.forum.pojo.entity.User;
import com.forum.pojo.vo.PostDetailVO;
import com.forum.pojo.vo.PostListVO;
import com.forum.pojo.vo.TagVO;
import com.forum.pojo.vo.UserVO;
import com.forum.server.mapper.PostMapper;
import com.forum.server.mapper.PostTagMapper;
import com.forum.server.service.CategoryService;
import com.forum.server.service.PostService;
import com.forum.server.service.TagService;
import com.forum.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final TagService tagService;
    private final PostTagMapper postTagMapper;
    private final UserService userService;
    private final CategoryService categoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(PostCreateDTO dto) {
        Long userId = BaseContext.getCurrentId();
        
        // 自动提取摘要 (前200字符纯文本)
        String summary = HtmlUtil.getSummary(dto.getContent(), 200);

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .summary(summary)
                .userId(userId)
                .categoryId(dto.getCategoryId())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .favoriteCount(0)
                .isTop(0)
                .isEssence(0)
                .status(1) // 默认正常
                .build();
        
        save(post);
        Long postId = post.getId();

        // 增加用户发帖数
        User user = userService.getById(userId);
        if (user != null) {
            user.setPostCount((user.getPostCount() == null ? 0 : user.getPostCount()) + 1);
            userService.updateById(user);
        }

        // 处理分类帖子数
        Category category = categoryService.getById(dto.getCategoryId());
        if (category != null) {
            category.setPostCount((category.getPostCount() == null ? 0 : category.getPostCount()) + 1);
            categoryService.updateById(category);
        }

        // 处理标签
        handleTags(postId, dto.getTagIds(), dto.getNewTags());

        // TODO 同步 ES (阶段4)
        
        return postId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(PostUpdateDTO dto) {
        Post post = getById(dto.getId());
        if (post == null) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }
        
        // 鉴权：只有作者或管理员才能修改
        Long userId = BaseContext.getCurrentId();
        User currentUser = userService.getById(userId);
        if (!post.getUserId().equals(userId) && currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        // 分类变化
        if (!post.getCategoryId().equals(dto.getCategoryId())) {
            Category oldC = categoryService.getById(post.getCategoryId());
            if (oldC != null) {
                oldC.setPostCount(Math.max(0, oldC.getPostCount() - 1));
                categoryService.updateById(oldC);
            }
            Category newC = categoryService.getById(dto.getCategoryId());
            if (newC != null) {
                newC.setPostCount(newC.getPostCount() + 1);
                categoryService.updateById(newC);
            }
        }

        String summary = HtmlUtil.getSummary(dto.getContent(), 200);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setSummary(summary);
        post.setCategoryId(dto.getCategoryId());
        
        updateById(post);

        // 删除旧的标签关联重建
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, post.getId()));
        handleTags(post.getId(), dto.getTagIds(), dto.getNewTags());

        // TODO 同步 ES (阶段4)
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        Post post = getById(id);
        if (post == null) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }
        
        Long userId = BaseContext.getCurrentId();
        User currentUser = userService.getById(userId);
        if (!post.getUserId().equals(userId) && currentUser.getRole() != 1) {
            throw new ForbiddenException(MessageConstant.NO_PERMISSION);
        }

        // 扣除用户发帖数、分类发帖数等冗余字段...
        
        removeById(id);
        // 删除关联标签
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, id));

        // TODO 从 ES 中删除 (阶段4)
    }

    @Override
    public PostDetailVO getPostDetail(Long id) {
        Post post = getById(id);
        if (post == null || post.getStatus() != 1) {
            throw new BaseException(MessageConstant.POST_NOT_FOUND);
        }

        // 异步增加浏览量或直接更新
        baseMapper.incrementViewCount(id);
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);

        PostDetailVO vo = PostDetailVO.builder().build();
        BeanUtils.copyProperties(post, vo);
        vo.setIsTop(post.getIsTop() == 1);
        vo.setIsEssence(post.getIsEssence() == 1);

        // 作者
        User user = userService.getById(post.getUserId());
        if (user != null) {
            vo.setAuthor(UserVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build());
        }

        // 分类
        Category category = categoryService.getById(post.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        // 标签
        List<PostTag> postTags = postTagMapper.selectList(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, id));
        if (!postTags.isEmpty()) {
            List<Long> tagIds = postTags.stream().map(PostTag::getTagId).collect(Collectors.toList());
            List<Tag> tags = tagService.listByIds(tagIds);
            List<TagVO> tagVOs = tags.stream().map(t -> {
                TagVO tvo = TagVO.builder().build();
                BeanUtils.copyProperties(t, tvo);
                return tvo;
            }).collect(Collectors.toList());
            vo.setTags(tagVOs);
        } else {
            vo.setTags(new ArrayList<>());
        }

        // TODO: 查询当前用户是否点赞/收藏 (第3阶段)
        vo.setIsLiked(false);
        vo.setIsFavorited(false);

        return vo;
    }

    @Override
    public PageResult<PostListVO> getPostPage(PostPageQueryDTO queryDTO) {
        Page<Post> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1);

        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Post::getCategoryId, queryDTO.getCategoryId());
        }

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(Post::getTitle, queryDTO.getKeyword());
        }

        // 排序规则
        if ("hot".equals(queryDTO.getSort())) {
            wrapper.orderByDesc(Post::getViewCount);
        } else if ("most_liked".equals(queryDTO.getSort())) {
            wrapper.orderByDesc(Post::getLikeCount);
        } else if ("most_commented".equals(queryDTO.getSort())) {
            wrapper.orderByDesc(Post::getCommentCount);
        } else {
            // 默认 latest
            // 置顶帖优先，再按时间倒序
            wrapper.orderByDesc(Post::getIsTop).orderByDesc(Post::getCreatedAt);
        }

        page(pageParam, wrapper);

        List<PostListVO> records = pageParam.getRecords().stream().map(post -> {
            PostListVO vo = PostListVO.builder().build();
            BeanUtils.copyProperties(post, vo);
            vo.setIsTop(post.getIsTop() == 1);
            vo.setIsEssence(post.getIsEssence() == 1);

            User user = userService.getById(post.getUserId());
            if (user != null) {
                vo.setAuthor(UserVO.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .build());
            }

            Category category = categoryService.getById(post.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(pageParam.getTotal(), records);
    }

    private void handleTags(Long postId, List<Long> tagIds, List<String> newTags) {
        if (tagIds == null) tagIds = new ArrayList<>();
        
        // 处理新标签
        if (newTags != null && !newTags.isEmpty()) {
            for (String tagName : newTags) {
                if (!StringUtils.hasText(tagName)) continue;
                // 查重
                Tag tag = tagService.getOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagName));
                if (tag == null) {
                    tag = Tag.builder().name(tagName).postCount(0).build();
                    tagService.save(tag);
                }
                tagIds.add(tag.getId());
            }
        }

        // 去重后保存关联并增加使用次数
        tagIds = tagIds.stream().distinct().collect(Collectors.toList());
        for (Long tid : tagIds) {
            postTagMapper.insert(PostTag.builder().postId(postId).tagId(tid).build());
            // 标签热度+1
            Tag tag = tagService.getById(tid);
            if(tag != null){
                 tag.setPostCount(tag.getPostCount() + 1);
                 tagService.updateById(tag);
            }
        }
    }
}
