package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.EnvironmentComponent;
import com.capitalone.dashboard.model.EnvironmentStatus;
import com.capitalone.dashboard.model.deploy.DeployableUnit;
import com.capitalone.dashboard.model.deploy.Environment;
import com.capitalone.dashboard.model.deploy.Server;
import com.capitalone.dashboard.repository.BuildRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.EnvironmentComponentRepository;
import com.capitalone.dashboard.repository.EnvironmentStatusRepository;
import com.capitalone.dashboard.request.BuildDataCreateRequest;
import com.capitalone.dashboard.request.CollectorRequest;
import com.capitalone.dashboard.request.DeployDataCreateRequest;
import com.capitalone.dashboard.response.BuildDataCreateResponse;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.capitalone.dashboard.service.DeployServiceImpl.RundeckXMLParser.getAttributeValue;
import static com.capitalone.dashboard.service.DeployServiceImpl.RundeckXMLParser.getChildNodeAttribute;
import static com.capitalone.dashboard.service.DeployServiceImpl.RundeckXMLParser.getChildNodeValue;

@Service
public class DeployServiceImpl implements DeployService {

    private static final Pattern INSTANCE_URL_PATTERN = Pattern.compile("https?://[^/]*");
    private static final String DEFAULT_COLLECTOR_NAME = "Jenkins";
    private static final String DEFAULT_DEPLOY_COLLECTOR_NAME = "JenkinsDeploy";
    private static final String PARAM = "Param";

    private final ComponentRepository componentRepository;
    private final EnvironmentComponentRepository environmentComponentRepository;
    private final EnvironmentStatusRepository environmentStatusRepository;
    private final CollectorRepository collectorRepository;
    private final CollectorItemRepository collectorItemRepository;
    private final BuildRepository buildRepository;
    private final CollectorService collectorService;
    private final BuildService buildService;


    @Autowired
    public DeployServiceImpl(ComponentRepository componentRepository,
                             EnvironmentComponentRepository environmentComponentRepository,
                             EnvironmentStatusRepository environmentStatusRepository,
                             CollectorRepository collectorRepository, CollectorItemRepository collectorItemRepository,
                             CollectorService collectorService, BuildRepository buildRepository,
                             BuildService buildService) {
        this.componentRepository = componentRepository;
        this.environmentComponentRepository = environmentComponentRepository;
        this.environmentStatusRepository = environmentStatusRepository;
        this.collectorRepository = collectorRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.collectorService = collectorService;
        this.buildRepository = buildRepository;
        this.buildService = buildService;
    }

    @Override
    public DataResponse<List<Environment>> getDeployStatus(ObjectId componentId) {
        Component component = componentRepository.findOne(componentId);

        Collection<CollectorItem> cis = component.getCollectorItems()
                .get(CollectorType.Deployment);

        return getDeployStatus(cis);
    }

    private DataResponse<List<Environment>> getDeployStatus(Collection<CollectorItem> deployCollectorItems) {
        List<Environment> environments = new ArrayList<>();
        long lastExecuted = 0;

        if (deployCollectorItems == null) {
            return new DataResponse<>(environments, 0);
        }

        // We will assume that if the component has multiple deployment collectors
        // then each collector will have a different url which means each Environment will be different
        for (CollectorItem item : deployCollectorItems) {
            ObjectId collectorItemId = item.getId();

            List<EnvironmentComponent> components = environmentComponentRepository
                    .findByCollectorItemId(collectorItemId);
            List<EnvironmentStatus> statuses = environmentStatusRepository
                    .findByCollectorItemId(collectorItemId);

            groupByEnvironment(
                    components).forEach((env, value) -> {
                environments.add(env);
                value.forEach(envComponent -> env.getUnits().add(
                        new DeployableUnit(envComponent, servers(envComponent,
                                statuses))));
            });

            Collector collector = collectorRepository
                    .findOne(item.getCollectorId());

            if (collector.getLastExecuted() > lastExecuted) {
                lastExecuted = collector.getLastExecuted();
            }
        }
        return new DataResponse<>(environments, lastExecuted);
    }

