package com.rinco.vnanalytics.api.global.client;

import com.rinco.vnanalytics.api.global.model.BangumiGameItem;

import java.util.List;

public record BangumiSearchResult(
        Integer total,
        List<BangumiGameItem> items
) {
}
