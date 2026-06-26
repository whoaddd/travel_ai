package com.whoa.service;

import com.whoa.entity.Favorite;
import com.whoa.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteMapper favoriteMapper;

    // 添加收藏
    public void addFavorite(Long userId, String type, String title, String content) {
        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setType(type);
        fav.setTitle(title);
        fav.setContent(content);
        favoriteMapper.insert(fav);
        log.info("添加收藏: userId={}, type={}", userId, type);
    }

    // 查收藏列表（可按类型筛选）
    public List<Favorite> listFavorites(Long userId, String type) {
        if (type != null && !type.isEmpty()) {
            return favoriteMapper.findByUserIdAndType(userId, type);
        }
        return favoriteMapper.findByUserId(userId);
    }

    // 取消收藏
    public void removeFavorite(Long userId, Long id) {
        favoriteMapper.deleteByIdAndUserId(id, userId);
        log.info("取消收藏: userId={}, favoriteId={}", userId, id);
    }

}
