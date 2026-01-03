package com.example.travelaibackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.mapper.UserMapper;
import com.example.travelaibackend.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements IUserService {
    // 继承 ServiceImpl 后，自动实现了基本的增删改查
}