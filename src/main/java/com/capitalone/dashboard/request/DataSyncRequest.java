package com.capitalone.dashboard.request;

import javax.validation.constraints.NotNull;

public class DataSyncRequest extends BaseRequest {
    @NotNull
    private String collectorName;

    public String getCollectorName() {
        return collectorName;
    }

    public void setCollectorName(String collectorName) {
        this.collectorName = collectorName;
    }
}
