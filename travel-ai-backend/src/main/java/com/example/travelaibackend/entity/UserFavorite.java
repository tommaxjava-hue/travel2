package com.example.travelaibackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_favorite") // 对应数据库表名
public class UserFavorite {

    @TableId(value = "favorite_id", type = IdType.AUTO)
    private Long favoriteId;

    private Long userId;

    private Long spotId;

    private LocalDateTime createTime;
}