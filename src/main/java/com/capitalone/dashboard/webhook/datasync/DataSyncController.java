package com.capitalone.dashboard.webhook.datasync;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.request.DataSyncRequest;
import com.capitalone.dashboard.request.DataSyncResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@Validated
@RequestMapping("/webhook")
public class DataSyncController {

    private final DataSyncService dataSyncService;

    @Autowired
    public DataSyncController(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }


    @RequestMapping(value = "/datasync/refresh", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DataSyncResponse> refresh(@Valid DataSyncRequest request) throws HygieiaException {
        DataSyncResponse response =  dataSyncService.refresh(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
