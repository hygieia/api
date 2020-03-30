package com.capitalone.dashboard.webhook.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationPropertiesBinding
public class GithubSyncSettings {
    private String token;
    private List<String> notBuiltCommits = new ArrayList<>();

    @Value("${githubSyncSettings.firstRunHistoryDays:60}")
    private int firstRunHistoryDays;

    @Value("${githubSyncSettings.offsetMinutes:10}") // 10 mins default
    private int offsetMinutes;

    @Value("${githubSyncSettings.fetchCount:25}")
    private int fetchCount;

    @Value("${githubSyncSettings.commitPullSyncTime:86400000}") // 1 day in milliseconds
    private long commitPullSyncTime;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getNotBuiltCommits() {
        return notBuiltCommits;
    }

    public void setNotBuiltCommits(List<String> notBuiltCommits) {
        this.notBuiltCommits = notBuiltCommits;
    }

    public int getFirstRunHistoryDays() {
        return firstRunHistoryDays;
    }

    public void setFirstRunHistoryDays(int firstRunHistoryDays) {
        this.firstRunHistoryDays = firstRunHistoryDays;
    }

    public int getOffsetMinutes() {
        return offsetMinutes;
    }

    public void setOffsetMinutes(int offsetMinutes) {
        this.offsetMinutes = offsetMinutes;
    }

    public int getFetchCount() {
        return fetchCount;
    }

    public void setFetchCount(int fetchCount) {
        this.fetchCount = fetchCount;
    }

    public long getCommitPullSyncTime() {
        return commitPullSyncTime;
    }

    public void setCommitPullSyncTime(long commitPullSyncTime) {
        this.commitPullSyncTime = commitPullSyncTime;
    }
}
