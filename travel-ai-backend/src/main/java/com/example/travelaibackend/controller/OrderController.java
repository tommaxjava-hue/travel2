package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.common.ResultCode;
import com.example.travelaibackend.dto.OrderCreateDTO;
import com.example.travelaibackend.service.IOrderService;
import com.example.travelaibackend.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/order")
@CrossOrigin
public class OrderController {

    @Autowired
    private IOrderService orderService;

    @PostMapping("/create")
    public Result<Long> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        Long orderId = orderService.createOrder(dto, userId);
        return Result.success(orderId);
    }

    @PostMapping("/pay")
    public Result<?> payOrder(@RequestBody Map<String, Object> params) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        Integer orderIdObj = (Integer) params.get("orderId");
        if (orderIdObj == null) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "订单ID不能为空");
        }

        orderService.payOrder(orderIdObj.longValue(), userId);
        return Result.success("支付成功");
    }
}