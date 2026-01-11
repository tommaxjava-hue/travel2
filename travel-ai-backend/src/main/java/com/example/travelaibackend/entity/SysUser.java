package com.example.travelaibackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user") // 对应数据库表名
public class SysUser {
    @TableId(type = IdType.AUTO) // 主键自增
    private Long userId;

    private String username;
    private String password;
    private String email;
    private String role;
    private LocalDateTime createTime;

    // --- 个人资料字段 (对应新SQL) ---
    private String tags;    // 兴趣标签
    private String name;    // 姓名/昵称 (原nickname)
    private String avatar;  // 头像
    private String phone;   // 电话

    // --- 新增字段 ---
    private String gender;  // 性别
    private Integer age;    // 年龄
    private String city;    // 城市
    private Integer status; // 状态: 1正常 0禁用
}