package com.capitalone.dashboard.webhook.github;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.request.GitSyncRequest;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/sync")
public class GitHubSyncController {
    private final GitHubSyncService gitHubSyncService;

    @Autowired
    public GitHubSyncController(GitHubSyncService gitHubSyncService) {
        this.gitHubSyncService = gitHubSyncService;
    }

    @RequestMapping(value = "/repo", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> syncGithubRepo(@RequestBody GitSyncRequest request) throws ParseException, HygieiaException, MalformedURLException {
        String response = gitHubSyncService.syncGithubRepo(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
