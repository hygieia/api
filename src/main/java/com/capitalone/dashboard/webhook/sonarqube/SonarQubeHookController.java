package com.capitalone.dashboard.webhook.sonarqube;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.request.SonarDataSyncRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.MalformedURLException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@Validated
@RequestMapping("/webhook")
public class SonarQubeHookController {
    private final SonarQubeHookService sonarQubeHookService;

    @Autowired
    public SonarQubeHookController(SonarQubeHookService sonarQubeHookService) {
        this.sonarQubeHookService = sonarQubeHookService;
    }

    @RequestMapping(value = "/sonarqube/v1", method = POST,
            consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createFromSonarQubeV1(@RequestBody JSONObject request) throws ParseException, HygieiaException, MalformedURLException {
        String response = sonarQubeHookService.createFromSonarQubeV1(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @RequestMapping(value = "/sonarqube/data-sync", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> codeQualityStaticAnalysisDataSync(@Valid SonarDataSyncRequest request) throws HygieiaException {
        return sonarQubeHookService.syncData(request);
    }
}
