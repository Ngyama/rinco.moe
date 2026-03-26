package com.rinco.vnanalytics.api.global.policy;

import org.springframework.util.StringUtils;

public enum RankPlatform {
    BANGUMI,
    VNDB,
    EGS;

    public static RankPlatform parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("platform is required");
        }
        try {
            return RankPlatform.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("platform must be bangumi, vndb or egs");
        }
    }
}
