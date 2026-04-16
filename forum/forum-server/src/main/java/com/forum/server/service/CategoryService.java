package com.forum.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forum.pojo.dto.CategoryDTO;
import com.forum.pojo.entity.Category;
import com.forum.pojo.vo.CategoryVO;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /**
     * 获取所有分类
     */
    List<CategoryVO> getAllCategories();

    /**
     * 根据 ID 获取分类信息
     */
    CategoryVO getCategoryById(Long id);

    /**
     * 管理后台：创建分类
     */
    void addCategory(CategoryDTO categoryDTO);

    /**
     * 管理后台：编辑分类
     */
    void updateCategory(CategoryDTO categoryDTO);

    /**
     * 管理后台：删除分类
     */
    void deleteCategory(Long id);
}
