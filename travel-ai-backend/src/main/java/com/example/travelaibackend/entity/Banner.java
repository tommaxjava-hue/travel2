package com.example.travelaibackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("banner")
public class Banner {
    // 严格映射数据库的 int 类型自增主键
    @TableId(type = IdType.AUTO)
    private Integer bannerId;

    private String imageUrl;

    private String linkUrl;

    private Integer sortOrder;

    private Integer isShow;

    private LocalDateTime createTime;
}