package com.example.travelaibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.travelaibackend.entity.Attraction; // 注意引入的是景点实体
import com.example.travelaibackend.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    // 核心 SQL：联表查询，找出某个用户收藏的所有景点详细信息
    @Select("SELECT a.* FROM attraction a " +
            "INNER JOIN user_favorite f ON a.spot_id = f.spot_id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.create_time DESC")
    List<Attraction> selectUserFavoriteSpots(@Param("userId") Long userId);
}