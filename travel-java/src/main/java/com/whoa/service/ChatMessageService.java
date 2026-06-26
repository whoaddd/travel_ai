package com.whoa.service;

import com.whoa.entity.ChatMessage;
import com.whoa.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageMapper chatMessageMapper;

    // 保存一条消息
    public void saveMessage(Long userId, String sessionId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        chatMessageMapper.insert(msg);
        log.debug("保存消息: userId={}, sessionId={}, role={}", userId, sessionId, role);
    }

    // 获取用户的所有会话列表
    public List<Map<String, Object>> getSessions(Long userId) {
        return chatMessageMapper.findSessionsByUserId(userId);
    }

    // 获取某个会话的消息列表
    public List<ChatMessage> getMessages(Long userId, String sessionId) {
        return chatMessageMapper.findByUserIdAndSessionId(userId, sessionId);
    }

    // 删除某个会话
    public void deleteSession(Long userId, String sessionId) {
        chatMessageMapper.deleteBySessionId(userId, sessionId);
        log.info("删除会话: userId={}, sessionId={}", userId, sessionId);
    }
}
