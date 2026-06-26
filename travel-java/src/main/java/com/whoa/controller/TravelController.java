package com.whoa.controller;

import com.whoa.dto.TravelRequestDTO;
import com.whoa.service.TravelService;
import com.whoa.vo.Result;
import com.whoa.vo.TravelRecommendVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.ok();
    }

    @PostMapping("/recommend")
    public Result<TravelRecommendVO> recommend(@Valid @RequestBody TravelRequestDTO travelRequestDTO) {
        log.info("旅游推荐请求: city={}, days={}, budget={}",
                travelRequestDTO.getCity(), travelRequestDTO.getDays(), travelRequestDTO.getBudget());
        TravelRecommendVO travelRecommendVO = travelService.recommend(
                travelRequestDTO.getCity(),
                travelRequestDTO.getDays(),
                travelRequestDTO.getBudget()
        );
        return Result.ok(travelRecommendVO);
    }

}