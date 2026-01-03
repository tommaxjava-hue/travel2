package com.example.travelaibackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.travelaibackend.entity.Attraction;

public interface IAttractionService extends IService<Attraction> {
    // 这里以后可以加复杂的搜索逻辑，比如 "根据 经纬度 查找附近景点"
}