package com.example.travelaibackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.travelaibackend.entity.Itinerary;
import com.example.travelaibackend.mapper.ItineraryMapper;
import com.example.travelaibackend.service.IItineraryService;
import org.springframework.stereotype.Service;

@Service
public class ItineraryServiceImpl extends ServiceImpl<ItineraryMapper, Itinerary> implements IItineraryService {
}