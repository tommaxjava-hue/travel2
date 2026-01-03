package com.example.travelaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.travelaibackend.entity.Itinerary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ItineraryMapper extends BaseMapper<Itinerary> {
}