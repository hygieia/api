package com.capitalone.dashboard.request;

import javax.validation.constraints.NotNull;

public class DataSyncRequest {
    @NotNull
    private String collectorName;

    public String getCollectorName() {
        return collectorName;
    }

    public void setCollectorName(String collectorName) {
        this.collectorName = collectorName;
    }
}
