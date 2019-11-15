package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.Metadata;
import com.capitalone.dashboard.request.MetadataCreateRequest;
import org.json.simple.JSONObject;

public interface MetadataService {

    String create(MetadataCreateRequest request) throws HygieiaException;


    DataResponse<Iterable<Metadata>> search(String searchKey, String value) throws HygieiaException;

}
