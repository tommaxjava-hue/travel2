package com.example.travelaibackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.travelaibackend.entity.Attraction;
import com.example.travelaibackend.mapper.AttractionMapper;
import com.example.travelaibackend.service.IAttractionService;
import org.springframework.stereotype.Service;

@Service
public class AttractionServiceImpl extends ServiceImpl<AttractionMapper, Attraction> implements IAttractionService {

}