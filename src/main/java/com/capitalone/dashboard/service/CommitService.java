package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Commit;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.request.CommitRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.List;

public interface CommitService {

    /**
     * Finds all of the Commits matching the specified request criteria.
     *
     * @param request search criteria
     * @return commits matching criteria
     */
    DataResponse<Iterable<Commit>> search(CommitRequest request);

    String createFromGitHubv3(JSONObject request) throws ParseException, HygieiaException;

    List<Commit> getCommitsBySha (String scmRevisionNumber);

    /**
     * Finds all of the Commits for a specific collectorItem
     *
     * @param request componentId collectorItemId
     * @return commits matching criteria
     */
    DataResponse<Iterable<Commit>> getCommitsForWidget(CommitRequest request);
}
