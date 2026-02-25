package com.example.travelaibackend.service;

import com.example.travelaibackend.dto.OrderCreateDTO;

public interface IOrderService {
    Long createOrder(OrderCreateDTO dto, Long userId);
    void payOrder(Long orderId, Long userId);
}