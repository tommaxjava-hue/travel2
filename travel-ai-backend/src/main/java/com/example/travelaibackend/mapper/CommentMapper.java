package com.example.travelaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.travelaibackend.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    // 自定义 SQL：查评论同时关联用户表，获取用户名
    @Select("SELECT c.*, u.username FROM comment c LEFT JOIN sys_user u ON c.user_id = u.user_id WHERE c.spot_id = #{spotId} ORDER BY c.create_time DESC")
    List<Comment> selectCommentsBySpotId(Long spotId);
}