package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.LibraryPolicyResult;
import com.capitalone.dashboard.request.LibraryPolicyRequest;

import java.util.List;

public interface LibraryPolicyService {

    /**
     * Finds all of the CodeQuality data matching the specified request criteria.
     *
     * @param request search criteria
     * @return quality data matching criteria
     */
    DataResponse<List<LibraryPolicyResult>> search(LibraryPolicyRequest request);


    /**
     * Finds the most recent LibraryPolicy result for a specific collectorItemId
     * @param request collectorItemId componentId
     * @return data response of matching LibraryPolicy
     */
    DataResponse<Iterable<LibraryPolicyResult>> getLibraryPolicyForWidget(LibraryPolicyRequest request);

}
