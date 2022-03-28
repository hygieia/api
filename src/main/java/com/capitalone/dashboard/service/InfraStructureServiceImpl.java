package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.InfrastructureScan;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.repository.InfrastructureScanRepository;
import com.capitalone.dashboard.request.InfraStructureRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class InfraStructureServiceImpl implements InfraStructureService {

    private final InfrastructureScanRepository infrastructureScanRepository;

    @Autowired
    public InfraStructureServiceImpl(InfrastructureScanRepository infrastructureScanRepository) {
        this.infrastructureScanRepository = infrastructureScanRepository;
    }

    @Override
    public DataResponse<Iterable<InfrastructureScan>> getInfraScanForWidget(InfraStructureRequest request) {
        List<InfrastructureScan> tempInfraScanList = infrastructureScanRepository.findByCollectorItemIdOrderByTimestampDesc(request.getCollectorItemId());
        List<InfrastructureScan> infrastructureScanList = new ArrayList<InfrastructureScan>();

        ArrayList<String> instanceIdArray = new ArrayList<String>();
        tempInfraScanList.forEach(scan -> {
            String instanceId = scan.getInstanceId();
            if(!instanceIdArray.contains(instanceId)){
                instanceIdArray.add(instanceId);
                infrastructureScanList.add(scan);
            }
        });

        return new DataResponse<>(infrastructureScanList, System.currentTimeMillis());
    }
}
