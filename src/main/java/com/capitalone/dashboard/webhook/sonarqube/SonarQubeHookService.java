package com.capitalone.dashboard.webhook.sonarqube;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.request.SonarDataSyncRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;

public interface SonarQubeHookService {
    String createFromSonarQubeV1(JSONObject request) throws ParseException, HygieiaException, MalformedURLException;
    ResponseEntity<String> syncData(SonarDataSyncRequest request) throws HygieiaException;
}
