package com.example.travelaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.travelaibackend.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    // 关联查询：查帖子同时把作者名字查出来
    @Select("SELECT p.*, u.username as author_name FROM post p LEFT JOIN sys_user u ON p.user_id = u.user_id ORDER BY p.create_time DESC")
    List<Post> selectPostWithUser();
}