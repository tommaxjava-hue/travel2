package com.example.travelaibackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.travelaibackend.entity.Comment;
import com.example.travelaibackend.mapper.CommentMapper;
import com.example.travelaibackend.service.ICommentService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {
    @Override
    public List<Comment> getCommentsBySpotId(Long spotId) {
        return baseMapper.selectCommentsBySpotId(spotId);
    }
}