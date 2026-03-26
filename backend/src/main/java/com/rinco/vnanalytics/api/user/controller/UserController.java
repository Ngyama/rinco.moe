package com.rinco.vnanalytics.api.user.controller;

import com.rinco.vnanalytics.api.user.model.UserCollectionsResponse;
import com.rinco.vnanalytics.api.user.service.UserCollectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserCollectionService userCollectionService;

    public UserController(UserCollectionService userCollectionService) {
        this.userCollectionService = userCollectionService;
    }

    @GetMapping("/collections")
    public ResponseEntity<UserCollectionsResponse> getGalgameCollections(
            @RequestParam("id") String usernameOrId
    ) {
        if (!StringUtils.hasText(usernameOrId)) {
            return ResponseEntity.badRequest().build();
        }
        UserCollectionsResponse response = userCollectionService.fetchGalgameCollections(usernameOrId.trim());
        return ResponseEntity.ok(response);
    }
}
