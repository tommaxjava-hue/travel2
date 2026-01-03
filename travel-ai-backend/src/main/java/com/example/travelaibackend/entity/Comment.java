package com.example.travelaibackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Long commentId;
    private Long userId;
    private Long spotId;
    private String content;
    private Integer score; // 1-5分
    private LocalDateTime createTime;

    // 下面这两个字段数据库表里没有，是查出来给前端显示的（谁发的评论？）
    // MyBatis-Plus 需要用 exist=false 标记它们不是数据库字段
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String username;
}