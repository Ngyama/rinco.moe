package com.rinco.vnanalytics.api.global.model;

import java.util.List;

public record BangumiGlobalResponse(
        Integer total,
        Integer limit,
        Integer offset,
        List<BangumiGameItem> items
) {
}
