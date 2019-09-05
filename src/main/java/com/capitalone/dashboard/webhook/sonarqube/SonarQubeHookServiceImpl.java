package com.capitalone.dashboard.webhook.sonarqube;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.SonarProjectRepository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.MalformedURLException;

@Service
public class SonarQubeHookServiceImpl implements SonarQubeHookService {

    private static final Log LOG = LogFactory.getLog(SonarQubeHookService.class);

    private static final String MINUTES_FORMAT = "%smin";
    private static final String HOURS_FORMAT = "%sh";
    private static final String DAYS_FORMAT = "%sd";
    private static final int HOURS_IN_DAY = 8;

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String KEY = "key";
    private static final String METRIC = "metric";
    private static final String MSR = "measures";
    private static final String VALUE = "value";
    private static final String CDT = "conditions";
    private static final String DSHBRD_URL = "url";
    private static final String TASK_ID = "taskId";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_ALERT = "ALERT";
    private static final String DATE = "date";
    private static final String EVENTS = "events";

    private final CodeQualityRepository codeQualityRepository;
    private final SonarProjectRepository sonarProjectRepository;

    @Autowired
    SonarQubeHookServiceImpl( CodeQualityRepository codeQualityRepository, SonarProjectRepository sonarProjectRepository)
    {
        this.codeQualityRepository = codeQualityRepository;
        this.sonarProjectRepository = sonarProjectRepository;
    }

    @Override
    public String createFromSonarQubeV1(JSONObject request) throws ParseException, HygieiaException, MalformedURLException {

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(request.toJSONString());
        JSONObject prjData = (JSONObject) jsonObject.get("project");
        SonarCollector collector = new SonarCollector();

        SonarProject project = new SonarProject();
        project.setInstanceUrl(str(prjData,DSHBRD_URL));
        project.setProjectId(str(jsonObject, KEY));
        project.setProjectName(str(prjData, NAME));
        refreshData(project, request);

        return "Processing Complete for " + project.getProjectName();
    }

    private void refreshData(SonarProject sonarProject, JSONObject request) {

        CodeQuality codeQuality = currentCodeQuality(sonarProject, request);
        if (codeQuality != null) {
            sonarProject.setLastUpdated(System.currentTimeMillis());
            sonarProjectRepository.save(sonarProject);
            codeQuality.setCollectorItemId(sonarProject.getId());
            codeQualityRepository.save(codeQuality);
        }
    }

    public CodeQuality currentCodeQuality(SonarProject project, JSONObject request) {

        try {
                JsonParser jsonParser = new JsonParser();
                JsonObject gsonObject = (JsonObject)jsonParser.parse(request.toString());
                JsonObject prjData = gsonObject.getAsJsonObject("project");
                CodeQuality codeQuality = new CodeQuality();
                codeQuality.setType(CodeQualityType.StaticAnalysis);
                codeQuality.setName(str(prjData, NAME));
                codeQuality.setUrl(str(prjData, DSHBRD_URL));
                JsonObject prjMetrics = gsonObject.getAsJsonObject("qualityGate");


            for (Object metricObj : prjMetrics.getAsJsonArray(CDT)) {
                    JsonObject metricJson = (JsonObject) metricObj;

                    CodeQualityMetric metric = new CodeQualityMetric(str(metricJson, METRIC));
                    metric.setValue(str(metricJson, VALUE));
                    if (metric.getName().equals("sqale_index")) {
                        metric.setFormattedValue(format(str(metricJson, VALUE)));
                    } else if (strSafe(metricJson, VALUE).indexOf(".") > 0) {
                        metric.setFormattedValue(str(metricJson, VALUE) + "%" );
                    } else if (strSafe(metricJson, VALUE).matches("\\d+")) {
                        metric.setFormattedValue(String.format("%,d", integer(metricJson, VALUE)));
                    } else {
                        metric.setFormattedValue(str(metricJson, VALUE));
                    }
                    codeQuality.getMetrics().add(metric);
                }

                return codeQuality;

        }  catch (RestClientException rce) {
            LOG.error("Rest Client Exception: " + project + ":" + rce.getMessage());
        }

        return null;
    }

    private String str(JsonObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : obj.toString();
    }

    private String str(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : obj.toString();
    }

    private String strSafe(JsonObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? "" : obj.toString();
    }

    private String strSafe(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? "" : obj.toString();
    }

    private Integer integer(JsonObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : Integer.valueOf(obj.toString());
    }

    @SuppressWarnings("unused")
    private Integer integer(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : Integer.valueOf(obj.toString());
    }

    @SuppressWarnings("unused")
    private BigDecimal decimal(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : new BigDecimal(obj.toString());
    }

    @SuppressWarnings("unused")
    private Boolean bool(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : Boolean.valueOf(obj.toString());
    }

    @SuppressWarnings("unused")
    private String format(String duration) {
        Long durationInMinutes = Long.valueOf(duration);
        if (durationInMinutes == 0) {
            return "0";
        }
        boolean isNegative = durationInMinutes < 0;
        Long absDuration = Math.abs(durationInMinutes);

        int days = ((Double) ((double) absDuration / HOURS_IN_DAY / 60)).intValue();
        Long remainingDuration = absDuration - (days * HOURS_IN_DAY * 60);
        int hours = ((Double) (remainingDuration.doubleValue() / 60)).intValue();
        remainingDuration = remainingDuration - (hours * 60);
        int minutes = remainingDuration.intValue();

        return format(days, hours, minutes, isNegative);
    }

    @SuppressWarnings("PMD")
    private static String format(int days, int hours, int minutes, boolean isNegative) {
        StringBuilder message = new StringBuilder();
        if (days > 0) {
            message.append(String.format(DAYS_FORMAT, isNegative ? (-1 * days) : days));
        }
        if (displayHours(days, hours)) {
            addSpaceIfNeeded(message);
            message.append(String.format(HOURS_FORMAT, isNegative && message.length() == 0 ? (-1 * hours) : hours));
        }
        if (displayMinutes(days, hours, minutes)) {
            addSpaceIfNeeded(message);
            message.append(String.format(MINUTES_FORMAT, isNegative && message.length() == 0 ? (-1 * minutes) : minutes));
        }
        return message.toString();
    }

    private static void addSpaceIfNeeded(StringBuilder message) {
        if (message.length() > 0) {
            message.append(" ");
        }
    }

    private JSONArray parseAsArray(JSONObject jsonObject, String key) throws ParseException {
        return (JSONArray) jsonObject.get(key);
    }

    private static boolean displayHours(int days, int hours) {
        return hours > 0 && days < 10;
    }

    private static boolean displayMinutes(int days, int hours, int minutes) {
        return minutes > 0 && hours < 10 && days == 0;
    }
}
