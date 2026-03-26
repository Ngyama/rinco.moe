package com.rinco.vnanalytics.api.structure.model;

import java.util.List;

public record ControversyResponse(int yearFrom, int yearTo, List<ControversyItem> items) {
}
