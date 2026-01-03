package com.example.travelaibackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {
    @TableId(value = "post_id", type = IdType.AUTO)
    private Long postId;
    private Long userId;
    private String title;
    private String coverImg;
    private String content;
    private Integer viewCount;
    private LocalDateTime createTime;

    // 辅助字段：作者名 (数据库里没有，查出来填进去)
    @TableField(exist = false)
    private String authorName;
    // 辅助字段：作者头像
    @TableField(exist = false)
    private String authorAvatar;
}