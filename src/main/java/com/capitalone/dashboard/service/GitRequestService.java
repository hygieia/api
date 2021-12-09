package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.GitRequest;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.LibraryPolicyResult;
import com.capitalone.dashboard.request.GitRequestRequest;
import com.capitalone.dashboard.request.LibraryPolicyRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public interface GitRequestService {

    /**
     * Finds all of the Pulls matching the specified request criteria.
     *
     * @param request search criteria
     * @param type search criteria - pull or issue
     * @param state search criteria - open, closed, merged or all (default)
     * @return Pulls matching criteria
     */
    DataResponse<Iterable<GitRequest>> search(GitRequestRequest request,
                                              String type, String state);


    String createFromGitHubv3(JSONObject request) throws ParseException, HygieiaException;

    /**
     * Finds the most recent GitRequest info for a specific collectorItemId
     * @param request collectorItemId componentId
     * @return data response of matching GitRequest
     */
    DataResponse<Iterable<GitRequest>> getGitRequestsForWidget(GitRequestRequest request, String type);

}
