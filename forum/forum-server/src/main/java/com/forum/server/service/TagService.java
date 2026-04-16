package com.forum.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forum.pojo.entity.Tag;
import com.forum.pojo.vo.TagVO;

import java.util.List;

public interface TagService extends IService<Tag> {

    /**
     * 获取标签列表（带分页，或者限制数量返回热门）
     */
    List<TagVO> getTagList(int page, int size);

    /**
     * 获取热门标签
     */
    List<TagVO> getHotTags(int limit);
}
