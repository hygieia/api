package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.PrefTestCreateRequest;
import com.capitalone.dashboard.model.TestJunit;
import com.capitalone.dashboard.model.TestResult;
import com.capitalone.dashboard.request.PerfTestDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import com.capitalone.dashboard.request.TestResultRequest;
import org.json.simple.JSONObject;

public interface TestResultService {

    DataResponse<Iterable<TestResult>> search(TestResultRequest request);
    String create(TestDataCreateRequest request) throws HygieiaException;
    String createV2(TestDataCreateRequest request) throws HygieiaException;
    String createPerf(PerfTestDataCreateRequest request) throws HygieiaException;
    String createPerfV2(PerfTestDataCreateRequest request) throws HygieiaException;
    String createPerfV3(PrefTestCreateRequest jsonRequest , TestJunit xmlRequest, String prefTool , String type) throws HygieiaException;

}
