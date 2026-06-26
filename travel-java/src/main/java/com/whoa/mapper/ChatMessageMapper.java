package com.whoa.mapper;

import com.whoa.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper {

    void insert(ChatMessage chatMessage);

    List<ChatMessage> findByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);

    List<Map<String, Object>> findSessionsByUserId(@Param("userId") Long userId);

    void deleteBySessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);

}
