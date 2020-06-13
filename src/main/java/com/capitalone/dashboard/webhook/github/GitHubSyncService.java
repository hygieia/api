package com.capitalone.dashboard.webhook.github;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.request.GitSyncRequest;
import org.json.simple.parser.ParseException;

import java.net.MalformedURLException;

public interface GitHubSyncService {
    String syncGithubRepo(GitSyncRequest request) throws ParseException, HygieiaException, MalformedURLException;
}
