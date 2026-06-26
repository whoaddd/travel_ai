package com.whoa.mapper;

import com.whoa.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper {
    void insert(Favorite favorite);

    List<Favorite> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    List<Favorite> findByUserId(@Param("userId") Long userId);

    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
