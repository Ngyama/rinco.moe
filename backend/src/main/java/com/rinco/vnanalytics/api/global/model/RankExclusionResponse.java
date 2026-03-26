package com.rinco.vnanalytics.api.global.model;

public record RankExclusionResponse(
        String status,
        String platform,
        String subjectId,
        Boolean excluded,
        Integer updated
) {
}
