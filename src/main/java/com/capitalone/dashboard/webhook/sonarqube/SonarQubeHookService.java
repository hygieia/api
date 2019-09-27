package com.capitalone.dashboard.webhook.sonarqube;

import com.capitalone.dashboard.misc.HygieiaException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.net.MalformedURLException;

public interface SonarQubeHookService {
    String createFromSonarQubeV1(JSONObject request) throws ParseException, HygieiaException, MalformedURLException;
}
