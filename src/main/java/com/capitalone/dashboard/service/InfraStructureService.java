package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.InfrastructureScan;
import com.capitalone.dashboard.request.InfraStructureRequest;

public interface InfraStructureService {
    DataResponse<Iterable<InfrastructureScan>> getInfraScanForWidget(InfraStructureRequest request);
}
