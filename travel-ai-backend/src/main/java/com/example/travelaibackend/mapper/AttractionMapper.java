package com.example.travelaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.travelaibackend.entity.Attraction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttractionMapper extends BaseMapper<Attraction> {
    // 同样，空着就行，MyBatis-Plus 会自动帮我们生成 SQL
}