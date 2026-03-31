package com.rinco.vnanalytics.api.structure.model;

/**
 * Shares of 1–2★ and 9–10★ among all votes (same year filter as controversy list).
 */
public record ExtremeBarItem(
        long bangumiSubjectId,
        String titleJp,
        int ratingTotal,
        double lowShare,
        double highShare
) {
}
