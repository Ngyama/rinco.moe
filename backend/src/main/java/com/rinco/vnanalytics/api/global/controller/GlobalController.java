package com.rinco.vnanalytics.api.global.controller;

import com.rinco.vnanalytics.api.global.model.BangumiGlobalResponse;
import com.rinco.vnanalytics.api.global.model.GlobalDualTopResponse;
import com.rinco.vnanalytics.api.global.model.RankExclusionResponse;
import com.rinco.vnanalytics.api.global.model.SyncResponse;
import com.rinco.vnanalytics.api.global.service.GlobalRankingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/global")
public class GlobalController {

    private final GlobalRankingService globalRankingService;

    public GlobalController(GlobalRankingService globalRankingService) {
        this.globalRankingService = globalRankingService;
    }

    @GetMapping("/bangumi")
    public BangumiGlobalResponse getBangumiGameTopList(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return globalRankingService.fetchGameTopList(limit, offset);
    }

    @GetMapping("/dual-top")
    public ResponseEntity<GlobalDualTopResponse> getDualTop(
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "false") boolean matchedOnly
    ) {
        GlobalDualTopResponse body = globalRankingService.fetchDualTop(limit, matchedOnly);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    @PostMapping("/rank-exclusion")
    public RankExclusionResponse setRankExclusion(
            @RequestParam String platform,
            @RequestParam String subjectId,
            @RequestParam(defaultValue = "true") boolean excluded
    ) {
        int updated = globalRankingService.setRankExcluded(platform, subjectId, excluded);
        return new RankExclusionResponse(
                "ok",
                platform,
                subjectId,
                excluded,
                updated
        );
    }

    @PostMapping("/bangumi/sync")
    public SyncResponse syncBangumiGameTopList(
            @RequestParam(defaultValue = "100") @Min(20) @Max(200) int limit
    ) {
        int inserted = globalRankingService.syncGameTopListToDatabase(limit);
        return new SyncResponse(inserted, "ok");
    }

    @GetMapping("/bangumi/sync")
    public SyncResponse syncBangumiGameTopListByGet(
            @RequestParam(defaultValue = "100") @Min(20) @Max(200) int limit
    ) {
        int inserted = globalRankingService.syncGameTopListToDatabase(limit);
        return new SyncResponse(inserted, "ok");
    }
}
