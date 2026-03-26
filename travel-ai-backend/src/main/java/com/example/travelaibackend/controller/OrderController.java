package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.common.ResultCode;
import com.example.travelaibackend.dto.OrderCreateDTO;
import com.example.travelaibackend.service.IOrderService;
import com.example.travelaibackend.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
@CrossOrigin
public class OrderController {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    // 【核心补全】：提供查询“我的订单”接口，打通前端二次支付业务流
    @GetMapping("/myList")
    public Result<List<Map<String, Object>>> myOrders() {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT order_id as orderId, spot_name as spotName, price, status, create_time as createTime FROM sys_order WHERE user_id = ? ORDER BY create_time DESC",
                userId
        );
        return Result.success(list);
    }
}