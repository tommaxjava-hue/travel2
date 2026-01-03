package com.example.travelaibackend.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.travelaibackend.entity.Post;
import java.util.List;

public interface IPostService extends IService<Post> {
    List<Post> getPostList();
}