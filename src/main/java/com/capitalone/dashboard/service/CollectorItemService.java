package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.CollectorType;
import org.springframework.http.ResponseEntity;

public interface CollectorItemService {
    /**
     *  Removing CollectorItems that are not connected
     *  to a Dashboard
     * */
    ResponseEntity<String> deleteDisconnectedItems(String collectorType, String collectorName);
}
