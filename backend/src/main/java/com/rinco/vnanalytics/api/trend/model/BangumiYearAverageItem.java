package com.rinco.vnanalytics.api.trend.model;

public record BangumiYearAverageItem(
        Integer year,
        Double avgScore,
        Integer count
) {
}
