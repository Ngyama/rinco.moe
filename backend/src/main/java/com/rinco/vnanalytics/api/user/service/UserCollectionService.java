package com.rinco.vnanalytics.api.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.rinco.vnanalytics.api.global.client.BangumiApiClient;
import com.rinco.vnanalytics.api.structure.mapper.StructureMapper;
import com.rinco.vnanalytics.api.user.model.UserCollectionItem;
import com.rinco.vnanalytics.api.user.model.UserCollectionsResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UserCollectionService {

    private final BangumiApiClient bangumiApiClient;
    private final StructureMapper structureMapper;

    public UserCollectionService(BangumiApiClient bangumiApiClient, StructureMapper structureMapper) {
        this.bangumiApiClient = bangumiApiClient;
        this.structureMapper = structureMapper;
    }

    public UserCollectionsResponse fetchGalgameCollections(String usernameOrId) {
        Set<Long> galgameIds = structureMapper.queryGalgameSubjectIds();
        JsonNode root = bangumiApiClient.fetchUserGameCollections(usernameOrId);
        List<UserCollectionItem> items = new ArrayList<>();

        for (JsonNode node : root.path("data")) {
            long subjectId = node.path("subject_id").asLong();
            if (!galgameIds.contains(subjectId)) continue;

            int rate = node.path("rate").asInt(0);
            String type = node.path("type").asText("");
            String comment = node.has("comment") && !node.path("comment").isNull()
                    ? node.path("comment").asText("") : "";

            String subjectName = "";
            String subjectNameCn = "";
            if (node.has("subject") && !node.path("subject").isNull()) {
                JsonNode sub = node.path("subject");
                subjectName = sub.path("name").asText("");
                subjectNameCn = sub.path("name_cn").asText("");
            }

            items.add(new UserCollectionItem(
                    subjectId,
                    subjectName,
                    subjectNameCn,
                    rate,
                    type,
                    comment
            ));
        }

        return new UserCollectionsResponse(usernameOrId, items);
    }
}
