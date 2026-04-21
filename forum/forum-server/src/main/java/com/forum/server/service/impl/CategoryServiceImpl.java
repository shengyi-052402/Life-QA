package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forum.common.constant.MessageConstant;
import com.forum.common.exception.BaseException;
import com.forum.pojo.dto.CategoryDTO;
import com.forum.pojo.entity.Category;
import com.forum.pojo.entity.Post;
import com.forum.pojo.vo.CategoryVO;
import com.forum.server.mapper.CategoryMapper;
import com.forum.server.mapper.PostMapper;
import com.forum.server.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final PostMapper postMapper;

    @Override
    public List<CategoryVO> getAllCategories() {
        List<Category> categories = list(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByDesc(Category::getCreatedAt));

        return categories.stream().map(category -> {
            CategoryVO vo = CategoryVO.builder().build();
            BeanUtils.copyProperties(category, vo);
            vo.setPostCount(countPostsByCategory(category.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public CategoryVO getCategoryById(Long id) {
        Category category = getById(id);
        if (category == null) {
            throw new BaseException(MessageConstant.CATEGORY_NOT_FOUND);
        }

        CategoryVO vo = CategoryVO.builder().build();
        BeanUtils.copyProperties(category, vo);
        vo.setPostCount(countPostsByCategory(category.getId()));
        return vo;
    }

    @Override
    public void addCategory(CategoryDTO categoryDTO) {
        long count = count(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, categoryDTO.getName()));
        if (count > 0) {
            throw new BaseException(MessageConstant.CATEGORY_NAME_EXISTS);
        }

        Category category = Category.builder().build();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setPostCount(0);
        save(category);
    }

    @Override
    public void updateCategory(CategoryDTO categoryDTO) {
        Category category = getById(categoryDTO.getId());
        if (category == null) {
            throw new BaseException(MessageConstant.CATEGORY_NOT_FOUND);
        }

        if (!category.getName().equals(categoryDTO.getName())) {
            long count = count(new LambdaQueryWrapper<Category>()
                    .eq(Category::getName, categoryDTO.getName()));
            if (count > 0) {
                throw new BaseException(MessageConstant.CATEGORY_NAME_EXISTS);
            }
        }

        BeanUtils.copyProperties(categoryDTO, category);
        updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = getById(id);
        if (category == null) {
            throw new BaseException(MessageConstant.CATEGORY_NOT_FOUND);
        }

        if (countPostsByCategory(id) > 0) {
            throw new BaseException(MessageConstant.CATEGORY_HAS_POSTS);
        }

        removeById(id);
    }

    private Integer countPostsByCategory(Long categoryId) {
        return Math.toIntExact(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getCategoryId, categoryId)
                .eq(Post::getStatus, 1)));
    }
}
