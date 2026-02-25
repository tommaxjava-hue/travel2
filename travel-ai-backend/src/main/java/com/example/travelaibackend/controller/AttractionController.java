package com.example.travelaibackend.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.annotation.RequireSuperAdmin;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.common.ResultCode;
import com.example.travelaibackend.entity.Attraction;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IAttractionService;
import com.example.travelaibackend.service.IUserService;
import com.example.travelaibackend.utils.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attraction")
@CrossOrigin
public class AttractionController {

    @Autowired
    private IAttractionService attractionService;

    @Autowired
    private IUserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/list")
    public Result<List<Attraction>> getList(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String city) {
        Long userId = UserContext.getUserId();
        String cacheKey = "attraction:list:" + StrUtil.nullToEmpty(keyword) + ":" + StrUtil.nullToEmpty(city);

        // 1. 尝试查 Redis 缓存 (仅在无个性化查询时)
        if (userId == null && Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            try {
                String json = redisTemplate.opsForValue().get(cacheKey);
                // 防缓存穿透：如果是特殊空值标识 "[]"，直接返回空集合
                if ("[]".equals(json)) {
                    return Result.success(new ArrayList<>());
                }
                if (StrUtil.isNotBlank(json)) {
                    List<Attraction> list = mapper.readValue(json, new TypeReference<List<Attraction>>() {});
                    return Result.success(list);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2. 查数据库
        QueryWrapper<Attraction> query = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            query.and(w -> w.like("name", keyword).or().like("city", keyword).or().like("tags", keyword));
        }
        if (StrUtil.isNotBlank(city)) {
            query.eq("city", city);
        }

        List<Attraction> allSpots = attractionService.list(query);

        // 防缓存穿透：如果数据库也为空，缓存一个较短时间的空值
        if (allSpots == null || allSpots.isEmpty()) {
            if (userId == null) {
                redisTemplate.opsForValue().set(cacheKey, "[]", 60, TimeUnit.SECONDS);
            }
            return Result.success(new ArrayList<>());
        }

        // 3. 排序逻辑
        if (userId != null) {
            SysUser user = userService.getById(userId);
            if (user != null && StrUtil.isNotBlank(user.getTags())) {
                String[] userTags = user.getTags().replace("，", ",").split(",");
                allSpots.sort((s1, s2) -> {
                    int hot1 = s1.getIsHot() == null ? 0 : s1.getIsHot();
                    int hot2 = s2.getIsHot() == null ? 0 : s2.getIsHot();
                    if (hot1 != hot2) return hot2 - hot1;

                    int score1 = calculateTagMatchScore(s1.getTags(), userTags);
                    int score2 = calculateTagMatchScore(s2.getTags(), userTags);
                    if (score1 != score2) return score2 - score1;

                    return s2.getRating().compareTo(s1.getRating());
                });
            }
        } else {
            allSpots.sort((s1, s2) -> {
                int hot1 = s1.getIsHot() == null ? 0 : s1.getIsHot();
                int hot2 = s2.getIsHot() == null ? 0 : s2.getIsHot();
                if (hot1 != hot2) return hot2 - hot1;
                return s2.getRating().compareTo(s1.getRating());
            });
        }

        // 4. 写入缓存 (防雪崩：过期时间 10 分钟 + 0~5分钟随机抖动)
        if (userId == null) {
            try {
                String json = mapper.writeValueAsString(allSpots);
                long randomMinutes = ThreadLocalRandom.current().nextLong(5);
                redisTemplate.opsForValue().set(cacheKey, json, 10 + randomMinutes, TimeUnit.MINUTES);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Result.success(allSpots);
    }

    private int calculateTagMatchScore(String spotTags, String[] userTags) {
        if (StrUtil.isBlank(spotTags) || userTags == null) return 0;
        int score = 0;
        for (String uTag : userTags) {
            if (spotTags.contains(uTag.trim())) score++;
        }
        return score;
    }

    @GetMapping("/cities")
    public Result<List<String>> getCities() {
        List<Attraction> list = attractionService.list();
        List<String> cities = list.stream()
                .map(Attraction::getCity)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        return Result.success(cities);
    }

    @GetMapping("/detail/{id}")
    public Result<Attraction> getDetail(@PathVariable Long id) {
        return Result.success(attractionService.getById(id));
    }

    @RequireSuperAdmin
    @PostMapping("/add")
    public Result<?> add(@RequestBody Attraction attraction) {
        if (StrUtil.isBlank(attraction.getName())) return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "名称不能为空");
        if (StrUtil.isBlank(attraction.getAddress())) return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "地址必须强制填写");
        if (attraction.getTicketPrice() == null) return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "门票价格必须强制填写");
        if (attraction.getLatitude() == null || attraction.getLongitude() == null) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "经纬度必填(影响地图显示)");
        }

        if (attraction.getRating() == null) attraction.setRating(java.math.BigDecimal.valueOf(4.5));
        if (attraction.getCreateTime() == null) attraction.setCreateTime(java.time.LocalDateTime.now());
        if (StrUtil.isBlank(attraction.getContentText())) attraction.setContentText(attraction.getDescription());
        if (attraction.getIsHot() == null) attraction.setIsHot(0);

        attractionService.save(attraction);
        deleteListCache();
        return Result.success("入库成功");
    }

    @RequireSuperAdmin
    @PostMapping("/update")
    public Result<?> update(@RequestBody Attraction attraction) {
        if (attraction.getSpotId() == null) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "ID不能为空");
        }
        attractionService.updateById(attraction);
        deleteListCache();
        return Result.success("更新成功");
    }

    @RequireSuperAdmin
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        attractionService.removeById(id);
        deleteListCache();
        return Result.success("已删除");
    }

    @RequireSuperAdmin
    @PostMapping("/toggleHot")
    public Result<?> toggleHot(@RequestBody Attraction spot) {
        Attraction dbSpot = attractionService.getById(spot.getSpotId());
        if (dbSpot == null) return Result.error("404", "景点不存在");

        Integer current = dbSpot.getIsHot() == null ? 0 : dbSpot.getIsHot();
        dbSpot.setIsHot(current == 1 ? 0 : 1);

        attractionService.updateById(dbSpot);
        deleteListCache();
        return Result.success("操作成功");
    }

    private void deleteListCache() {
        try {
            // 清除默认列表缓存。大规模生产环境下建议使用 Redis 事件订阅机制或 Canal 监听 Binlog 进行精确失效
            redisTemplate.delete("attraction:list::");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}