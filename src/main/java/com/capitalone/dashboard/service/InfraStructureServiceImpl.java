package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.InfrastructureScan;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.repository.InfrastructureScanRepository;
import com.capitalone.dashboard.request.InfraStructureRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class InfraStructureServiceImpl implements InfraStructureService {

    private final InfrastructureScanRepository infrastructureScanRepository;

    @Autowired
    public InfraStructureServiceImpl(InfrastructureScanRepository infrastructureScanRepository) {
        this.infrastructureScanRepository = infrastructureScanRepository;
    }

    @Override
    public DataResponse<Iterable<InfrastructureScan>> getInfraScanForWidget(InfraStructureRequest request) {
        InfrastructureScan infrastructureScan = infrastructureScanRepository.findTopByCollectorItemIdOrderByTimestampDesc(request.getCollectorItemId());
        return new DataResponse<>(Collections.singletonList(infrastructureScan), System.currentTimeMillis());
    }
}
