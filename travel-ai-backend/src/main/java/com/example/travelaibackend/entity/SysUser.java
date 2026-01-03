package com.example.travelaibackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user") // 指定对应的数据库表名
public class SysUser {
    @TableId(type = IdType.AUTO) // 指定主键是自增的
    private Long userId;
    private String username;
    private String password;
    private String email;
    private String role;
    private LocalDateTime createTime;
    private String tags;
}