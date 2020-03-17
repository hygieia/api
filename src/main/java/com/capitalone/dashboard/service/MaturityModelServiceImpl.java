package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.MaturityModel;
import com.capitalone.dashboard.repository.MaturityModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MaturityModelServiceImpl implements MaturityModelService {

    private final MaturityModelRepository maturityModelRepository;

    @Autowired
    public MaturityModelServiceImpl(MaturityModelRepository maturityModelRepository) {

        this.maturityModelRepository = maturityModelRepository;
    }

    @Override
    public MaturityModel getMaturityModel(String profile) {
        return maturityModelRepository.findByProfile(profile);
    }

    @Override
    public List<String> getProfiles() {
        List<MaturityModel> maturityModels = Optional.ofNullable(maturityModelRepository.getAllProfiles()).orElse(Collections.emptyList());
        return maturityModels.stream().map(MaturityModel::getProfile).collect(Collectors.toList());
    }

}
