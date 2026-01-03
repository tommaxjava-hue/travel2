package com.example.travelaibackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("itinerary")
public class Itinerary {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title; // 行程标题，如"国庆北京游"

    private LocalDate startDate; // 开始日期

    private LocalDate endDate;   // 结束日期

    private String note;         // 这里存 AI 生成的完整路线文本

    private LocalDateTime createTime;
}