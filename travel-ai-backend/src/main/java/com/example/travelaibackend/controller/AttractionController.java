package com.example.travelaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.entity.Attraction;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IAttractionService;
import com.example.travelaibackend.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attraction")
@CrossOrigin
public class AttractionController {

    @Autowired
    private IAttractionService attractionService;

    @Autowired
    private IUserService userService;

    // 1. 获取景点列表 (含排序逻辑)
    @GetMapping("/list")
    public Result<List<Attraction>> getList(@RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String city) {
        QueryWrapper<Attraction> query = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w.like("name", keyword).or().like("city", keyword));
        }
        if (city != null && !city.isEmpty()) {
            query.eq("city", city);
        }

        List<Attraction> allSpots = attractionService.list(query);

        SysUser user = userId != null ? userService.getById(userId) : null;
        String[] userTags = (user != null && user.getTags() != null) ? user.getTags().split(",") : null;

        allSpots.sort((s1, s2) -> {
            int hot1 = s1.getIsHot() == null ? 0 : s1.getIsHot();
            int hot2 = s2.getIsHot() == null ? 0 : s2.getIsHot();
            if (hot1 != hot2) return hot2 - hot1;

            if (userTags != null) {
                int score1 = calculateTagMatchScore(s1.getTags(), userTags);
                int score2 = calculateTagMatchScore(s2.getTags(), userTags);
                if (score1 != score2) return score2 - score1;
            }
            return s2.getRating().compareTo(s1.getRating());
        });

        return Result.success(allSpots);
    }

    private int calculateTagMatchScore(String spotTags, String[] userTags) {
        if (spotTags == null || userTags == null) return 0;
        int score = 0;
        for (String uTag : userTags) {
            if (spotTags.contains(uTag)) score++;
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

    // 4. 新增
    @PostMapping("/add")
    public Result<?> add(@RequestBody Attraction attraction) {
        if (attraction.getRating() == null) attraction.setRating(java.math.BigDecimal.valueOf(4.5));
        if (attraction.getCreateTime() == null) attraction.setCreateTime(java.time.LocalDateTime.now());
        if (attraction.getContentText() == null) attraction.setContentText(attraction.getDescription());
        if (attraction.getIsHot() == null) attraction.setIsHot(0);

        attractionService.save(attraction);
        return Result.success("入库成功");
    }

    // 5. 删除
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        attractionService.removeById(id);
        return Result.success("已删除");
    }

    // 6. 切换热门
    @PostMapping("/toggleHot")
    public Result<?> toggleHot(@RequestBody Attraction spot) {
        Attraction dbSpot = attractionService.getById(spot.getSpotId());
        if (dbSpot == null) return Result.error("404", "景点不存在");

        Integer current = dbSpot.getIsHot() == null ? 0 : dbSpot.getIsHot();
        dbSpot.setIsHot(current == 1 ? 0 : 1);

        attractionService.updateById(dbSpot);
        return Result.success("操作成功");
    }

    // 7. 修改景点信息 (解决 Req 2)
    @PostMapping("/update")
    public Result<?> update(@RequestBody Attraction attraction) {
        if (attraction.getSpotId() == null) {
            return Result.error("400", "ID不能为空");
        }
        boolean success = attractionService.updateById(attraction);
        return success ? Result.success("更新成功") : Result.error("500", "更新失败");
    }
}