package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.model.quality.CucumberJsonReport;
import com.capitalone.dashboard.model.quality.JunitXmlReport;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.TestResultRepository;
import com.capitalone.dashboard.request.CollectorRequest;
import com.capitalone.dashboard.request.PerfTestDataCreateRequest;
import com.capitalone.dashboard.request.TestDataCreateRequest;
import com.capitalone.dashboard.settings.ApiSettings;
import com.capitalone.dashboard.util.TestResultConstants;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mysema.query.BooleanBuilder;
import hygieia.transformer.CucumberJsonToTestCapabilityTransformer;
import hygieia.transformer.JunitXmlToTestCapabilityTransformer;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TestResultServiceImpl implements TestResultService {

    private final TestResultRepository testResultRepository;
    private final ComponentRepository componentRepository;
    private final CollectorRepository collectorRepository;
    private final CollectorService collectorService;
    private final CmdbService cmdbService;
    private final ApiSettings apiSettings;



    @Autowired
    public TestResultServiceImpl(TestResultRepository testResultRepository,
                                 ComponentRepository componentRepository,
                                 CollectorRepository collectorRepository,
                                 CollectorService collectorService,
                                 CmdbService cmdbService,
                                 ApiSettings apiSettings) {
        this.testResultRepository = testResultRepository;
        this.componentRepository = componentRepository;
        this.collectorRepository = collectorRepository;
        this.collectorService = collectorService;
        this.cmdbService = cmdbService;
        this.apiSettings = apiSettings;
    }

    @Override
    public DataResponse<Iterable<TestResult>> search(com.capitalone.dashboard.request.TestResultRequest request) {
        Component component = componentRepository.findOne(request.getComponentId());

        if ((component == null) || !component.getCollectorItems().containsKey(CollectorType.Test)) {
            return new DataResponse<>(null, 0L);
        }
        List<TestResult> result = new ArrayList<>();
        validateAllCollectorItems(request, component, result);
        //One collector per Type. get(0) is hardcoded.
        if (!CollectionUtils.isEmpty(component.getCollectorItems().get(CollectorType.Test)) && (component.getCollectorItems().get(CollectorType.Test).get(0) != null)) {
            Collector collector = collectorRepository.findOne(component.getCollectorItems().get(CollectorType.Test).get(0).getCollectorId());
            if (collector != null) {
                return new DataResponse<>(pruneToDepth(result, request.getDepth()), collector.getLastExecuted());
            }
        }

        return new DataResponse<>(null, 0L);
    }



    private void validateAllCollectorItems(com.capitalone.dashboard.request.TestResultRequest request, Component component, List<TestResult> result) {
        // add all test result repos
        component.getCollectorItems().get(CollectorType.Test).forEach(item -> {
            QTestResult testResult = new QTestResult("testResult");
            BooleanBuilder builder = new BooleanBuilder();
            builder.and(testResult.collectorItemId.eq(item.getId()));
            validateStartDateRange(request, testResult, builder);
            validateEndDateRange(request, testResult, builder);
            validateDurationRange(request, testResult, builder);
            validateTestCapabilities(request, testResult, builder);
            addAllTestResultRepositories(request, result, testResult, builder);
        });
    }

    private void addAllTestResultRepositories(com.capitalone.dashboard.request.TestResultRequest request, List<TestResult> result, QTestResult testResult, BooleanBuilder builder) {
        if (request.getMax() == null) {
            result.addAll(Lists.newArrayList(testResultRepository.findAll(builder.getValue(), testResult.timestamp.desc())));
        } else {
            PageRequest pageRequest = new PageRequest(0, request.getMax(), Sort.Direction.DESC, "timestamp");
            result.addAll(Lists.newArrayList(testResultRepository.findAll(builder.getValue(), pageRequest).getContent()));
        }
    }

    private void validateTestCapabilities(com.capitalone.dashboard.request.TestResultRequest request, QTestResult testResult, BooleanBuilder builder) {
        if (!request.getTypes().isEmpty()) {
            builder.and(testResult.testCapabilities.any().type.in(request.getTypes()));
        }
    }

    private void validateDurationRange(com.capitalone.dashboard.request.TestResultRequest request, QTestResult testResult, BooleanBuilder builder) {
        if (request.validDurationRange()) {
            builder.and(testResult.duration.between(request.getDurationGreaterThan(), request.getDurationLessThan()));
        }
    }

    private void validateEndDateRange(com.capitalone.dashboard.request.TestResultRequest request, QTestResult testResult, BooleanBuilder builder) {
        if (request.validEndDateRange()) {
            builder.and(testResult.endTime.between(request.getEndDateBegins(), request.getEndDateEnds()));
        }
    }

    private void validateStartDateRange(com.capitalone.dashboard.request.TestResultRequest request, QTestResult testResult, BooleanBuilder builder) {
        if (request.validStartDateRange()) {
            builder.and(testResult.startTime.between(request.getStartDateBegins(), request.getStartDateEnds()));
        }
    }

    private Iterable<TestResult> pruneToDepth(List<TestResult> results, Integer depth) {
        // Prune the response to the requested depth
        // 0 - TestResult
        // 1 - TestCapability
        // 2 - TestSuite
        // 3 - TestCase
        // 4 - Entire response
        // null - Entire response
        if (depth == null || depth > 3) {
            return results;
        }
        results.forEach(result -> {
            if (depth == 0) {
                result.getTestCapabilities().clear();
            } else {
                result.getTestCapabilities().forEach(testCapability -> {
                    if (depth == 1) {
                        testCapability.getTestSuites().clear();
                    } else {
                        testCapability.getTestSuites().forEach(testSuite -> {
                            if (depth == 2) {
                                testSuite.getTestCases().clear();
                            } else { // depth == 3
                                testSuite.getTestCases().forEach(testCase -> {
                                    testCase.getTestSteps().clear();
                                });
                            }
                        });
                    }
                });
            }
        });

        return results;
    }


    protected TestResult createTest(TestDataCreateRequest request) throws HygieiaException {
        /*
          Step 1: create Collector if not there
          Step 2: create Collector item if not there
          Step 3: Insert test data if new. If existing, update it
         */
        Collector collector = createCollector();

        if (collector == null) {
            throw new HygieiaException("Failed creating Test collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }

        CollectorItem collectorItem = createCollectorItem(collector, request);

        if (collectorItem == null) {
            throw new HygieiaException("Failed creating Test collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }

        TestResult testResult = createTest(collectorItem, request);

        if (testResult == null) {
            throw new HygieiaException("Failed inserting/updating Test information.", HygieiaException.ERROR_INSERTING_DATA);
        }

        return testResult;

    }

    @Override
    public String create(TestDataCreateRequest request) throws HygieiaException {
        TestResult testResult = createTest(request);
        return testResult.getId().toString();
    }

    @Override
    public String createV2(TestDataCreateRequest request) throws HygieiaException {
        TestResult testResult = createTest(request);
        return testResult.getId().toString() + "," + testResult.getCollectorItemId().toString();
    }



    protected TestResult createPerfTest(PerfTestDataCreateRequest request) throws HygieiaException {
        /**
         * Step 1: create performance Collector if not there
         * Step 2: create Perfomance Collector item if not there
         * Step 3: Insert performance test data if new. If existing, update it
         */
        Collector collector = createGenericCollector(request.getPerfTool());
        if (collector == null) {
            throw new HygieiaException("Failed creating Test collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }
        CollectorItem collectorItem = createGenericCollectorItem(collector, request);
        if (collectorItem == null) {
            throw new HygieiaException("Failed creating Test collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }
        TestResult testResult = createPerfTest(collectorItem, request);
        if (testResult == null) {
            throw new HygieiaException("Failed inserting/updating Test information.", HygieiaException.ERROR_INSERTING_DATA);
        }
        return testResult;

    }

    protected TestResult createTestCucumber(TestCreateRequest request) throws HygieiaException {

        CucumberJsonReport.Feature cucumberFeature = null;

        try {
            cucumberFeature = decodeJsonPayload(CucumberJsonReport.Feature.class , request);
            if(cucumberFeature.getId() == null || cucumberFeature.getKeyword() == null || cucumberFeature.getName() == null || cucumberFeature.getElements() == null) {
                throw new HygieiaException("TestResult is not a valid json.", HygieiaException.JSON_FORMAT_ERROR);
            }
        }catch (Exception ex){
            throw new HygieiaException("TestResult is not a valid json.", HygieiaException.JSON_FORMAT_ERROR);
        }
        Collector collector = createGenericCollector(request, TestResultConstants.JENKINSCUCUMBERTEST);
        if (collector == null) {
            throw new HygieiaException("Failed creating Test collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }
        CollectorItem collectorItem = createGenericCollectorItem(collector, request , cucumberFeature.getName());
        if (collectorItem == null) {
            throw new HygieiaException("Failed creating Test collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }
        List<CucumberJsonReport.Feature> features = new ArrayList<>();
        features.add(cucumberFeature);
        CucumberJsonReport cucumberRequest = new CucumberJsonReport(features);
        CucumberJsonToTestCapabilityTransformer transformer = new CucumberJsonToTestCapabilityTransformer(null,"");
        TestCapability testCapability = transformer.convert(cucumberRequest);
        TestResult testResult = createTestCucumber(collectorItem,cucumberFeature , TestSuiteType.Functional, request, testCapability);
        if (testResult == null) {
            throw new HygieiaException("Failed inserting cucumber Test information.", HygieiaException.ERROR_INSERTING_DATA);
        }
        return testResult;

    }

    protected TestResult createTestJunit(TestCreateRequest request) throws HygieiaException {

        JunitXmlReport  junitXmlReport = decodeXmlPayload(JunitXmlReport.class,request);
        Collector collector = createGenericCollector(request, TestResultConstants.JUNITTEST);;
        if (collector == null) {
            throw new HygieiaException("Failed creating Test collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }
        CollectorItem collectorItem = createGenericCollectorItem(collector, request , junitXmlReport.getName() );
        if (collectorItem == null) {
            throw new HygieiaException("Failed creating Test collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }
        JunitXmlToTestCapabilityTransformer transformer = new JunitXmlToTestCapabilityTransformer();
        TestCapability testCapability = transformer.convert(junitXmlReport);
        TestResult testResult = createTestJunit(collectorItem, junitXmlReport,  TestSuiteType.Unit, request,testCapability);
        if (testResult == null) {
            throw new HygieiaException("Failed inserting Junit Test information.", HygieiaException.ERROR_INSERTING_DATA);
        }
        return testResult;

    }

    private <T> T decodeJsonPayload (Class<T> type , TestCreateRequest request) throws HygieiaException{
        if(request == null || StringUtils.isEmpty(request.getTestResult())) {
            throw new HygieiaException("TestResult is not a valid json.", HygieiaException.JSON_FORMAT_ERROR);
        }
        byte[] decodedBytes = Base64.getDecoder().decode(request.getTestResult());
        String decodedPayload = new String(decodedBytes);
        Gson gson = new Gson();
        return gson.fromJson(decodedPayload , type);

    }

    private <T> T decodeXmlPayload (Class<T> type , TestCreateRequest request) throws HygieiaException{

        if(request == null || StringUtils.isEmpty(request.getTestResult())) {
            throw new HygieiaException("TestResult is not a valid Xml", HygieiaException.JSON_FORMAT_ERROR);
        }
        byte[] decodedBytes = Base64.getDecoder().decode(request.getTestResult());
        String decodedPayload = new String(decodedBytes);
        StringReader sr = new StringReader(decodedPayload);
        T junitXmlReport = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(type);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            junitXmlReport = (T) unmarshaller.unmarshal(sr);
        }catch (JAXBException ex){
            ex.printStackTrace();
            throw new HygieiaException("TestResult is not a valid Xml", HygieiaException.JSON_FORMAT_ERROR);
        }
        return junitXmlReport;
    }

    @Override
    public String createTest(TestCreateRequest request) throws HygieiaException {
        TestResult testResult = null;
        validConfigurationItem(request.getConfigurationItem(),request.getTargetAppName());
        if (TestResultConstants.FUNCTIONAL.equals(request.getTestType()) && apiSettings.getFunctional().get("cucumber").equals(request.getSourceFormat())) {

            testResult = createTestCucumber(request);

        } else if (TestResultConstants.UNIT.equals(request.getTestType())&& apiSettings.getUnit().equals(request.getSourceFormat())) {

            testResult = createTestJunit(request);
        } else {
            return "Hygieia does not support " + request.getTestType() + " sourceFormat " + request.getSourceFormat();
        }
        return testResult.getId() + "," + testResult.getCollectorItemId();
    }


    @Override
    public String createPerf(PerfTestDataCreateRequest request) throws HygieiaException {
        TestResult testResult = createPerfTest(request);
        return testResult.getId().toString();
    }

    @Override
    public String createPerfV2(PerfTestDataCreateRequest request) throws HygieiaException {
        TestResult testResult = createPerfTest(request);
        return testResult.getId().toString() + "," + testResult.getCollectorItemId().toString();
    }

    private Collector createCollector() {
        CollectorRequest collectorReq = new CollectorRequest();
        collectorReq.setName("JenkinsCucumberTest");
        collectorReq.setCollectorType(CollectorType.Test);
        Collector col = collectorReq.toCollector();
        col.setEnabled(true);
        col.setOnline(true);
        col.setLastExecuted(System.currentTimeMillis());
        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put("jobUrl", "");
        allOptions.put("instanceUrl", "");
        allOptions.put("jobName","");
        allOptions.put("testType","");
        col.setAllFields(allOptions);
        //Combination of jobName and jobUrl should be unique always.
        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put("jobUrl", "");
        uniqueOptions.put("jobName","");
        uniqueOptions.put("testType","");
        col.setUniqueFields(uniqueOptions);
        return collectorService.createCollector(col);
    }


    private Collector createGenericCollector(String performanceTool) {
        CollectorRequest collectorReq = new CollectorRequest();
        collectorReq.setName(performanceTool);
        collectorReq.setCollectorType(CollectorType.Test);
        Collector col = collectorReq.toCollector();
        col.setEnabled(true);
        col.setOnline(true);
        col.setLastExecuted(System.currentTimeMillis());
        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put("jobName","");
        allOptions.put("instanceUrl", "");
        allOptions.put("testType","");
        col.setAllFields(allOptions);
        col.setUniqueFields(allOptions);
        return collectorService.createCollector(col);
    }

    private CollectorItem createCollectorItem(Collector collector, TestDataCreateRequest request) throws HygieiaException {
        CollectorItem tempCi = new CollectorItem();
        tempCi.setCollectorId(collector.getId());
        tempCi.setDescription(request.getDescription());
        tempCi.setPushed(true);
        tempCi.setLastUpdated(System.currentTimeMillis());
        Map<String, Object> option = new HashMap<>();
        option.put("jobName", request.getTestJobName());
        option.put("jobUrl", request.getTestJobUrl());
        option.put("instanceUrl", request.getServerUrl());
        option.put("testType",request.getType());
        tempCi.getOptions().putAll(option);
        tempCi.setNiceName(request.getNiceName());
        if (StringUtils.isEmpty(tempCi.getNiceName())) {
            return collectorService.createCollectorItem(tempCi);
        }
        return collectorService.createCollectorItemByNiceNameAndJobName(tempCi, request.getTestJobName());
    }


    private CollectorItem createGenericCollectorItem(Collector collector, PerfTestDataCreateRequest request) {
        CollectorItem tempCi = new CollectorItem();
        tempCi.setCollectorId(collector.getId());
        tempCi.setDescription(request.getPerfTool()+" : "+request.getTestName());
        tempCi.setPushed(true);
        tempCi.setLastUpdated(System.currentTimeMillis());
        Map<String, Object> option = new HashMap<>();
        option.put("jobName", request.getTestName());
        option.put("instanceUrl", request.getInstanceUrl());
        option.put("testType",request.getType());
        tempCi.getOptions().putAll(option);
        tempCi.setNiceName(request.getPerfTool());
        return collectorService.createCollectorItem(tempCi);
    }


    private Collector createGenericCollector(TestCreateRequest request, String name) {
        CollectorRequest collectorReq = new CollectorRequest();
        collectorReq.setName(name);
        collectorReq.setCollectorType(CollectorType.Test);
        Collector col = collectorReq.toCollector();
        col.setEnabled(true);
        col.setOnline(true);
        col.setLastExecuted(System.currentTimeMillis());
        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put("jobUrl", "");
        allOptions.put("jobName","");
        allOptions.put("instanceUrl", "");
        allOptions.put("testType","");
        col.setAllFields(allOptions);
        col.setUniqueFields(allOptions);
        return collectorService.createCollector(col);
    }

    private Map<String, String> getJobNameAndInstanceUrl(String jobUrl){
        Map<String ,String> map = new HashMap<>();
        if(StringUtils.isNotEmpty(jobUrl)){
            Pattern pattern = Pattern.compile("(.*?://)([^:^/]*)?(.*)?");
            Matcher matcher = pattern.matcher(jobUrl);
            matcher.find();
            String protocol = matcher.group(1);
            String domain = matcher.group(2);
            String uri = matcher.group(3);
                map.put("instanceUrl", protocol+domain);
                map.put("jobName", uri);
        }
        return map;
    }



    private CollectorItem createGenericCollectorItem(Collector collector,  TestCreateRequest request, String desc) {
        CollectorItem tempCi = new CollectorItem();
        tempCi.setCollectorId(collector.getId());
        tempCi.setDescription(desc);
        tempCi.setPushed(true);
        tempCi.setLastUpdated(System.currentTimeMillis());
        Map<String, Object> option = new HashMap<>();
        option.put("jobUrl", request.getJobUrl());
        Map<String,String> map = getJobNameAndInstanceUrl(request.getJobUrl());
        option.put("jobName",map.get("jobName"));
        option.put("instanceUrl", map.get("instanceUrl"));
        option.put("testType",request.getTestType());
        tempCi.getOptions().putAll(option);
        tempCi.setNiceName(request.getSourceFormat());
        return collectorService.createCollectorItem(tempCi);
    }



    private TestResult createTest(CollectorItem collectorItem, TestDataCreateRequest request) {
        TestResult testResult = testResultRepository.findByCollectorItemIdAndExecutionId(collectorItem.getId(),
                request.getExecutionId());
        if (testResult == null) {
            testResult = new TestResult();
        }
        testResult.setTargetAppName(request.getTargetAppName());
        testResult.setTargetEnvName(request.getTargetEnvName());
        testResult.setCollectorItemId(collectorItem.getId());
        testResult.setType(request.getType());
        testResult.setDescription(request.getDescription());
        testResult.setDuration(request.getDuration());
        testResult.setEndTime(request.getEndTime());
        testResult.setExecutionId(request.getExecutionId());
        testResult.setFailureCount(request.getFailureCount());
        testResult.setSkippedCount(request.getSkippedCount());
        testResult.setStartTime(request.getStartTime());
        testResult.setSuccessCount(request.getSuccessCount());
        if(request.getTimestamp() == 0) request.setTimestamp(System.currentTimeMillis());
        testResult.setTimestamp(request.getTimestamp());
        testResult.setTotalCount(request.getTotalCount());
        testResult.setUnknownStatusCount(request.getUnknownStatusCount());
        testResult.setUrl(request.getTestJobUrl());
        testResult.getTestCapabilities().addAll(request.getTestCapabilities());
        testResult.setBuildId(new ObjectId(request.getTestJobId()));
        testResult.setBuildArtifact(request.getBuildArtifact());
        return testResultRepository.save(testResult);
    }

    private TestResult createPerfTest(CollectorItem collectorItem, PerfTestDataCreateRequest request) {
        TestResult testResult = testResultRepository.findByCollectorItemIdAndExecutionId(collectorItem.getId(),
                request.getRunId());
        if (testResult == null) {
            testResult = new TestResult();
        }
        testResult.setTargetAppName(request.getTargetAppName());
        testResult.setTargetEnvName(request.getTargetEnvName());
        testResult.setCollectorItemId(collectorItem.getId());
        testResult.setType(request.getType());
        testResult.setDescription(request.getDescription());
        testResult.setDuration(request.getDuration());
        testResult.setEndTime(request.getEndTime());
        testResult.setExecutionId(request.getRunId());
        testResult.setFailureCount(request.getFailureCount());
        testResult.setSkippedCount(request.getSkippedCount());
        testResult.setStartTime(request.getStartTime());
        testResult.setSuccessCount(request.getSuccessCount());
        if(request.getTimestamp() == 0) request.setTimestamp(System.currentTimeMillis());
        testResult.setTimestamp(request.getTimestamp());
        testResult.setTotalCount(request.getTotalCount());
        testResult.setUnknownStatusCount(request.getUnknownStatusCount());
        testResult.setUrl(request.getReportUrl());
        testResult.getTestCapabilities().addAll(request.getTestCapabilities());
        testResult.setDescription(request.getDescription());
        testResult.setResultStatus(request.getResultStatus());
        testResult.setBuildArtifact(request.getBuildArtifact());
        testResult.setPerfRisk(request.getPerfRisk());
        return testResultRepository.save(testResult);
    }




    private TestResult createTestCucumber(CollectorItem collectorItem, CucumberJsonReport.Feature cucumberReport, TestSuiteType type, TestCreateRequest request, TestCapability cucumberTestCapabilityTransformer) {

        TestResult  testResult = new TestResult();
        Collection<TestCapability> testCapabilities = new ArrayList();
        testCapabilities.add(cucumberTestCapabilityTransformer);
        testResult.setTestCapabilities(testCapabilities);
        testResult.setType(type);
        testResult.setCollectorItemId(collectorItem.getId());
        testResult.setDescription(cucumberReport.getName());
        testResult.setTestCapabilities(testCapabilities);
        testResult.setType(type);
        testResult.setCollectorItemId(collectorItem.getId());
        testResult.setTargetEnvName(getConfigurationItem(request.getConfigurationItem(),request.getTargetAppName()));
        testResult.setTargetAppName(request.getTargetAppName());
        testResult.setDuration(cucumberTestCapabilityTransformer.getDuration());
        testResult.setFailureCount(cucumberTestCapabilityTransformer.getFailedTestSuiteCount());
        testResult.setSuccessCount(cucumberTestCapabilityTransformer.getSuccessTestSuiteCount());
        testResult.setSkippedCount(cucumberTestCapabilityTransformer.getSkippedTestSuiteCount());
        testResult.setTotalCount(cucumberTestCapabilityTransformer.getTotalTestSuiteCount());
        testResult.setUnknownStatusCount(cucumberTestCapabilityTransformer.getUnknownStatusTestSuiteCount());
        testResult.setTimestamp(convertTimestamp(request.getTimeStamp()));
        testResult.getTestCapabilities().addAll(testCapabilities);
        TestResult result = testResultRepository.save(testResult);
        return result;
    }


    private TestResult createTestJunit(CollectorItem collectorItem, JunitXmlReport junitXmlReport, TestSuiteType type, TestCreateRequest request, TestCapability testCapability) {

        TestResult  testResult = new TestResult();
        Collection<TestCapability> testCapabilities = new ArrayList();
        testCapabilities.add(testCapability);
        testResult.setTestCapabilities(testCapabilities);
        testResult.setType(type);
        testResult.setCollectorItemId(collectorItem.getId());
        testResult.setDescription(junitXmlReport.getName());
        testResult.setTargetEnvName(getConfigurationItem(request.getConfigurationItem(),request.getTargetAppName()));
        testResult.setTargetAppName((request.getTargetAppName()));
        testResult.setDuration(junitXmlReport.getTime().longValue());
        testResult.setFailureCount(junitXmlReport.getFailures());
        testResult.setSuccessCount(testCapability.getSuccessTestSuiteCount());
        testResult.setSkippedCount(StringUtils.isNotEmpty(junitXmlReport.getSkipped())? Integer.parseInt(junitXmlReport.getSkipped()):StringUtils.isNotEmpty(junitXmlReport.getSkips())? Integer.parseInt(junitXmlReport.getSkips()): 0);
        testResult.setTotalCount(junitXmlReport.getTests());
        testResult.setUnknownStatusCount(testCapability.getUnknownStatusTestSuiteCount());
        testResult.setTimestamp(convertTimestamp(request.getTimeStamp()));
        testResult.getTestCapabilities().addAll(testCapabilities);
        TestResult result = testResultRepository.save(testResult);
        return result;
    }



    private String getConfigurationItem(String configurationItem, String targetAppName){

        if (StringUtils.isNotBlank(configurationItem))
        {
            return configurationItem;
        }
        else if (StringUtils.isNotBlank(targetAppName) ){
           List<Cmdb> cmdb = cmdbService.commonNameByConfigurationItem(targetAppName);
           if(cmdb.size() > 0){
               return cmdb.get(0).getConfigurationItem();
           }
        }

        return null;
    }

    private void validConfigurationItem(String configurationItem, String targetAppName) throws HygieiaException{

        if (StringUtils.isBlank(configurationItem))
        {
            if (StringUtils.isBlank(targetAppName)){
                throw new HygieiaException("targetAppName should not be null.",HygieiaException.BAD_DATA);

            }
        }
    }

    private long convertTimestamp (String timestamp){

        long time = 0;

        if(StringUtils.isNotEmpty(timestamp)){
          time = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS Z").parseMillis(timestamp);

        }else{
            time = System.currentTimeMillis();
        }


        return time;
    }

}
