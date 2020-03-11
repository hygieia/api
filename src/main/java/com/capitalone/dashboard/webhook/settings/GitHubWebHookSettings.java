package com.capitalone.dashboard.webhook.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationPropertiesBinding
public class GitHubWebHookSettings {
    private String token;
    private int commitTimestampOffset;
    private List<String> notBuiltCommits;
    private String userAgent;
    private List<String> githubEnterpriseHosts;
    @Value("${webHook.gitHub.maxRetries:5}")
    private int maxRetries;

    public List<String> getGithubEnterpriseHosts() { return githubEnterpriseHosts; }
    public void setGithubEnterpriseHosts(List<String> githubEnterpriseHosts) { this.githubEnterpriseHosts = githubEnterpriseHosts; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public int getCommitTimestampOffset() { return commitTimestampOffset; }
    public void setCommitTimestampOffset(int commitTimestampOffset) { this.commitTimestampOffset = commitTimestampOffset; }

    public List<String> getNotBuiltCommits() { return notBuiltCommits; }
    public void setNotBuiltCommits(List<String> notBuiltCommits) { this.notBuiltCommits = notBuiltCommits; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

}

