package com.capitalone.dashboard.webhook.datasync;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.request.DataSyncRequest;
import com.capitalone.dashboard.request.DataSyncResponse;

public interface DataSyncService {

    DataSyncResponse refresh(DataSyncRequest request) throws HygieiaException;
}
