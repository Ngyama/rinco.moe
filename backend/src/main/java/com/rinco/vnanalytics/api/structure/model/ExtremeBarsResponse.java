package com.rinco.vnanalytics.api.structure.model;

import java.util.List;

public record ExtremeBarsResponse(
        int yearFrom,
        int yearTo,
        List<ExtremeBarItem> items
) {
}
