package com.capitalone.dashboard.request;

public class CodeQualityDataSyncRequest {

    private String syncFrom;
    private String syncTo;
    private boolean isSync;

    public String getSyncFrom() {
        return syncFrom;
    }

    public void setSyncFrom(String syncFrom) {
        this.syncFrom = syncFrom;
    }

    public String getSyncTo() {
        return syncTo;
    }

    public void setSyncTo(String syncTo) {
        this.syncTo = syncTo;
    }

    public boolean isSync() {
        return isSync;
    }

    public void setSync(boolean sync) {
        isSync = sync;
    }
}
