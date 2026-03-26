package com.rinco.vnanalytics.api.global.model;

public record GlobalSiteScoreItem(
        Integer rank,
        String titleJp,
        Double score,
        /** vn_match_triple.id when this subject appears in the match table; null otherwise */
        Long matchTripleId
) {
}
