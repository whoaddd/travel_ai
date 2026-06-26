package com.whoa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whoa.service.ChatMessageService;
import com.whoa.utils.LLMUtils;
import com.whoa.vo.StreamChunkVO;
import com.whoa.vo.StreamDoneVO;
import com.whoa.vo.StreamErrorVO;
import com.whoa.vo.TravelRecommendVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TravelService {
    @Value("${spring.ai.openai.api-key}")
    private String apikey;
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.chat.model}")
    private String model;

    private LLMUtils llmUtils;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ChatMessageService chatMessageService;   // ← 新增注入

    @PostConstruct
    public void init() {
        this.llmUtils = new LLMUtils(apikey, baseUrl, model);
    }

    public TravelRecommendVO recommend(String city, Integer days, Double budget) {
        TravelRecommendVO result = new TravelRecommendVO();
        String prompt = buildTravelPrompt(city, days, budget);
        try {
            String response = llmUtils.chat(null, prompt);
            return parseTravelResponse(response);
        } catch (Exception e) {
            log.error("旅游推荐失败", e);
            result.setSuccess(false);
            result.setError("旅游推荐失败");
            return result;
        }
    }

    // 改造后的 chat 方法
    public SseEmitter chat(String message, Long userId, String sessionId) {
        SseEmitter emitter = new SseEmitter(180000L);

        new Thread(() -> {
            // 用 StringBuilder 拼装完整的 assistant 回复
            StringBuilder fullContent = new StringBuilder();

            try {
                String systemPrompt = "你是一个友好的旅游助手，请用中文回答用户关于旅游的问题";
                Consumer<String> callback = content -> {
                    try {
                        fullContent.append(content);   // ← 拼装完整内容
                        String json = objectMapper.writeValueAsString(StreamChunkVO.of(content));
                        emitter.send(SseEmitter.event().data(json));
                    } catch (Exception e) {
                        log.error("发送消息失败", e);
                    }
                };
                llmUtils.chatStream(systemPrompt, message, callback);

                // 流完成后保存 assistant 回复
                chatMessageService.saveMessage(userId, sessionId, "assistant", fullContent.toString());
                log.info("对话回复已保存: userId={}, sessionId={}", userId, sessionId);

                String doneJson = objectMapper.writeValueAsString(StreamDoneVO.of());
                emitter.send(SseEmitter.event().data(doneJson));
                emitter.complete();
            } catch (Exception e) {
                log.error("对话异常", e);
                try {
                    String errorJson = objectMapper.writeValueAsString(StreamErrorVO.of(e.getMessage()));
                    emitter.send(SseEmitter.event().data(errorJson));
                } catch (Exception e1) {
                    log.error("发送错误消息失败", e1);
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    // 以下方法保持不变
    private TravelRecommendVO parseTravelResponse(String response) {
        TravelRecommendVO result = new TravelRecommendVO();
        try {
            String jsonContent = extractJson(response);
            if (jsonContent != null) {
                result = objectMapper.readValue(jsonContent, TravelRecommendVO.class);
            } else {
                result.setSuccess(false);
                result.setError("未能从响应中提取JSON");
                result.setRawResponse(response);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("JSON解析错误");
            result.setRawResponse(response);
        }
        return result;
    }

    private String extractJson(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String buildTravelPrompt(String city, Integer days, Double budget) {
        return "你是一个专业的旅游规划师,请根据用户的需求生成详细的旅行行程。\n\n" +
                "请根据以下信息为用户生成一份详细的旅游规划:\n" +
                "- 目的地城市:" + city + "\n" +
                "-预算:" + budget + "元\n" +
                "-旅行天数:" + days + "天\n\n" +
                "要求:\n" +
                "1. 每天的行程安排(上午、下午、晚上)\n" +
                "2. 每个景点的详细介绍\n" +
                "3. 交通建议\n" +
                "4. 预算分配明细\n" +
                "5. 注意事项\n\n" +
                "请以JSON格式输出,结构如下:\n" +
                "{\"success\":true,\"city\":\"城市名\",\"days\":天数,\"totalBudget\":总预算," +
                "\"dailyItinerary\":[{\"day\":1,\"date\":\"第1天\"," +
                "\"morning\":{\"spot\":\"景点名称\",\"duration\":\"游览时长\",\"ticket\":\"门票价格\"," +
                "\"transportation\":\"交通方式\",\"description\":\"景点介绍\"}," +
                "\"afternoon\":{\"spot\":\"景点名称\",\"duration\":\"游览时长\",\"ticket\":\"门票价格\"," +
                "\"transportation\":\"交通方式\",\"description\":\"景点介绍\"}," +
                "\"evening\":{\"spot\":\"活动名称\",\"duration\":\"活动时长\",\"ticket\":\"费用\"," +
                "\"transportation\":\"交通方式\",\"description\":\"活动介绍\"}}]," +
                "\"budgetBreakdown\":{\"accommodation\":住宿费用,\"food\":餐饮费用," +
                "\"transportation\":交通费用,\"tickets\":门票费用,\"other\":其他费用}," +
                "\"tips\":[\"提示1\",\"提示2\",\"提示3\"],\"warnings\":[\"注意事项1\",\"注意事项2\"]}\n\n" +
                "请确保JSON格式正确,可以被解析。";
    }
}