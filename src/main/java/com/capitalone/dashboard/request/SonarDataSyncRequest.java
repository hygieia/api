package com.capitalone.dashboard.request;

public class SonarDataSyncRequest extends BaseRequest {

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

    public boolean getIsSync() {
        return isSync;
    }

    public void setIsSync(boolean isSync) {
        this.isSync = isSync;
    }
}
