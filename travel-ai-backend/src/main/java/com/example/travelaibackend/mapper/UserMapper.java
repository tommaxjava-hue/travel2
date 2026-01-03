package com.example.travelaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.travelaibackend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
    // 哪怕这里一行代码不写，我们已经拥有了 CRUD 能力
}