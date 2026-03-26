package com.rinco.vnanalytics.api.staff.model;

public record StaffPersonItem(
        long bangumiPersonId,
        String name,
        String nameCn,
        String nameJp,
        String imageUrl,
        String relation
) {}
