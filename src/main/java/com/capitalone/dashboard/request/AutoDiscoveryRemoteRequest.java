package com.capitalone.dashboard.request;

import com.capitalone.dashboard.model.AutoDiscovery;

public class AutoDiscoveryRemoteRequest extends AutoDiscovery {

    String autoDiscoveryId;

    public String getAutoDiscoveryId() { return autoDiscoveryId; }

    public void setAutoDiscoveryId(String autoDiscoveryId) { this.autoDiscoveryId = autoDiscoveryId; }
}
