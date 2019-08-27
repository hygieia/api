package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.model.AutoDiscoveryRemoteRequest;

public interface AutoDiscoveryService {
    public AutoDiscovery save(AutoDiscoveryRemoteRequest request) throws HygieiaException;
}




