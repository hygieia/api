package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.FeatureFlag;
import org.bson.types.ObjectId;

import java.util.List;

public interface FeatureFlagService {
    String createOrUpdateFlags(String json) ;
    List<FeatureFlag> getFeatureFlags();
    void deleteFlags(ObjectId id);

}
