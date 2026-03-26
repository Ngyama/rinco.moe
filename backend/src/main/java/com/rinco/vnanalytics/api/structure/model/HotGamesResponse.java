package com.rinco.vnanalytics.api.structure.model;

import java.util.List;

public record HotGamesResponse(int year, List<HotGameItem> games) {
}
