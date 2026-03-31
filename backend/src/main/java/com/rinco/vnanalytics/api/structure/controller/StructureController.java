package com.rinco.vnanalytics.api.structure.controller;

import com.rinco.vnanalytics.api.structure.model.ControversyResponse;
import com.rinco.vnanalytics.api.structure.model.ExtremeBarsResponse;
import com.rinco.vnanalytics.api.structure.model.HotGameDetail;
import com.rinco.vnanalytics.api.structure.model.HotGamesResponse;
import com.rinco.vnanalytics.api.structure.service.StructureService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/structure")
public class StructureController {

    private final StructureService structureService;

    public StructureController(StructureService structureService) {
        this.structureService = structureService;
    }

    @GetMapping("/hot")
    public HotGamesResponse getHotGames(
            @RequestParam(defaultValue = "2025") @Min(1990) @Max(2030) int year,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit,
            @RequestParam(defaultValue = "markers") String sort
    ) {
        return structureService.fetchHotGames(year, limit, sort);
    }

    @GetMapping("/hot/{id}")
    public ResponseEntity<HotGameDetail> getHotGameDetail(@PathVariable long id) {
        HotGameDetail detail = structureService.fetchHotGameDetail(id);
        return detail != null ? ResponseEntity.ok(detail) : ResponseEntity.notFound().build();
    }

    @GetMapping("/controversy")
    public ControversyResponse getControversy(
            @RequestParam(defaultValue = "2000") @Min(1990) @Max(2030) int yearFrom,
            @RequestParam(defaultValue = "2025") @Min(1990) @Max(2030) int yearTo,
            @RequestParam(defaultValue = "20") @Min(0) @Max(5000) int minVotes
    ) {
        return structureService.fetchControversy(yearFrom, yearTo, minVotes);
    }

    @GetMapping("/controversy/extreme-bars")
    public ExtremeBarsResponse getExtremeBars(
            @RequestParam(defaultValue = "2000") @Min(1990) @Max(2030) int yearFrom,
            @RequestParam(defaultValue = "2025") @Min(1990) @Max(2030) int yearTo,
            @RequestParam(defaultValue = "20") @Min(0) @Max(5000) int minVotes
    ) {
        return structureService.fetchExtremeBars(yearFrom, yearTo, minVotes);
    }
}
