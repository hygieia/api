package com.capitalone.dashboard.webhook.sonarqube;

import com.capitalone.dashboard.client.RestClient;
import com.capitalone.dashboard.client.RestUserInfo;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.CodeQualityMetric;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.SonarProject;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.SonarProjectRepository;
import com.capitalone.dashboard.request.SonarDataSyncRequest;
import com.capitalone.dashboard.settings.ApiSettings;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ComponentRepository componentRepository;
    private final RestClient restClient;
    @Autowired
    private ApiSettings settings;


    @Autowired
    SonarQubeHookServiceImpl( CodeQualityRepository codeQualityRepository, SonarProjectRepository sonarProjectRepository,
                              CollectorRepository collectorRepository, ComponentRepository componentRepository,ApiSettings settings, RestClient restClient)
    {
        this.codeQualityRepository = codeQualityRepository;
        this.sonarProjectRepository = sonarProjectRepository;
        this.collectorRepository = collectorRepository;
        this.componentRepository = componentRepository;
        this.settings = settings;
        this.restClient = restClient;
    }

    @Override
    public String createFromSonarQubeV1(JSONObject request) throws ParseException {

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

    private CodeQuality currentCodeQuality(SonarProject project, JSONObject request) {

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

    private Integer integer(JsonObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : Integer.valueOf(obj.toString().replaceAll("\"",""));
    }

    private String format(String duration) {
        long durationInMinutes = Long.parseLong(duration);
        if (durationInMinutes == 0) {
            return "0";
        }
        boolean isNegative = durationInMinutes < 0;
        Long absDuration = Math.abs(durationInMinutes);

        int days = ((Double) ((double) absDuration / HOURS_IN_DAY / 60)).intValue();
        Long remainingDuration =  absDuration - (days * HOURS_IN_DAY * 60);
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

    /**
     * Sync code quality static analysis data from one server to the another
     * @param request
     * @return
     * @throws HygieiaException
     */
    public ResponseEntity<String> syncData(SonarDataSyncRequest request) throws HygieiaException {

        String getVersionEpt = "/api/server/version";
        List<SonarProject> updatedProjects = new ArrayList<>();
        String from = request.getSyncFrom();
        String to = request.getSyncTo();
        boolean isSync = request.getIsSync();
        final AtomicInteger index = new AtomicInteger();
        final AtomicInteger compIndex = new AtomicInteger();
        HttpHeaders httpHeaders = new HttpHeaders();
        Collector collector;

        try {
            if (Strings.isNullOrEmpty(from) || Strings.isNullOrEmpty(to)) {
                throw new HygieiaException("sonar server host names should not be null or empty", HygieiaException.INVALID_CONFIGURATION);
            }
            collector = collectorRepository.findByName("Sonar");
            if (collector == null) {
                throw new HygieiaException("Collector not found", HygieiaException.COLLECTOR_CREATE_ERROR);
            }
        } catch (Exception e) {
            throw new HygieiaException(e.getMessage(), e.getCause(), false, true);
        }
        List<SonarProject> projects = getSonarProjects(to);
        projects.stream().forEach(project -> {
            String projectName = project.getProjectName();
            Optional<SonarProject> existingSonarProjectOpt = Optional.ofNullable(sonarProjectRepository.findSonarProjectByProjectName(
                    collector.getId(), from, projectName));
            existingSonarProjectOpt.ifPresent(eSonarProject -> {
                eSonarProject.setProjectId(project.getProjectId());
                eSonarProject.setInstanceUrl(to);
                updatedProjects.add(eSonarProject);
                updateComponent(eSonarProject, isSync, compIndex);
                LOG.info((index.getAndIncrement() + " : " + projectName + "'s project id & instance url updated"));
            });
        });

        String math = updatedProjects.size() + "/" + projects.size();
        String message = math + " sonar collector items and " + compIndex + " dashboard components can be updated";
        if (isSync) {
            sonarProjectRepository.save(updatedProjects);
            message = math + " sonar collector items and " + compIndex + " dashboard components updated";
        }
        LOG.info(message);
        return ResponseEntity.ok(message);
    }

    /**
     * Get code quality projects
     * @param serverUrl
     * @return List
     * @throws HygieiaException
     */
    private List<SonarProject> getSonarProjects(String serverUrl) throws HygieiaException {

        String getProjectsEpt = serverUrl + "/api/components/search?qualifiers=TRK&ps=500";
        JSONParser jsonParser = new JSONParser();
        JSONArray jsonArray = new JSONArray();
        List<SonarProject> projects = new ArrayList<>();
        String sonarApiToken = settings.getSonarDataSyncSettings().getToken();
        String userId= settings.getSonarDataSyncSettings().getUserId();
        String passCode = settings.getSonarDataSyncSettings().getPassCode();
        RestUserInfo restUserInfo = new RestUserInfo(userId, passCode, sonarApiToken);
        HttpHeaders httpHeaders = settings.getSonarDataSyncSettings().getHeaders(restUserInfo);

        try {
            ResponseEntity<String> response = restClient.makeRestCallGet(getProjectsEpt, httpHeaders);
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                throw new HygieiaException(response.getBody(), HygieiaException.INVALID_CONFIGURATION);
            }
            JSONObject responseBody = (JSONObject) jsonParser.parse(response.getBody());
            Optional<Object> pagingOpt = Optional.ofNullable(responseBody.get("paging"));
            long totalProjects = pagingOpt.isPresent() ? (Long) ((JSONObject) pagingOpt.get()).get("total") : 0;
            long pageSize = pagingOpt.isPresent() ? (Long) ((JSONObject) pagingOpt.get()).get("pageSize") : 1;
            int pages = (int) Math.ceil((double) totalProjects / pageSize);

            if (totalProjects <= pageSize) {
                jsonArray.addAll((JSONArray) responseBody.get("components"));
            } else {
                for (int start = 1; start <= pages; start++) {
                    String urlFinal = getProjectsEpt + "&p=" + start;
                    response = restClient.makeRestCallGet(urlFinal, httpHeaders);
                    JSONObject jsonObjectResponse = (JSONObject) jsonParser.parse(response.getBody());
                    jsonArray.addAll((JSONArray) jsonObjectResponse.get("components"));
                }
            }
        } catch (Exception e) {
            throw new HygieiaException(e.getMessage(), HygieiaException.INVALID_CONFIGURATION);
        }
        jsonArray.forEach(jsonObj -> {
            JSONObject prjData = (JSONObject) jsonObj;
            SonarProject project = new SonarProject();
            project.setInstanceUrl(serverUrl);
            project.setProjectId((String) Optional.ofNullable(prjData.get("id")).orElse(null));
            project.setProjectName((String) Optional.ofNullable(prjData.get("name")).orElse(null));
            projects.add(project);
        });
        return projects;
    }

    /**
     * Updated the component's code quality collector items' project id and instance url
     * @param eSonarProject
     * @param isSync
     * @param compIndex
     */
    private void updateComponent(SonarProject eSonarProject, boolean isSync, @Nullable AtomicInteger compIndex) {
        List<Component> components = componentRepository.findByCodeQualityCollectorItems(eSonarProject.getId());
        List<CollectorItem> codeQualityCollectorItems = new ArrayList<>();
        components.forEach(component -> {
            component.getCollectorItems(CollectorType.CodeQuality).forEach(collectorItem -> {
                if (eSonarProject.getProjectName().equals(collectorItem.getOptions().get("projectName"))) {
                    collectorItem.getOptions().put("projectId", eSonarProject.getProjectId());
                    collectorItem.getOptions().put("instanceUrl", eSonarProject.getInstanceUrl());
                    collectorItem.setLastUpdated(System.currentTimeMillis());
                    if (null != compIndex) {
                        compIndex.getAndIncrement();
                    }
                }
                codeQualityCollectorItems.add(collectorItem);
            });
            Map<CollectorType, List<CollectorItem>> collectorItems = component.getCollectorItems();
            collectorItems.put(CollectorType.CodeQuality, codeQualityCollectorItems);
            component.setCollectorItems(collectorItems);
            if (isSync) {
                componentRepository.save(component);
            }
        });
    }
}
