package com.capitalone.dashboard.webhook.sonarqube;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    private static final String ANALYSIS_TIME = "analysedAt";
    private static final String TASK_ID = "taskId";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_ALERT = "ALERT";
    private static final String DATE = "date";
    private static final String EVENTS = "events";
    private static final String VERSION = "revision";
    private static final String NICE_NAME = "niceName";
    private static final String PROJECT_NAME = "options.projectName";

    private final CodeQualityRepository codeQualityRepository;
    private final SonarProjectRepository sonarProjectRepository;
    private final CollectorRepository collectorRepository;

    @Autowired
    SonarQubeHookServiceImpl( CodeQualityRepository codeQualityRepository, SonarProjectRepository sonarProjectRepository,CollectorRepository collectorRepository)
    {
        this.codeQualityRepository = codeQualityRepository;
        this.sonarProjectRepository = sonarProjectRepository;
        this.collectorRepository = collectorRepository;
    }

    @Override
    public String createFromSonarQubeV1(JSONObject request) throws ParseException, HygieiaException, MalformedURLException {

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(request.toJSONString());
        JSONObject prjData = (JSONObject) jsonObject.get("project");

        SonarProject existingProject;
        SonarProject project = new SonarProject();

        project.setEnabled(false);
        project.setDescription(project.getProjectName());
        project.setNiceName("SonarWebhook");
        project.setInstanceUrl(str(prjData,DSHBRD_URL));
        project.setProjectId(str(prjData, KEY));
        project.setProjectName(str(prjData, NAME));
        project.setDescription(project.getProjectName());

        Collector collector = collectorRepository.findByName("Sonar");
        if(collector == null){
            collector = new Collector();
            collector.setName("Sonar");
            collector.setCollectorType(CollectorType.CodeQuality);
            collector.setOnline(true);
            collector.setEnabled(true);
            collector.setSearchFields(Arrays.asList(PROJECT_NAME,NICE_NAME));
            Map<String, Object> allOptions = new HashMap<>();
            allOptions.put(project.getInstanceUrl(),"");
            allOptions.put(project.getProjectName(),"");
            allOptions.put(project.getProjectId(), "");
            collector.setAllFields(allOptions);
            Map<String, Object> uniqueOptions = new HashMap<>();
            uniqueOptions.put(project.getInstanceUrl(),"");
            uniqueOptions.put(project.getProjectName(),"");
            collector.setUniqueFields(uniqueOptions);
        }

        project.setCollectorId(collector.getId());


        existingProject = sonarProjectRepository.findSonarProject(project.getCollectorId(),project.getInstanceUrl(),project.getProjectName());

        if(existingProject != null)
        {
            refreshData(existingProject, request);
        }else
        {
            refreshData(project, request);
        }

        return "Processing Complete for " + project.getProjectName();
    }

    private void refreshData(SonarProject sonarProject, JSONObject request) {

        CodeQuality codeQuality = currentCodeQuality(sonarProject, request);
        if (codeQuality != null && isNewQualityData(sonarProject, codeQuality)) {
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
                codeQuality.setTimestamp(getTimestamp(request,ANALYSIS_TIME));
                codeQuality.setVersion(str(gsonObject,VERSION));
                JsonObject prjMetrics = gsonObject.getAsJsonObject("qualityGate");

                //Obtain missing metrics not in conditions
                CodeQualityMetric qualityGateMetric = new CodeQualityMetric();
                qualityGateMetric.setName("quality_gate_details");
                qualityGateMetric.setValue(strSafe(gsonObject,"qualityGate"));
                codeQuality.getMetrics().add(qualityGateMetric);

                qualityGateMetric.setName("alert_status");
                qualityGateMetric.setValue(str(prjMetrics,"status"));
                codeQuality.getMetrics().add(qualityGateMetric);

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

    private boolean isNewQualityData(SonarProject project, CodeQuality codeQuality) {
        return codeQualityRepository.findByCollectorItemIdAndTimestamp(
                project.getId(), codeQuality.getTimestamp()) == null;
    }

    private boolean isNewProject(SonarCollector collector, SonarProject application) {
        return sonarProjectRepository.findSonarProject(
                collector.getId(), application.getInstanceUrl(), application.getProjectId()) == null;
    }

    private String str(JsonObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : obj.toString().replaceAll("\"","");
    }

    private String str(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : obj.toString().replaceAll("\"","");
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
        return obj == null ? null : Integer.valueOf(obj.toString().replaceAll("\"",""));
    }

    @SuppressWarnings("unused")
    private Integer integer(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : Integer.valueOf(obj.toString().replaceAll("\"",""));
    }

    @SuppressWarnings("unused")
    private BigDecimal decimal(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : new BigDecimal(obj.toString().replaceAll("\"",""));
    }

    @SuppressWarnings("unused")
    private Boolean bool(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : Boolean.valueOf(obj.toString().replaceAll("\"",""));
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

    private long getTimestamp(JSONObject json, String key) {
        Object obj = json.get(key);
        if (obj != null) {
            try {
                return new SimpleDateFormat(DATE_FORMAT).parse(obj.toString().replaceAll("\"","")).getTime();
            } catch (java.text.ParseException e) {
                LOG.error(obj + " is not in expected format " + DATE_FORMAT, e);
            }
        }
        return 0;
    }
}
