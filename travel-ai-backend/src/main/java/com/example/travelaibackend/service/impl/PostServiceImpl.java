package com.example.travelaibackend.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.travelaibackend.entity.Post;
import com.example.travelaibackend.mapper.PostMapper;
import com.example.travelaibackend.service.IPostService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {
    @Override
    public List<Post> getPostList() {
        return baseMapper.selectPostWithUser();
    }
}