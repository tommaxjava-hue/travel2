package com.example.travelaibackend.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.travelaibackend.entity.Comment;
import java.util.List;

public interface ICommentService extends IService<Comment> {
    List<Comment> getCommentsBySpotId(Long spotId);
}