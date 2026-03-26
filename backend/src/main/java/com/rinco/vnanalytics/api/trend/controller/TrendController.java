package com.rinco.vnanalytics.api.trend.controller;

import com.rinco.vnanalytics.api.trend.model.BangumiTrendResponse;
import com.rinco.vnanalytics.api.trend.service.TrendService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/trend")
public class TrendController {

    private final TrendService trendService;

    public TrendController(TrendService trendService) {
        this.trendService = trendService;
    }

    @GetMapping("/bangumi/history")
    public BangumiTrendResponse getBangumiHistory(
            @RequestParam(defaultValue = "50") @Min(0) @Max(5000) int minVotes
    ) {
        return trendService.fetchBangumiYearTrend(minVotes);
    }
}
