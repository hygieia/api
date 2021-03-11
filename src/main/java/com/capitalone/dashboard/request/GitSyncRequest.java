package com.capitalone.dashboard.request;

import javax.validation.constraints.NotNull;

public class GitSyncRequest extends BaseRequest {
    @NotNull
    private String repo;
    @NotNull
    private String branch;
    @NotNull
    private int historyDays;

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getHistoryDays() { return historyDays; }

    public void setHistoryDays(int historyDays) { this.historyDays = historyDays; }
}
