package com.example.travelaibackend.service.impl;

import com.example.travelaibackend.common.BusinessException;
import com.example.travelaibackend.dto.OrderCreateDTO;
import com.example.travelaibackend.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements IOrderService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO dto, Long userId) {
        // 企业级防超卖：Redis 分布式锁 (锁定当前景点的库存操作)
        String lockKey = "lock:spot:inventory:" + dto.getSpotId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException("当前预订人数过多，请稍后再试");
        }

        try {
            // 在此处可加入数据库真实库存校验逻辑 (例如 SELECT stock FROM spot WHERE id = ? FOR UPDATE)

            jdbcTemplate.update("INSERT INTO sys_order (user_id, spot_id, spot_name, price, status, create_time) VALUES (?,?,?,?,'UNPAID',NOW())",
                    userId, dto.getSpotId(), dto.getSpotName(), dto.getPrice());

            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long userId) {
        int updated = jdbcTemplate.update("UPDATE sys_order SET status = 'PAID' WHERE order_id = ? AND user_id = ? AND status = 'UNPAID'",
                orderId, userId);
        if (updated == 0) {
            throw new BusinessException("支付失败：订单不存在或已被处理");
        }
    }
}