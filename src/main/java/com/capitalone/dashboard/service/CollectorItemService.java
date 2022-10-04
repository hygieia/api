package com.capitalone.dashboard.service;

import org.springframework.http.ResponseEntity;

public interface CollectorItemService {
    /**
     *  Removing CollectorItems that are not connected
     *  to a Dashboard
     * */
    ResponseEntity<String> cleanup(String collectorType, String collectorName);
}
