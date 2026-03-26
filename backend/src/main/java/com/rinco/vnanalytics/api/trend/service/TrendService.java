package com.rinco.vnanalytics.api.trend.service;

import com.rinco.vnanalytics.api.global.mapper.BangumiRankingMapper;
import com.rinco.vnanalytics.api.global.policy.GlobalRankingPolicy;
import com.rinco.vnanalytics.api.trend.model.BangumiTrendResponse;
import com.rinco.vnanalytics.api.trend.model.BangumiYearAverageItem;
import com.rinco.vnanalytics.api.trend.model.BangumiYearPointItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrendService {

    private final BangumiRankingMapper bangumiRankingMapper;

    public TrendService(BangumiRankingMapper bangumiRankingMapper) {
        this.bangumiRankingMapper = bangumiRankingMapper;
    }

    public BangumiTrendResponse fetchBangumiYearTrend(int minVotes) {
        int safeMinVotes = Math.max(0, minVotes);
        List<BangumiYearAverageItem> averages = bangumiRankingMapper.queryTrendYearAverages(
                safeMinVotes,
                GlobalRankingPolicy.EDITION_EXCLUSION_REGEX
        ).stream().map(row -> new BangumiYearAverageItem(
                (Integer) row[0],
                (Double) row[1],
                (Integer) row[2]
        )).toList();

        List<BangumiYearPointItem> points = bangumiRankingMapper.queryTrendPoints(
                safeMinVotes,
                GlobalRankingPolicy.EDITION_EXCLUSION_REGEX
        ).stream().map(row -> new BangumiYearPointItem(
                (Integer) row[0],
                (String) row[1],
                (String) row[2],
                (Double) row[3]
        )).toList();

        return new BangumiTrendResponse(safeMinVotes, averages, points);
    }
}
