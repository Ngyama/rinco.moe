package com.rinco.vnanalytics.api.global.policy;

public final class GlobalRankingPolicy {

    private GlobalRankingPolicy() {
    }

    public static final int MIN_BANGUMI_VOTES_FOR_LIST = 100;
    public static final int MIN_VOTES_FOR_TOP = 50;

    public static final int MIN_TOP_LIMIT = 1;
    public static final int MAX_TOP_LIMIT = 200;

    public static final int MIN_SYNC_LIMIT = 20;
    public static final int MAX_SYNC_LIMIT = 200;

    public static final String EDITION_EXCLUSION_REGEX =
            "(HD版|HD\\s*Remaster|Premium\\s*Edition|Complete\\s*Edition|Latest\\s*Edition|DVD版|BD版|Blu-?ray版|"
                    + "全年齢版|新装版|廉価版|移植版|Portable版|Console版|International\\s*Edition|Definitive\\s*Edition|"
                    + "Special\\s*Edition|Deluxe\\s*Edition|Ultimate\\s*Edition|Perfect\\s*Edition|"
                    + "Director'?s\\s*Cut|Full\\s*Voice|フールボイス|フルボイス)";
}
