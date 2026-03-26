package com.rinco.vnanalytics.api.trend.model;

public record BangumiYearPointItem(
        Integer year,
        String releaseDate,
        String title,
        Double score
) {
}
