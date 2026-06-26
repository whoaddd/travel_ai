package com.whoa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FavoriteDTO {
    @NotBlank(message = "收藏类型不能为空")
    private String type;    // itinerary 或 message

    private String title;

    @NotBlank(message = "收藏内容不能为空")
    private String content;
}
