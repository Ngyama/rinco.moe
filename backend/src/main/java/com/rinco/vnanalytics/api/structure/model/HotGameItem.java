package com.rinco.vnanalytics.api.structure.model;

public record HotGameItem(
        long bangumiSubjectId,
        String titleJp,
        Double score,
        Integer ratingTotal,
        String releaseDate,
        /** Std-dev of 1–10 score distribution; higher = more controversial. */
        double controversy
) {
}
