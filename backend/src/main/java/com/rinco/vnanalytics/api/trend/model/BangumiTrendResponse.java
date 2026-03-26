package com.rinco.vnanalytics.api.trend.model;

import java.util.List;

public record BangumiTrendResponse(
        Integer minVotes,
        List<BangumiYearAverageItem> averages,
        List<BangumiYearPointItem> points
) {
}
