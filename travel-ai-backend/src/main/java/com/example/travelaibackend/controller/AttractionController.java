package com.example.travelaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.entity.Attraction;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IAttractionService;
import com.example.travelaibackend.service.IUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    // Jackson 对象映射器，用于序列化/反序列化 Redis 数据
    private final ObjectMapper mapper = new ObjectMapper();

    // 1. 获取景点列表 (集成 Redis 缓存 + 修复标签推荐 + 搜索)
    @GetMapping("/list")
    public Result<List<Attraction>> getList(@RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String city) {
        // 定义缓存 Key (格式：attraction:list:keyword:city)
        // 注意：个性化推荐(userId != null) 时不读写此通用缓存
        String cacheKey = "attraction:list:" + (keyword == null ? "" : keyword) + ":" + (city == null ? "" : city);

        // 1. 尝试查 Redis 缓存 (仅在无用户个性化参数时)
        if (userId == null && Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            try {
                String json = redisTemplate.opsForValue().get(cacheKey);
                if (json != null && !json.isEmpty()) {
                    List<Attraction> list = mapper.readValue(json, new TypeReference<List<Attraction>>() {});
                    return Result.success(list);
                }
            } catch (Exception e) {
                // 缓存读取出错不应阻断业务，降级查库
                e.printStackTrace();
            }
        }

        // 2. 查数据库
        QueryWrapper<Attraction> query = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w.like("name", keyword)
                    .or().like("city", keyword)
                    .or().like("tags", keyword));
        }
        if (city != null && !city.isEmpty()) {
            query.eq("city", city);
        }

        List<Attraction> allSpots = attractionService.list(query);

        // 3. 排序逻辑 (热门 -> 标签 -> 评分)
        if (userId != null) {
            SysUser user = userService.getById(userId);
            if (user != null && user.getTags() != null) {
                // 🔥 修复：兼容中英文逗号，防止标签分割失败
                String[] userTags = user.getTags().replace("，", ",").split(",");

                allSpots.sort((s1, s2) -> {
                    // 优先级 1: 热门置顶
                    int hot1 = s1.getIsHot() == null ? 0 : s1.getIsHot();
                    int hot2 = s2.getIsHot() == null ? 0 : s2.getIsHot();
                    if (hot1 != hot2) return hot2 - hot1;

                    // 优先级 2: 标签匹配度 (个性化)
                    int score1 = calculateTagMatchScore(s1.getTags(), userTags);
                    int score2 = calculateTagMatchScore(s2.getTags(), userTags);
                    if (score1 != score2) return score2 - score1;

                    // 优先级 3: 评分
                    return s2.getRating().compareTo(s1.getRating());
                });
            }
        } else {
            // 未登录排序: 热门 -> 评分
            allSpots.sort((s1, s2) -> {
                int hot1 = s1.getIsHot() == null ? 0 : s1.getIsHot();
                int hot2 = s2.getIsHot() == null ? 0 : s2.getIsHot();
                if (hot1 != hot2) return hot2 - hot1;
                return s2.getRating().compareTo(s1.getRating());
            });
        }

        // 4. 写入缓存 (仅缓存通用结果，过期时间 10 分钟)
        if (userId == null) {
            try {
                String json = mapper.writeValueAsString(allSpots);
                redisTemplate.opsForValue().set(cacheKey, json, 10, TimeUnit.MINUTES);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Result.success(allSpots);
    }

    // 计算标签匹配分数
    private int calculateTagMatchScore(String spotTags, String[] userTags) {
        if (spotTags == null || userTags == null) return 0;
        int score = 0;
        for (String uTag : userTags) {
            // 清除空白字符，提高匹配准确率
            if (spotTags.contains(uTag.trim())) score++;
        }
        return score;
    }

    // 2. 获取城市列表 (去重)
    @GetMapping("/cities")
    public Result<List<String>> getCities() {
        List<Attraction> list = attractionService.list();
        List<String> cities = list.stream()
                .map(Attraction::getCity)
                .filter(city -> city != null && !city.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        return Result.success(cities);
    }

    // 3. 详情
    @GetMapping("/detail/{id}")
    public Result<Attraction> getDetail(@PathVariable Long id) {
        return Result.success(attractionService.getById(id));
    }

    // 4. 新增 (增加强制校验 + 清除缓存)
    @PostMapping("/add")
    public Result<?> add(@RequestBody Attraction attraction) {
        // 🔥 强制校验逻辑
        if (attraction.getName() == null || attraction.getName().isEmpty())
            return Result.error("400", "名称不能为空");
        if (attraction.getAddress() == null || attraction.getAddress().isEmpty())
            return Result.error("400", "地址必须强制填写");
        if (attraction.getTicketPrice() == null)
            return Result.error("400", "门票价格必须强制填写");
        // 经纬度校验，确保地图功能正常
        if (attraction.getLatitude() == null || attraction.getLongitude() == null)
            return Result.error("400", "经纬度必填(影响地图显示)");

        // 默认值填充
        if (attraction.getRating() == null) attraction.setRating(java.math.BigDecimal.valueOf(4.5));
        if (attraction.getCreateTime() == null) attraction.setCreateTime(java.time.LocalDateTime.now());
        if (attraction.getContentText() == null) attraction.setContentText(attraction.getDescription());
        if (attraction.getIsHot() == null) attraction.setIsHot(0);

        attractionService.save(attraction);

        // 数据变更，清除缓存
        deleteListCache();
        return Result.success("入库成功");
    }

    // 5. 修改景点信息 (清除缓存)
    @PostMapping("/update")
    public Result<?> update(@RequestBody Attraction attraction) {
        if (attraction.getSpotId() == null) {
            return Result.error("400", "ID不能为空");
        }
        attractionService.updateById(attraction);
        deleteListCache();
        return Result.success("更新成功");
    }

    // 6. 删除 (清除缓存)
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        attractionService.removeById(id);
        deleteListCache();
        return Result.success("已删除");
    }

    // 7. 切换热门 (清除缓存)
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

    // 辅助方法：清除列表缓存
    private void deleteListCache() {
        try {
            // 删除最常用的默认列表缓存
            redisTemplate.delete("attraction:list::");
            // 注：为了性能考虑，此处只删除了无参查询的缓存。
            // 生产环境中可能需要配合 Keys 模糊匹配删除所有 attraction:list:*，
            // 但 RedisTemplate 并不建议在生产环境频繁使用 keys 指令。
            // 对于带参数的查询，目前依赖 10 分钟自动过期。
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}