    private Map<Environment, List<EnvironmentComponent>> groupByEnvironment(
            List<EnvironmentComponent> components) {
        Map<Environment, Map<String, EnvironmentComponent>> trackingMap = new LinkedHashMap<>();
        //two conditions to overwrite the value for the specific component
        components.forEach(component -> {
            Environment env = new Environment(component.getEnvironmentName(),
                    component.getEnvironmentUrl());
            if (!trackingMap.containsKey(env)) {
                trackingMap.put(env, new LinkedHashMap<>());
            }
            if (trackingMap.get(env).get(component.getComponentName()) == null ||
                    component.getAsOfDate() > trackingMap.get(env)
                            .get(component.getComponentName()).getAsOfDate()) {
                trackingMap.get(env).put(component.getComponentName(), component);
            }
        });

        //flatten the deeper map into a list
        return trackingMap.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                        e.getValue().entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Iterable<Server> servers(final EnvironmentComponent component,
                                     List<EnvironmentStatus> statuses) {
        return Iterables.transform(
                Iterables.filter(statuses, new ComponentMatches(component)),
                new ToServer());
    }

    private class ComponentMatches implements Predicate<EnvironmentStatus> {
        private EnvironmentComponent component;

        public ComponentMatches(EnvironmentComponent component) {
            this.component = component;
        }

        @Override
        public boolean apply(EnvironmentStatus environmentStatus) {
            return Objects.equals(environmentStatus.getEnvironmentName(), component.getEnvironmentName())
                    && Objects.equals(environmentStatus.getComponentName(), component.getComponentName());
        }
    }

    private class ToServer implements com.google.common.base.Function<EnvironmentStatus, Server> {
        @Override
        public Server apply(EnvironmentStatus status) {
            return new Server(status.getResourceName(), status.isOnline());
        }
    }


    protected EnvironmentComponent createDeploy(DeployDataCreateRequest request, boolean associateBuild) throws HygieiaException {
        /*
          Step 1: create Collector if not there
          Step 2: create Collector item if not there
          Step 3: Insert build data if new. If existing, update it.
         */
        Collector collector = createCollector(request);

        if (collector == null) {
            throw new HygieiaException("Failed creating Deploy collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }

        CollectorItem collectorItem = createCollectorItem(collector, request);

        if (collectorItem == null) {
            throw new HygieiaException("Failed creating Deploy collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }

        EnvironmentComponent deploy = createEnvComponent(collectorItem, request, associateBuild);

        if (deploy == null) {
            throw new HygieiaException("Failed inserting/updating Deployment information.", HygieiaException.ERROR_INSERTING_DATA);
        }

        return deploy;

    }

    @Override
    public String create(DeployDataCreateRequest request) throws HygieiaException {
        EnvironmentComponent deploy = createDeploy(request, Boolean.FALSE);
        return deploy.getId().toString();
    }

    @Override
    public String createV2(DeployDataCreateRequest request) throws HygieiaException {
        EnvironmentComponent deploy = createDeploy(request, Boolean.FALSE);
        return String.format("%s,%s", deploy.getId().toString(), deploy.getCollectorItemId().toString());

    }

    public String createV3(DeployDataCreateRequest request) throws HygieiaException {
        /*
          Step 1: create JenkinsDeploy Collector if not there
          Step 2: create Collector item if not there
          Step 3: Insert build data if new. If existing, update it.
         */

        Collector collector = createJenkinsDeployCollector(request);

        if (collector == null) {
            throw new HygieiaException("Failed creating Deploy collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }

        CollectorItem collectorItem = createJenkinsDeployCollectorItem(collector, request);

        if (collectorItem == null) {
            throw new HygieiaException("Failed creating Deploy collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }

        if(StringUtils.isNotEmpty(request.getJobNumber())) {
            // find the build collectorItem associated
            // next find the associated build using number if not found then create it
            // enrich the build.
            BuildDataCreateRequest buildRequest = buildRequestFromDeployRequest(request);
            BuildDataCreateResponse buildResponse = buildService.createV3(buildRequest);
            ObjectId buildId = buildResponse.getId();
            Build build  = buildRepository.findOne(buildId);
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("appName", request.getAppName());
            metadata.put("appServiceName",request.getAppServiceName());
            metadata.put("artifactName", request.getArtifactName());
            metadata.put("artifactVersion", request.getArtifactVersion());
            metadata.put("artifactPath", request.getArtifactPath());
            metadata.put("artifactGroup", request.getArtifactGroup());
            metadata.put("envName", request.getEnvName());
            metadata.put("executionId",request.getExecutionId());
            build.setDeployMetadata(metadata);
            buildRepository.save(build);
            return String.format("%s,%s", build.getId().toString(), build.getCollectorItemId().toString());
        }

        return String.format("%s,%s", "deployId", "deployCollectorItemId");

    }

    private BuildDataCreateRequest buildRequestFromDeployRequest(DeployDataCreateRequest request) {
        BuildDataCreateRequest buildRequest = new BuildDataCreateRequest();
        buildRequest.setBuildStatus("InProgress");
        buildRequest.setNumber(request.getJobNumber());
        buildRequest.setStartTime(request.getStartTime());
        buildRequest.setJobName(request.getJobName());
        buildRequest.setBuildUrl(request.getBuildUrl());
        buildRequest.setNiceName(request.getNiceName());
        buildRequest.setJobUrl(request.getJobUrl());
        buildRequest.setInstanceUrl(request.getInstanceUrl());
        return buildRequest;
    }

    @Override
    public DataResponse<List<Environment>> getDeployStatus(String applicationName) {
        List<Collector> collectorList = collectorRepository.findByCollectorType(CollectorType.Deployment);
        for (Collector collector : collectorList) {
            List<CollectorItem> cis = collectorItemRepository.findByOptionsAndDeployedApplicationName(collector.getId(), applicationName);
            if (!cis.isEmpty()) {
                return getDeployStatus(cis);
            }
        }
        return new DataResponse<>(null, 0);
    }

    private Collector createCollector(DeployDataCreateRequest request) {
        CollectorRequest collectorReq = new CollectorRequest();
        String collectorName = request.getCollectorName();
        if (StringUtils.isBlank(collectorName)) {
            collectorName = DEFAULT_COLLECTOR_NAME;
        }
        collectorReq.setName(collectorName);
        collectorReq.setCollectorType(CollectorType.Deployment);
        Collector col = collectorReq.toCollector();
        col.setEnabled(true);
        col.setOnline(true);
        col.setLastExecuted(System.currentTimeMillis());

        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put("applicationName", "");
        allOptions.put("instanceUrl", "");
        col.getAllFields().putAll(allOptions);
        col.getUniqueFields().putAll(allOptions);
        return collectorService.createCollector(col);
    }

    private Collector createJenkinsDeployCollector(DeployDataCreateRequest request) {
        CollectorRequest collectorReq = new CollectorRequest();
        String collectorName = request.getCollectorName();
        if (StringUtils.isBlank(collectorName)) {
            collectorName = DEFAULT_DEPLOY_COLLECTOR_NAME;
        }
        collectorReq.setName(collectorName);
        collectorReq.setCollectorType(CollectorType.Deployment);
        Collector col = collectorReq.toCollector();
        col.setEnabled(true);
        col.setOnline(true);
        col.setLastExecuted(System.currentTimeMillis());

        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put("jobName", "");
        allOptions.put("jobUrl", "");
        allOptions.put("instanceUrl", "");
        col.getAllFields().putAll(allOptions);

        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put("jobName", "");
        uniqueOptions.put("jobUrl", "");
        col.getUniqueFields().putAll(uniqueOptions);

        String[] searchFields  = { "options.jobName" , "nicename" };
        col.setSearchFields(Arrays.asList(searchFields));

        return collectorService.createCollector(col);
    }

    private CollectorItem createJenkinsDeployCollectorItem(Collector collector, DeployDataCreateRequest request) {
        CollectorItem tempCi = new CollectorItem();
        tempCi.setCollectorId(collector.getId());
        tempCi.setDescription(request.getAppName());
        tempCi.setPushed(true);
        tempCi.setLastUpdated(System.currentTimeMillis());
        tempCi.setNiceName(request.getNiceName());
        Map<String, Object> option = new HashMap<>();
        option.put("jobName", request.getJobName());
        option.put("jobUrl", request.getJobUrl());
        option.put("instanceUrl", request.getInstanceUrl());
        tempCi.getOptions().putAll(option);

        return collectorService.createCollectorItem(tempCi);
    }

    private CollectorItem createCollectorItem(Collector collector, DeployDataCreateRequest request) {
        CollectorItem tempCi = new CollectorItem();
        tempCi.setCollectorId(collector.getId());
        tempCi.setDescription(request.getAppName());
        tempCi.setPushed(true);
        tempCi.setLastUpdated(System.currentTimeMillis());
        tempCi.setNiceName(request.getNiceName());
        Map<String, Object> option = new HashMap<>();
        option.put("applicationName", request.getAppName());
        option.put("instanceUrl", request.getInstanceUrl());
        tempCi.getOptions().putAll(option);

        return collectorService.createCollectorItem(tempCi);
    }

    private EnvironmentComponent createEnvComponent(CollectorItem collectorItem, DeployDataCreateRequest request, boolean associateBuild) {
        EnvironmentComponent deploy = environmentComponentRepository.
                findByUniqueKey(collectorItem.getId(), request.getArtifactName(), request.getArtifactName(), request.getEndTime());
        if (deploy == null) {
            deploy = new EnvironmentComponent();
        }
        deploy.setChangeReference(request.getExecutionId());
        deploy.setAsOfDate(System.currentTimeMillis());
        deploy.setCollectorItemId(collectorItem.getId());
        deploy.setComponentID(request.getArtifactGroup());
        deploy.setComponentName(request.getArtifactName());
        deploy.setComponentPath( request.getArtifactPath());
        deploy.setServiceName (request.getAppServiceName());
        deploy.setApplicationName (request.getAppName());
        deploy.setComponentVersion(request.getArtifactVersion());
        deploy.setCollectorItemId(collectorItem.getId());
        deploy.setEnvironmentName(request.getEnvName());
        deploy.setEnvironmentUrl(request.getInstanceUrl());
        deploy.setJobUrl(request.getJobUrl());
        deploy.setDeployTime(request.getEndTime());
        deploy.setJobStageName(request.getStageName());
        deploy.setJobStageStatus(request.getStageStatus());
        if(associateBuild) {
            associateBuildToDeploy(request, deploy);
        }
        deploy.setDeployed("SUCCESS".equalsIgnoreCase(request.getDeployStatus()));

        return environmentComponentRepository.save(deploy); // Save = Update (if ID present) or Insert (if ID not there)
    }

    private void associateBuildToDeploy(DeployDataCreateRequest request, EnvironmentComponent deploy) {

        String collectorName = StringUtils.isNotEmpty(request.getCollectorName()) ? request.getCollectorName() : "Hudson";
        List<Collector> collectors = collectorRepository.findByCollectorTypeAndName(CollectorType.Build, collectorName);
        Optional<Collector> collector = collectors.stream().findFirst();
        if(!collector.isPresent()) return;

        ObjectId buildCollectorId = collector.get().getId();
        Map<String, Object> option = new HashMap<>();
        option.put("jobName", request.getJobName());
        option.put("jobUrl", request.getJobUrl());
        option.put("instanceUrl", request.getInstanceUrl());

        List<CollectorItem> buildCollectorItems = IterableUtils.toList(collectorItemRepository.findAllByOptionMapAndCollectorIdsIn(option, Stream.of(buildCollectorId).collect(Collectors.toList())));
        Optional<CollectorItem> enabledCollectorItem = buildCollectorItems.stream().filter(CollectorItem::isEnabled).findFirst();
        if(!enabledCollectorItem.isPresent()) return; // Need to handle the case of create build collector item later

        Build build =buildRepository.findByCollectorItemIdAndNumber(enabledCollectorItem.get().getId(), request.getJobNumber());
        if(build == null) return; // Need to handle the case of create build later
        deploy.setBuildId(build.getId());
    }

    protected DeployDataCreateRequest createRundeck(Document doc, Map<String, String[]> parameters, String executionId, String status) throws HygieiaException {
        Node executionNode = doc.getElementsByTagName("execution").item(0);
        Node jobNode = executionNode.getFirstChild();
        RundeckXMLParser p = new RundeckXMLParser(doc);
        DeployDataCreateRequest request = new DeployDataCreateRequest();
        request.setExecutionId(executionId);
        request.setDeployStatus(status.toUpperCase());
        String appNameOption = evaluateParametersOrDefault(parameters, p, "appName", false, "appName");
        if (appNameOption == null) {
            appNameOption = getAttributeValue(executionNode, "project");
        }
        request.setAppName(appNameOption);
        request.setEnvName(evaluateParametersOrDefault(parameters, p, "envName", true,"env"));
        request.setArtifactName(evaluateParametersOrDefault(parameters, p, "artifactName", true, "artifactName"));
        request.setArtifactGroup(evaluateParametersOrDefault(parameters, p, "artifactGroup", false, "group"));
        request.setArtifactVersion(evaluateParametersOrDefault(parameters, p, "artifactVersion", true, "version"));
        request.setNiceName(evaluateParametersOrDefault(parameters, p, "niceName", false, "niceName"));
        request.setStartedBy(getChildNodeValue(executionNode, "user"));
        request.setStartTime(Long.valueOf(getChildNodeAttribute(executionNode, "date-started", "unixtime")));
        request.setEndTime(Long.valueOf(getChildNodeAttribute(executionNode, "date-ended", "unixtime")));
        request.setDuration(request.getEndTime() - request.getStartTime());
        request.setJobUrl(getAttributeValue(executionNode, "href"));
        Matcher matcher = INSTANCE_URL_PATTERN.matcher(request.getJobUrl());
        if (matcher.find()) {
            request.setInstanceUrl(matcher.group());
        }
        request.setJobName(getChildNodeValue(jobNode, "name"));
        return request;
    }

    @Override
    public String createRundeckBuild(Document doc, Map<String, String[]> parameters, String executionId, String status) throws HygieiaException {
        DeployDataCreateRequest request = createRundeck(doc, parameters, executionId, status);
        return create(request);
    }

    @Override
    public String createRundeckBuildV2(Document doc, Map<String, String[]> parameters, String executionId, String status) throws HygieiaException {
        DeployDataCreateRequest request = createRundeck(doc, parameters, executionId, status);
        return createV2(request);
    }

    private String evaluateParametersOrDefault(Map<String, String[]> params,
                                               RundeckXMLParser p, String name, boolean required, String defaultOptions) throws HygieiaException {
        String output;
        if (params.containsKey(name)) {
            output = params.get(name)[0];
        } else if (params.containsKey(name + PARAM)) {
            output = p.findMatchingOption(params.get(name + PARAM));
        } else {
            output = p.findMatchingOptionRegex(defaultOptions);
        }
        if (required && output == null) {
            throw new HygieiaException(name + " option is required and not available.  " +
                    "Please check the documentation and provide the option value.", 500);
        }
        return output;
    }

    static class RundeckXMLParser {

        private NodeList nodes;
        private final Map<String, Node> optionNameNode;

        public RundeckXMLParser(Document doc) {
            nodes = doc.getElementsByTagName("option");
            optionNameNode = IntStream.range(0, nodes.getLength())
                    .mapToObj(i -> nodes.item(i))
                    .collect(Collectors.toMap(n -> getAttributeValue(n, "name"), Function.identity()));
        }

        public static String getAttributeValue(Node node, String attributeName) {
            if (node == null) {
                return null;
            }
            Node attributeNode = node.getAttributes().getNamedItem(attributeName);
            if (attributeNode == null) {
                return null;
            } else {
                return attributeNode.getNodeValue();
            }
        }

        public static String getChildNodeAttribute(Node node, String childNodeName, String attributeName) {
            return actOnChildNode(node, childNodeName, n -> getAttributeValue(n, attributeName));
        }

        public static String getChildNodeValue(Node node, String childNodeName) {
            return actOnChildNode(node, childNodeName, Node::getNodeValue);
        }

        public static String actOnChildNode(Node node, String childNodeName, Function<Node, String> valueSupplier) {
            Optional<Node> childNode = getNamedChild(node, childNodeName);
            return childNode.map(valueSupplier::apply).orElse(null);
        }

        public static Optional<Node> getNamedChild(Node node, String childNodeName) {
            NodeList nodes = node.getChildNodes();
            return IntStream.range(0, nodes.getLength())
                    .filter(i -> Objects.equals(childNodeName, nodes.item(i).getNodeName()))
                    .mapToObj(i -> nodes.item(i))
                    .findFirst();
        }

        public String findMatchingOption(String... optionNames) {
            List<String> options = Arrays.asList(optionNames);
            return options.stream().filter(opt -> optionNameNode.keySet().contains(opt))
                    .findFirst()
                    .map(opt -> getAttributeValue(optionNameNode.get(opt), "value")).orElse(null);


        }

        public String findMatchingOptionRegex(String regexOption){
            Pattern p = Pattern.compile(".*?"+regexOption+".*",Pattern.CASE_INSENSITIVE );
            Node node = optionNameNode.entrySet()
                    .stream()
                    .filter(entry -> p.matcher(entry.getKey()).matches())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet()).stream().findFirst().orElse(null);
            return  getAttributeValue(node,"value");
        }
    }

}
