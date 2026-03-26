package com.rinco.vnanalytics.api.global.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinco.vnanalytics.api.global.model.BangumiGameItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class BangumiApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String token;
    private final String userAgent;

    public BangumiApiClient(
            @Value("${bangumi.base-url}") String baseUrl,
            @Value("${bangumi.token:}") String token,
            @Value("${bangumi.user-agent}") String userAgent,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.token = token;
        this.userAgent = userAgent;
    }

    public BangumiSearchResult fetchGalgameSubjects(int limit, int offset) {
        JsonNode root = fetchByTag(limit, offset, "tag");
        if (root.path("data").isEmpty()) {
            root = fetchByTag(limit, offset, "meta_tags");
        }
        int total = root.path("total").asInt(0);
        List<BangumiGameItem> items = extractItems(root);
        return new BangumiSearchResult(total, items);
    }

    private JsonNode fetchByTag(int limit, int offset, String tagFieldName) {
        String payload = """
                {
                  "keyword": "",
                  "sort": "rank",
                  "filter": {
                    "type": [4],
                    "%s": ["Galgame"]
                  }
                }
                """.formatted(tagFieldName);

        RestClient.RequestBodySpec request = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v0/search/subjects")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .build())
                .header(HttpHeaders.USER_AGENT, userAgent)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        if (StringUtils.hasText(token)) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        String responseBody = request.body(payload).retrieve().body(String.class);
        return readJson(responseBody);
    }

    private List<BangumiGameItem> extractItems(JsonNode root) {
        List<BangumiGameItem> items = new ArrayList<>();
        for (JsonNode node : root.path("data")) {
            int rankValue = node.path("rating").path("rank").asInt(0);
            long id = node.path("id").asLong();
            String name = node.path("name").asText("");
            String nameCn = node.path("name_cn").asText("");
            String date = node.path("date").asText("");
            double scoreValue = node.path("rating").path("score").asDouble(0);
            int votesValue = node.path("rating").path("total").asInt(0);

            items.add(new BangumiGameItem(
                    id,
                    name,
                    nameCn,
                    date,
                    rankValue <= 0 ? null : rankValue,
                    scoreValue == 0 ? null : scoreValue,
                    votesValue == 0 ? null : votesValue,
                    "https://bgm.tv/subject/" + id
            ));
        }
        return items;
    }

    /**
     * Fetch user's game collection (subject_type=4). Paginates through all pages.
     */
    public JsonNode fetchUserGameCollections(String usernameOrId) {
        List<JsonNode> allData = new ArrayList<>();
        int limit = 50;
        int offset = 0;
        int total = Integer.MAX_VALUE;

        while (offset < total) {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri("/v0/users/{uid}/collections?subject_type=4&limit={limit}&offset={offset}",
                            usernameOrId, limit, offset)
                    .header(HttpHeaders.USER_AGENT, userAgent)
                    .accept(MediaType.APPLICATION_JSON);

            if (StringUtils.hasText(token)) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }

            String responseBody = request.retrieve().body(String.class);
            JsonNode root = readJson(responseBody);
            total = root.path("total").asInt(0);
            JsonNode data = root.path("data");
            for (JsonNode item : data) {
                allData.add(item);
            }
            offset += limit;
            if (data.isEmpty() || offset >= total) break;
        }

        return objectMapper.createObjectNode()
                .put("total", allData.size())
                .set("data", objectMapper.valueToTree(allData));
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Bangumi response", ex);
        }
    }
}
