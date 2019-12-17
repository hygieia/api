package com.capitalone.dashboard.service;

import com.capitalone.dashboard.client.RestClient;
import com.capitalone.dashboard.collector.RestOperationsSupplier;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.CodeQualityMetric;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.QCodeQuality;
import com.capitalone.dashboard.model.SonarProject;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.SonarProjectRepository;
import com.capitalone.dashboard.request.CodeQualityCreateRequest;
import com.capitalone.dashboard.request.CodeQualityDataSyncRequest;
import com.capitalone.dashboard.request.CodeQualityRequest;
import com.capitalone.dashboard.request.CollectorRequest;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.mysema.query.BooleanBuilder;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.joda.time.LocalDate;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CodeQualityServiceImpl implements CodeQualityService {


    private static final Logger LOGGER = LoggerFactory.getLogger(CodeQualityServiceImpl.class);

    private final CodeQualityRepository codeQualityRepository;
    private final ComponentRepository componentRepository;
    private final CollectorRepository collectorRepository;
    private final SonarProjectRepository sonarProjectRepository;
    private final CollectorService collectorService;
    private RestClient restClient = new RestClient(new RestOperationsSupplier());

    @Autowired
    public CodeQualityServiceImpl(CodeQualityRepository codeQualityRepository,
                                  ComponentRepository componentRepository,
                                  CollectorRepository collectorRepository,
                                  SonarProjectRepository sonarProjectRepository,
                                  CollectorService collectorService) {
        this.codeQualityRepository = codeQualityRepository;
        this.componentRepository = componentRepository;
        this.collectorRepository = collectorRepository;
        this.sonarProjectRepository = sonarProjectRepository;
        this.collectorService = collectorService;
    }

    @Override
    public DataResponse<Iterable<CodeQuality>> search(CodeQualityRequest request) {
        if (request == null) {
            return emptyResponse();
        }

        if (request.getType() == null) { // return whole model
            // TODO: but the dataresponse needs changing.. the timestamp breaks this ability.
//            Iterable<CodeQuality> concatinatedResult = ImmutableList.of();
//            for (CodeQualityType type : CodeQualityType.values()) {
//                request.setType(type);
//                DataResponse<Iterable<CodeQuality>> result = searchType(request);
//                Iterables.concat(concatinatedResult, result.getResult());
//            }
            return emptyResponse();
        }

        return searchType(request);
    }

    private DataResponse<Iterable<CodeQuality>> emptyResponse() {
        return new DataResponse<>(null, System.currentTimeMillis());
    }

    private DataResponse<Iterable<CodeQuality>> searchType(CodeQualityRequest request) {
        CollectorItem item = getCollectorItem(request);
        if (item == null) {
            return emptyResponse();
        }

        QCodeQuality quality = new QCodeQuality("quality");
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(quality.collectorItemId.eq(item.getId()));

        if (request.getNumberOfDays() != null) {
            long endTimeTarget =
                    new LocalDate().minusDays(request.getNumberOfDays()).toDate().getTime();
            builder.and(quality.timestamp.goe(endTimeTarget));
        } else if (request.validDateRange()) {
            builder.and(quality.timestamp.between(request.getDateBegins(), request.getDateEnds()));
        }
        Iterable<CodeQuality> result;
        if (request.getMax() == null) {
            result = codeQualityRepository.findAll(builder.getValue(), quality.timestamp.desc());
        } else {
            PageRequest pageRequest =
                    new PageRequest(0, request.getMax(), Sort.Direction.DESC, "timestamp");
            result = codeQualityRepository.findAll(builder.getValue(), pageRequest).getContent();
        }
        String instanceUrl = (String)item.getOptions().get("instanceUrl");
        String projectId = (String) item.getOptions().get("projectId");
        String reportUrl = getReportURL(instanceUrl,"dashboard/index/",projectId);
        Collector collector = collectorRepository.findOne(item.getCollectorId());
        long lastExecuted = (collector == null) ? 0 : collector.getLastExecuted();
        return new DataResponse<>(result, lastExecuted,reportUrl);
    }


    protected CollectorItem getCollectorItem(CodeQualityRequest request) {
        Component component = componentRepository.findOne(request.getComponentId());
        if (component == null) {
            return null;
        }

        CodeQualityType qualityType = Objects.firstNonNull(request.getType(), CodeQualityType.StaticAnalysis);

        return component.getLastUpdatedCollectorItemForType(qualityType.collectorType());
    }

    protected CodeQuality createCodeQuality(CodeQualityCreateRequest request) throws HygieiaException {
        /*
          Step 1: create Collector if not there
          Step 2: create Collector item if not there
          Step 3: Insert Quality data if new. If existing, update it.
         */
        Collector collector = createCollector(request);

        if (collector == null) {
            throw new HygieiaException("Failed creating code quality collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }

        CollectorItem collectorItem = createCollectorItem(collector, request);

        if (collectorItem == null) {
            throw new HygieiaException("Failed creating code quality collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }

        CodeQuality quality = createCodeQuality(collectorItem, request);

        if (quality == null) {
            throw new HygieiaException("Failed inserting/updating Code Quality information.", HygieiaException.ERROR_INSERTING_DATA);
        }

        return quality;

    }

    @Override
    public String create(CodeQualityCreateRequest request) throws HygieiaException {
        CodeQuality quality = createCodeQuality(request);
        return quality.getId().toString();
    }

    @Override
    public String createV2(CodeQualityCreateRequest request) throws HygieiaException {
        CodeQuality quality = createCodeQuality(request);
        return quality.getId().toString() + "," + quality.getCollectorItemId().toString();

    }

    private Collector createCollector(CodeQualityCreateRequest request) {
        CollectorRequest collectorReq = new CollectorRequest();
        collectorReq.setName(StringUtils.isEmpty(request.getToolName()) ? "Sonar" : request.getToolName());
        collectorReq.setCollectorType(CollectorType.CodeQuality);
        Collector col = collectorReq.toCollector();
        col.setEnabled(true);
        col.setOnline(true);
        col.setLastExecuted(System.currentTimeMillis());
        return collectorService.createCollector(col);
    }

    private CollectorItem createCollectorItem(Collector collector, CodeQualityCreateRequest request) throws HygieiaException {
        CollectorItem tempCi = new CollectorItem();
        tempCi.setCollectorId(collector.getId());
        tempCi.setDescription(request.getProjectName());
        tempCi.setPushed(true);
        tempCi.setLastUpdated(System.currentTimeMillis());
        Map<String, Object> option = new HashMap<>();
        option.put("projectName", request.getProjectName());
        option.put("projectId", request.getProjectId());
        option.put("instanceUrl", request.getServerUrl());
        tempCi.getOptions().putAll(option);
        tempCi.setNiceName(request.getNiceName());

        if (StringUtils.isEmpty(tempCi.getNiceName())) {
            return collectorService.createCollectorItem(tempCi);
        }
        return collectorService.createCollectorItemByNiceNameAndProjectId(tempCi, request.getProjectId());
    }

    private CodeQuality createCodeQuality(CollectorItem collectorItem, CodeQualityCreateRequest request) {
        CodeQuality quality = codeQualityRepository.findByCollectorItemIdAndTimestamp(
                collectorItem.getId(), request.getTimestamp());
        if (quality == null) {
            quality = new CodeQuality();
        }
        quality.setCollectorItemId(collectorItem.getId());
        quality.setBuildId(new ObjectId(request.getHygieiaId()));
        quality.setName(request.getProjectName());
        quality.setType(CodeQualityType.StaticAnalysis);
        quality.setUrl(request.getProjectUrl());
        quality.setVersion(request.getProjectVersion());
        quality.setTimestamp(System.currentTimeMillis());
        for (CodeQualityMetric cm : request.getMetrics()) {
            quality.getMetrics().add(cm);
        }
        return codeQualityRepository.save(quality); // Save = Update (if ID present) or Insert (if ID not there)
    }

    // get projectUrl and projectId from collectorItem and form reportUrl
    private String getReportURL(String projectUrl,String path,String projectId) {
        StringBuilder sb = new StringBuilder(projectUrl);
        if(!projectUrl.endsWith("/")) {
            sb.append("/");
        }
        sb.append(path)
                .append(projectId);
        return sb.toString();
    }

    /**
     * Sync code quality static analysis data from one server to the another
     * @param request
     * @return
     * @throws HygieiaException
     */
    public ResponseEntity<String> syncData(CodeQualityDataSyncRequest request) throws HygieiaException {

        String getVersionEpt = "/api/server/version";
        List<SonarProject> updatedProjects = new ArrayList<>();
        String from = request.getSyncFrom();
        String to = request.getSyncTo();
        boolean isSync = request.isSync();
        final AtomicInteger index = new AtomicInteger();
        final AtomicInteger compIndex = new AtomicInteger();
        Collector collector;

        try {
            if (Strings.isNullOrEmpty(from) ||
                    Strings.isNullOrEmpty(to) ||
                    !HttpStatus.OK.equals(restClient.makeRestCallGet(from + getVersionEpt).getStatusCode()) ||
                    !HttpStatus.OK.equals(restClient.makeRestCallGet(to + getVersionEpt).getStatusCode())) {
                throw new HygieiaException("Invalid arguments...", HygieiaException.INVALID_CONFIGURATION);
            }
            collector = collectorRepository.findByName("Sonar");
            if (collector == null) {
                throw new HygieiaException("Collector not found", HygieiaException.COLLECTOR_CREATE_ERROR);
            }
        } catch (Exception e) {
            throw new HygieiaException("Invalid arguments...", HygieiaException.INVALID_CONFIGURATION);
        }
        List<SonarProject> projects = getCodeQualityProjects(to);
        projects.stream().forEach(project -> {
            String projectName = project.getProjectName();
            Optional<SonarProject> existingSonarProjectOpt = Optional.ofNullable(sonarProjectRepository.findSonarProjectByProjectName(
                    collector.getId(), from, projectName));
            existingSonarProjectOpt.ifPresent(eSonarProject -> {
                eSonarProject.setProjectId(project.getProjectId());
                eSonarProject.setInstanceUrl(to);
                updatedProjects.add(eSonarProject);
                updateComponent(eSonarProject, isSync, compIndex);
                LOGGER.info((index.getAndIncrement() + " : " + projectName + "'s project id & instance url updated"));
            });
        });

        String math = updatedProjects.size() + "/" + projects.size();
        String message = math + " code quality projects and " + compIndex + " dashboard components can be updated";
        if (isSync) {
            sonarProjectRepository.save(updatedProjects);
            message = math + " code quality projects and " + compIndex + " dashboard components updated";
        }
        LOGGER.info(message);
        return ResponseEntity.ok(message);
    }

    /**
     * Get code quality projects
     * @param serverUrl
     * @return List
     * @throws HygieiaException
     */
    private List<SonarProject> getCodeQualityProjects(String serverUrl) throws HygieiaException {

        String getProjectsEpt = serverUrl + "/api/components/search?qualifiers=TRK&ps=500";
        JSONParser jsonParser = new JSONParser();
        JSONArray jsonArray = new JSONArray();
        List<SonarProject> projects = new ArrayList<>();

        try {
            ResponseEntity<String> response = restClient.makeRestCallGet(getProjectsEpt);
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
                    response = restClient.makeRestCallGet(urlFinal);
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
                if (eSonarProject.getProjectName().equals((String) collectorItem.getOptions().get("projectName"))){
                    collectorItem.getOptions().put("projectId", eSonarProject.getProjectId());
                    collectorItem.getOptions().put("instanceUrl", eSonarProject.getInstanceUrl());
                    if (null != compIndex) { compIndex.getAndIncrement(); }
                }
                codeQualityCollectorItems.add(collectorItem);
            });
            component.setCollectorItems(Collections.singletonMap(CollectorType.CodeQuality, codeQualityCollectorItems));
            if (isSync) { componentRepository.save(component); }
        });

    }
}
