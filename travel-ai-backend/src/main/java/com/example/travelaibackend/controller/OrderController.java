package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/order")
@CrossOrigin
public class OrderController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/create")
    public Result<Long> createOrder(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        Integer spotId = (Integer) params.get("spotId");
        String spotName = (String) params.get("spotName");
        Double price = Double.valueOf(params.get("price").toString());

        jdbcTemplate.update("INSERT INTO sys_order (user_id, spot_id, spot_name, price, status, create_time) VALUES (?,?,?,?,'UNPAID',NOW())",
                userId, spotId, spotName, price);

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Result.success(id);
    }

    @PostMapping("/pay")
    public Result<?> payOrder(@RequestBody Map<String, Object> params) {
        // 这里可以加简单的 Token 校验
        Integer orderId = Integer.valueOf(params.get("orderId").toString());
        jdbcTemplate.update("UPDATE sys_order SET status = 'PAID' WHERE order_id = ?", orderId);
        return Result.success("支付成功");
    }
}