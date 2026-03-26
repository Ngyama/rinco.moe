package com.rinco.vnanalytics.api.structure.model;

import java.util.List;

public record HotGameDetail(
        long bangumiSubjectId,
        String titleJp,
        Double score,
        Integer ratingTotal,
        String releaseDate,
        double median,
        int mode,
        double stdDev,
        double extremeRatio,
        List<Integer> ratingCounts
) {
}
