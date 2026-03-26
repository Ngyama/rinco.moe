package com.rinco.vnanalytics.api.structure.service;

import com.rinco.vnanalytics.api.structure.model.ControversyItem;
import com.rinco.vnanalytics.api.structure.model.ControversyResponse;
import com.rinco.vnanalytics.api.structure.model.HotGameDetail;
import com.rinco.vnanalytics.api.structure.model.HotGameItem;
import com.rinco.vnanalytics.api.structure.model.HotGamesResponse;
import com.rinco.vnanalytics.api.structure.mapper.StructureMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StructureService {

    private final StructureMapper structureMapper;

    public StructureService(StructureMapper structureMapper) {
        this.structureMapper = structureMapper;
    }

    public HotGamesResponse fetchHotGames(int year, int limit) {
        List<Object[]> rows = structureMapper.queryHotGames(year, limit);
        List<HotGameItem> games = new ArrayList<>();
        for (Object[] r : rows) {
            games.add(new HotGameItem(
                    (Long) r[0],
                    (String) r[1],
                    r[2] != null ? ((Number) r[2]).doubleValue() : null,
                    (Integer) r[3],
                    (String) r[4]
            ));
        }
        return new HotGamesResponse(year, games);
    }

    public HotGameDetail fetchHotGameDetail(long bangumiSubjectId) {
        Object[] r = structureMapper.queryHotGameDetail(bangumiSubjectId);
        if (r == null) {
            return null;
        }
        int[] counts = new int[10];
        for (int i = 0; i < 10; i++) {
            counts[i] = ((Number) r[5 + i]).intValue();
        }
        int total = counts[0] + counts[1] + counts[2] + counts[3] + counts[4]
                + counts[5] + counts[6] + counts[7] + counts[8] + counts[9];
        double score = r[2] != null ? ((Number) r[2]).doubleValue() : 0;
        double median = computeMedian(counts, total);
        int mode = computeMode(counts);
        double stdDev = computeStdDev(counts, total);
        double extremeRatio = computeExtremeRatio(counts, total);
        List<Integer> ratingCounts = new ArrayList<>();
        for (int c : counts) {
            ratingCounts.add(c);
        }
        return new HotGameDetail(
                (Long) r[0],
                (String) r[1],
                score,
                (Integer) r[3],
                (String) r[4],
                median,
                mode,
                stdDev,
                extremeRatio,
                ratingCounts
        );
    }

    public ControversyResponse fetchControversy(int yearFrom, int yearTo, int minVotes) {
        List<Object[]> rows = structureMapper.queryControversy(yearFrom, yearTo, minVotes);
        List<ControversyItem> items = new ArrayList<>();
        for (Object[] r : rows) {
            int[] counts = new int[10];
            for (int i = 0; i < 10; i++) {
                counts[i] = ((Number) r[4 + i]).intValue();
            }
            int total = ((Number) r[3]).intValue();
            double controversy = computeStdDev(counts, total);
            items.add(new ControversyItem(
                    (Long) r[0],
                    (String) r[1],
                    ((Number) r[2]).doubleValue(),
                    total,
                    controversy
            ));
        }
        return new ControversyResponse(yearFrom, yearTo, items);
    }

    private double computeMedian(int[] counts, int total) {
        if (total <= 0) return 0;
        int half = (total + 1) / 2;
        int acc = 0;
        for (int score = 1; score <= 10; score++) {
            acc += counts[score - 1];
            if (acc >= half) {
                return score;
            }
        }
        return 10;
    }

    private int computeMode(int[] counts) {
        int maxIdx = 0;
        for (int i = 1; i < 10; i++) {
            if (counts[i] > counts[maxIdx]) {
                maxIdx = i;
            }
        }
        return maxIdx + 1;
    }

    private double computeStdDev(int[] counts, int total) {
        if (total <= 0) return 0;
        double mean = 0;
        for (int i = 0; i < 10; i++) {
            mean += (i + 1) * counts[i];
        }
        mean /= total;
        double variance = 0;
        for (int i = 0; i < 10; i++) {
            double diff = (i + 1) - mean;
            variance += diff * diff * counts[i];
        }
        variance /= total;
        return Math.sqrt(variance);
    }

    private double computeExtremeRatio(int[] counts, int total) {
        if (total <= 0) return 0;
        int extreme = counts[0] + counts[9]; // 1分 + 10分
        return (double) extreme / total;
    }
}
