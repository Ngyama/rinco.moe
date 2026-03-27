package com.rinco.vnanalytics.api.global.model;

/**
 * Combined score: 50% Bangumi + 25% VNDB + 25% EGS (only rows with all three scores).
 */
public record GlobalCombinedRankItem(
        Integer rank,
        String titleJp,
        Double bangumiScore,
        Double vndbScore,
        Double egsScore,
        Double combinedScore,
        Long matchTripleId
) {
}
