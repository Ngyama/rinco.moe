package com.rinco.vnanalytics.api.global.service;

import com.rinco.vnanalytics.api.global.client.BangumiApiClient;
import com.rinco.vnanalytics.api.global.client.BangumiSearchResult;
import com.rinco.vnanalytics.api.global.mapper.BangumiRankingMapper;
import com.rinco.vnanalytics.api.global.mapper.EgsRankingMapper;
import com.rinco.vnanalytics.api.global.mapper.GlobalSchemaMapper;
import com.rinco.vnanalytics.api.global.mapper.VndbRankingMapper;
import com.rinco.vnanalytics.api.global.model.BangumiGameItem;
import com.rinco.vnanalytics.api.global.model.BangumiGlobalResponse;
import com.rinco.vnanalytics.api.global.model.GlobalCombinedRankItem;
import com.rinco.vnanalytics.api.global.model.GlobalDualTopResponse;
import com.rinco.vnanalytics.api.global.model.GlobalSiteScoreItem;
import com.rinco.vnanalytics.api.global.policy.GlobalRankingPolicy;
import com.rinco.vnanalytics.api.global.policy.RankPlatform;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
public class GlobalRankingService {

    private final BangumiApiClient bangumiApiClient;
    private final BangumiRankingMapper bangumiRankingMapper;
    private final VndbRankingMapper vndbRankingMapper;
    private final EgsRankingMapper egsRankingMapper;

    public GlobalRankingService(
            BangumiApiClient bangumiApiClient,
            BangumiRankingMapper bangumiRankingMapper,
            VndbRankingMapper vndbRankingMapper,
            EgsRankingMapper egsRankingMapper,
            GlobalSchemaMapper globalSchemaMapper
    ) {
        this.bangumiApiClient = bangumiApiClient;
        this.bangumiRankingMapper = bangumiRankingMapper;
        this.vndbRankingMapper = vndbRankingMapper;
        this.egsRankingMapper = egsRankingMapper;
        bangumiRankingMapper.ensureBangumiSubjectTable();
        globalSchemaMapper.ensureRankingExcludeColumns();
        globalSchemaMapper.ensureMatchTripleCombinedWeightedColumn();
    }

    public BangumiGlobalResponse fetchGameTopList(int limit, int offset) {
        bangumiRankingMapper.ensureBangumiSubjectTable();
        return fetchFromLocalCache(limit, offset);
    }

    public GlobalDualTopResponse fetchDualTop(int limit, boolean matchedOnly) {
        int safeLimit = Math.min(GlobalRankingPolicy.MAX_TOP_LIMIT, Math.max(GlobalRankingPolicy.MIN_TOP_LIMIT, limit));
        String regex = GlobalRankingPolicy.EDITION_EXCLUSION_REGEX;
        int minVotes = GlobalRankingPolicy.MIN_VOTES_FOR_TOP;
        if (matchedOnly) {
            bangumiRankingMapper.refreshCombinedWeightedScores();
            List<GlobalSiteScoreItem> bangumi = bangumiRankingMapper.queryCuratedPoolBangumi(safeLimit);
            List<GlobalSiteScoreItem> vndb = vndbRankingMapper.queryCuratedPoolVndb(safeLimit);
            List<GlobalSiteScoreItem> egs = egsRankingMapper.queryCuratedPoolEgs(safeLimit);
            List<GlobalCombinedRankItem> combinedTop10 = bangumiRankingMapper.queryCuratedPoolCombinedTop(10);
            return new GlobalDualTopResponse(safeLimit, bangumi, vndb, egs, combinedTop10);
        }
        List<GlobalSiteScoreItem> bangumi = bangumiRankingMapper.queryTop(minVotes, safeLimit, regex);
        List<GlobalSiteScoreItem> vndb = vndbRankingMapper.queryTop(minVotes, safeLimit, regex);
        List<GlobalSiteScoreItem> egs = egsRankingMapper.queryTop(minVotes, safeLimit, regex);
        return new GlobalDualTopResponse(safeLimit, bangumi, vndb, egs, List.of());
    }

    public int setRankExcluded(String platform, String subjectId, boolean excluded) {
        if (!StringUtils.hasText(platform) || !StringUtils.hasText(subjectId)) {
            throw new IllegalArgumentException("platform and subjectId are required");
        }
        RankPlatform parsedPlatform = RankPlatform.parse(platform);
        return switch (parsedPlatform) {
            case BANGUMI -> bangumiRankingMapper.updateRankExcluded(parseNumericId(subjectId, "Bangumi"), excluded);
            case VNDB -> vndbRankingMapper.updateRankExcluded(subjectId.trim(), excluded);
            case EGS -> egsRankingMapper.updateRankExcluded(parseNumericId(subjectId, "EGS"), excluded);
        };
    }

    public int syncGameTopListToDatabase(int limit) {
        bangumiRankingMapper.ensureBangumiSubjectTable();
        int countBefore = bangumiRankingMapper.querySubjectCountAll();
        int safeLimit = Math.min(GlobalRankingPolicy.MAX_SYNC_LIMIT, Math.max(GlobalRankingPolicy.MIN_SYNC_LIMIT, limit));
        fetchFromRemoteAndPersist(safeLimit, 0);
        int countAfter = bangumiRankingMapper.querySubjectCountAll();
        return Math.max(0, countAfter - countBefore);
    }

    private BangumiGlobalResponse fetchFromRemoteAndPersist(int limit, int offset) {
        int safeLimit = Math.min(GlobalRankingPolicy.MAX_SYNC_LIMIT, Math.max(GlobalRankingPolicy.MIN_SYNC_LIMIT, limit));
        int safeOffset = Math.max(0, offset);

        BangumiSearchResult remoteResult = bangumiApiClient.fetchGalgameSubjects(safeLimit, safeOffset);
        List<BangumiGameItem> items = remoteResult.items();
        items.sort(Comparator.comparing(BangumiGameItem::rank, Comparator.nullsLast(Integer::compareTo)));
        bangumiRankingMapper.persistSubjects(items);
        return new BangumiGlobalResponse(remoteResult.total(), safeLimit, safeOffset, items);
    }

    private BangumiGlobalResponse fetchFromLocalCache(int limit, int offset) {
        Integer total = bangumiRankingMapper.querySubjectCount(
                GlobalRankingPolicy.MIN_BANGUMI_VOTES_FOR_LIST,
                GlobalRankingPolicy.EDITION_EXCLUSION_REGEX
        );
        if (total == null || total == 0) {
            return new BangumiGlobalResponse(0, limit, offset, List.of());
        }

        List<BangumiGameItem> items = bangumiRankingMapper.queryLocalList(
                GlobalRankingPolicy.MIN_BANGUMI_VOTES_FOR_LIST,
                limit,
                offset,
                GlobalRankingPolicy.EDITION_EXCLUSION_REGEX
        );
        return new BangumiGlobalResponse(total, limit, offset, items);
    }

    private long parseNumericId(String rawId, String platformName) {
        try {
            return Long.parseLong(rawId.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(platformName + " subjectId must be numeric");
        }
    }
}
