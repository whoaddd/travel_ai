package com.whoa.controller;

import com.whoa.dto.ChatRequestDTO;
import com.whoa.entity.ChatMessage;
import com.whoa.service.ChatMessageService;
import com.whoa.service.TravelService;
import com.whoa.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final TravelService travelService;
    private final ChatMessageService chatMessageService;

    // 发起对话（流式）
    @PostMapping(value = "/send", produces = "text/event-stream")
    public SseEmitter chat(@Valid @RequestBody ChatRequestDTO chatRequestDTO,
                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String sessionId = chatRequestDTO.getSessionId();
        String message = chatRequestDTO.getMessage();

        // 保存用户消息
        chatMessageService.saveMessage(userId, sessionId, "user", message);

        // 调用已有的 chat 方法，但传入 userId 和 sessionId 用于保存回复
        return travelService.chat(message, userId, sessionId);
    }

    // 获取会话列表
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> sessions = chatMessageService.getSessions(userId);
        return Result.ok(sessions);
    }

    // 获取某会话的消息列表
    @GetMapping("/messages")
    public Result<List<ChatMessage>> messages(HttpServletRequest request,
                                              @RequestParam String sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        List<ChatMessage> messages = chatMessageService.getMessages(userId, sessionId);
        return Result.ok(messages);
    }

    // 删除会话
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(HttpServletRequest request,
                                      @PathVariable String sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        chatMessageService.deleteSession(userId, sessionId);
        return Result.ok();
    }
}
