package com.rinco.vnanalytics.api.structure.model;

public record ControversyItem(
        long bangumiSubjectId,
        String titleJp,
        double score,
        int ratingTotal,
        double controversy
) {
}
