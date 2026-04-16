package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forum.pojo.entity.Tag;
import com.forum.pojo.vo.TagVO;
import com.forum.server.mapper.TagMapper;
import com.forum.server.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    public List<TagVO> getTagList(int page, int size) {
        Page<Tag> pageParam = new Page<>(page, size);
        page(pageParam, new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getPostCount));
        
        return pageParam.getRecords().stream().map(tag -> {
            TagVO vo = TagVO.builder().build();
            BeanUtils.copyProperties(tag, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TagVO> getHotTags(int limit) {
        Page<Tag> pageParam = new Page<>(1, limit);
        page(pageParam, new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getPostCount));
        
        return pageParam.getRecords().stream().map(tag -> {
            TagVO vo = TagVO.builder().build();
            BeanUtils.copyProperties(tag, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
