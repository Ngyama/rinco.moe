package com.rinco.vnanalytics.api.global.model;

import java.util.List;

public record GlobalDualTopResponse(
        Integer limit,
        List<GlobalSiteScoreItem> bangumi,
        List<GlobalSiteScoreItem> vndb,
        List<GlobalSiteScoreItem> egs
) {
}
