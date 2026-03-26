package com.rinco.vnanalytics.api.global.model;

public record BangumiGameItem(
        Long id,
        String name,
        String nameCn,
        String date,
        Integer rank,
        Double score,
        Integer votes,
        String url
) {
}
