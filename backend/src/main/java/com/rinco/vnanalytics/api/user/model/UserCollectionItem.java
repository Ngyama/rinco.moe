package com.rinco.vnanalytics.api.user.model;

public record UserCollectionItem(
        long subjectId,
        String subjectName,
        String subjectNameCn,
        int rate,
        String type,
        String comment
) {}
