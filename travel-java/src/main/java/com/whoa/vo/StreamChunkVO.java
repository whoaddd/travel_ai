package com.whoa.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StreamChunkVO {
    private String content;
    private String type = "chunk";

    public static StreamChunkVO of(String content) {
        return new StreamChunkVO(content, "chunk");
    }
}
