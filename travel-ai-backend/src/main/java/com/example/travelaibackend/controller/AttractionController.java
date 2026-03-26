package com.example.travelaibackend.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.annotation.RequireAdmin;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.common.ResultCode;
import com.example.travelaibackend.entity.Attraction;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IAttractionService;
import com.example.travelaibackend.service.IUserService;
import com.example.travelaibackend.utils.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city) {

        Long userId = UserContext.getUserId();
        String cacheKey = "attraction:list:all:" + StrUtil.nullToEmpty(keyword) + ":" + StrUtil.nullToEmpty(city);

        List<Attraction> allSpots = null;

        if (userId == null && Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            try {
                String json = redisTemplate.opsForValue().get(cacheKey);
                if ("[]".equals(json)) {
                    allSpots = new ArrayList<>();
                } else if (StrUtil.isNotBlank(json)) {
                    allSpots = mapper.readValue(json, new TypeReference<List<Attraction>>() {});
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (allSpots == null) {
            QueryWrapper<Attraction> query = new QueryWrapper<>();
            if (StrUtil.isNotBlank(keyword)) {
                query.and(w -> w.like("name", keyword).or().like("city", keyword).or().like("tags", keyword));
            }
            if (StrUtil.isNotBlank(city)) {
                query.eq("city", city);
            }
            allSpots = attractionService.list(query);

            if (allSpots == null || allSpots.isEmpty()) {
                if (userId == null) {
                    redisTemplate.opsForValue().set(cacheKey, "[]", 60, TimeUnit.SECONDS);
                }
                Map<String, Object> emptyRes = new HashMap<>();
                emptyRes.put("total", 0);
                emptyRes.put("list", new ArrayList<>());
                return Result.success(emptyRes);
            }

            if (userId == null) {
                try {
                    String json = mapper.writeValueAsString(allSpots);
                    long randomMinutes = ThreadLocalRandom.current().nextLong(5);
                    redisTemplate.opsForValue().set(cacheKey, json, 10 + randomMinutes, TimeUnit.MINUTES);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

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

        int total = allSpots.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Attraction> pageList = fromIndex < total ? allSpots.subList(fromIndex, toIndex) : new ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", pageList);

        return Result.success(result);
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
        Attraction spot = attractionService.getById(id);
        if (spot != null) {
            redisTemplate.opsForValue().increment("attraction:views:" + id);
        }
        return Result.success(spot);
    }

    @RequireAdmin
    @PostMapping("/add")
    public Result<?> add(@RequestBody Attraction attraction) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        boolean isAdmin = user != null && "admin".equals(user.getRole());

        // 业务层兜底拦截
        if (userId == null || (userId != 88L && !isAdmin)) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：新增资源仅管理员及最高统帅(ID:88)可用");
        }

        if (StrUtil.isBlank(attraction.getName())) return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "名称不能为空");
        if (StrUtil.isBlank(attraction.getAddress())) return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "地址必须强制填写");
        if (attraction.getTicketPrice() == null) return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "门票价格必须强制填写");
        if (attraction.getLatitude() == null || attraction.getLongitude() == null) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "经纬度必填");
        }

        if (attraction.getRating() == null) attraction.setRating(java.math.BigDecimal.valueOf(4.5));
        if (attraction.getCreateTime() == null) attraction.setCreateTime(java.time.LocalDateTime.now());
        if (StrUtil.isBlank(attraction.getContentText())) attraction.setContentText(attraction.getDescription());
        if (attraction.getIsHot() == null) attraction.setIsHot(0);

        attractionService.save(attraction);
        deleteListCache();
        return Result.success("入库成功");
    }

    @RequireAdmin
    @PostMapping("/update")
    public Result<?> update(@RequestBody Attraction attraction) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        boolean isAdmin = user != null && "admin".equals(user.getRole());

        if (userId == null || (userId != 88L && !isAdmin)) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：修改资源仅管理员及最高统帅(ID:88)可用");
        }

        if (attraction.getSpotId() == null) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "ID不能为空");
        }
        attractionService.updateById(attraction);
        deleteListCache();
        return Result.success("更新成功");
    }

    @RequireAdmin
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        boolean isAdmin = user != null && "admin".equals(user.getRole());

        if (userId == null || (userId != 88L && !isAdmin)) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：删除资源仅管理员及最高统帅(ID:88)可用");
        }

        attractionService.removeById(id);
        deleteListCache();
        return Result.success("已删除");
    }

    @RequireAdmin
    @PostMapping("/toggleHot")
    public Result<?> toggleHot(@RequestBody Attraction spot) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        boolean isAdmin = user != null && "admin".equals(user.getRole());

        if (userId == null || (userId != 88L && !isAdmin)) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "业务层越权拦截：核心操作仅管理员及最高统帅(ID:88)可用");
        }

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
            redisTemplate.delete("attraction:list:all::");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}