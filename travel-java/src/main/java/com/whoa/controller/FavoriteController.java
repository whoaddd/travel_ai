package com.whoa.controller;

import com.whoa.dto.FavoriteDTO;
import com.whoa.entity.Favorite;
import com.whoa.service.FavoriteService;
import com.whoa.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    // 添加收藏
    @PostMapping
    public Result<Void> add(@Valid @RequestBody FavoriteDTO favoriteDTO,
                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.addFavorite(
                userId,
                favoriteDTO.getType(),
                favoriteDTO.getTitle(),
                favoriteDTO.getContent()
        );
        return Result.ok();
    }

    // 收藏列表（可选按类型筛选）
    @GetMapping
    public Result<List<Favorite>> list(HttpServletRequest request,
                                       @RequestParam(required = false) String type) {
        Long userId = (Long) request.getAttribute("userId");
        List<Favorite> favorites = favoriteService.listFavorites(userId, type);
        return Result.ok(favorites);
    }

    // 取消收藏
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.removeFavorite(userId, id);
        return Result.ok();
    }
}
