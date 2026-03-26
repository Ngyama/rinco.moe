package com.rinco.vnanalytics.api.user.model;

import java.util.List;

public record UserCollectionsResponse(String usernameOrId, List<UserCollectionItem> items) {}
