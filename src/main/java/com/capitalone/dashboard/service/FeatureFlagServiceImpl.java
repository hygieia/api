package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.FeatureFlag;
import com.capitalone.dashboard.model.adapter.FeatureFlagAdapter;
import com.capitalone.dashboard.repository.FeatureFlagRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections4.IterableUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;


@Component
public class FeatureFlagServiceImpl implements FeatureFlagService {

    public static final GsonBuilder featureFlagBuilder = new GsonBuilder().registerTypeAdapter(FeatureFlag.class, new FeatureFlagAdapter());
    private FeatureFlagRepository featureFlagRepository;

    @Autowired
    public FeatureFlagServiceImpl(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    @Override
    public String createOrUpdateFlags(String json){
        Gson gson = featureFlagBuilder.create();
        FeatureFlag ff = gson.fromJson(json,FeatureFlag.class);
        FeatureFlag existing = featureFlagRepository.findByName(ff.getName());
        if(Objects.nonNull(existing)){
            ff.setId(existing.getId());
        }
        featureFlagRepository.save(ff);
        return ff.toString();
    }

    @Override
    public List<FeatureFlag> getFeatureFlags(){
        return IterableUtils.toList(featureFlagRepository.findAll());

    }

    @Override
    public void deleteFlags(ObjectId id){
        featureFlagRepository.delete(id);
    }

}
