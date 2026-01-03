package com.example.travelaibackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("attraction") // 对应数据库表名
public class Attraction {

    // 指定主键字段名和自增策略
    @TableId(value = "spot_id", type = IdType.AUTO)
    private Long spotId;

    private String name;

    private String description;

    // 专门用来喂给 AI 的长文本
    private String contentText;

    private String address;

    private String city;

    private String tags; // 逗号分隔的标签

    private String openTime;

    private BigDecimal ticketPrice;

    private String imageUrl;

    private BigDecimal rating;

    private LocalDateTime createTime;

    // --- 第九步新增：经纬度字段 ---
    private BigDecimal latitude;
    private BigDecimal longitude;

    private Integer isHot;
}