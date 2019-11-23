package com.capitalone.dashboard.model;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;

public class PrefTestCreateRequest {


    @NotNull
    private String runId;
    @NotNull
    private String testName;
    @NotNull
    private String perfTool;
    @NotNull
    private TestSuiteType type;
    private TestPerformance testPerformance;
    private String resultStatus;
    private String reportUrl;
    private long timestamp;
    private String description;
    private long startTime;
    private long endTime;
    private long duration;
    private int failureCount;
    private int successCount;
    private int skippedCount;
    private int unknownStatusCount;
    private int totalCount;
    private String targetAppName;
    private String targetEnvName;
    private Object buildArtifact;
    private String perfRisk;
    private Collection<TestCapability> testCapabilities = new ArrayList();
    private String instanceUrl;
    private String line;
    private TestCucumber.Elements[] elements;
    private String name;
    private String id;
    private String keyword;
    private String uri;
    private String buildJobId;
    private String applicationName;
    private String bapComponentName;
    private TestPerformance.PerformanceMetrics performanceMetrics;
    private String testType;
    private String testId;
    private String testAgentType;
    private String componentName;
    private String status;
    private String testRequestId;

    public TestPerformance.PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }


    public PrefTestCreateRequest() {
    }

    public String getRunId() {
        return this.runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getTestName() {
        return this.testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getResultStatus() {
        return this.resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getReportUrl() {
        return this.reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public String getPerfTool() {
        return this.perfTool;
    }

    public void setPerfTool(String perfTool) {
        this.perfTool = perfTool;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getFailureCount() {
        return this.failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public int getSuccessCount() {
        return this.successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getSkippedCount() {
        return this.skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getTotalCount() {
        return this.totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getUnknownStatusCount() {
        return this.unknownStatusCount;
    }

    public void setUnknownStatusCount(int unknownStatusCount) {
        this.unknownStatusCount = unknownStatusCount;
    }

    public TestSuiteType getType() {
        return this.type;
    }

    public void setType(TestSuiteType type) {
        this.type = type;
    }

    public String getTargetAppName() {
        return this.targetAppName;
    }

    public void setTargetAppName(String targetAppName) {
        this.targetAppName = targetAppName;
    }

    public String getTargetEnvName() {
        return this.targetEnvName;
    }

    public void setTargetEnvName(String targetEnvName) {
        this.targetEnvName = targetEnvName;
    }

    public Collection<TestCapability> getTestCapabilities() {
        return this.testCapabilities;
    }

    public void setTestCapabilities(Collection<TestCapability> testCapabilities) {
        this.testCapabilities = testCapabilities;
    }

    public String getInstanceUrl() {
        return this.instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public Object getBuildArtifact() {
        return this.buildArtifact;
    }

    public void setBuildArtifact(Object buildArtifact) {
        this.buildArtifact = buildArtifact;
    }

    public String getPerfRisk() {
        return this.perfRisk;
    }

    public void setPerfRisk(String perfRisk) {
        this.perfRisk = perfRisk;
    }

    public TestPerformance getTestPerformance() {
        return testPerformance;
    }

    public void setTestPerformance(TestPerformance testPerformance) {
        this.testPerformance = testPerformance;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public TestCucumber.Elements[] getElements() {
        return elements;
    }

    public void setElements(TestCucumber.Elements[] elements) {
        this.elements = elements;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getBuildJobId() {
        return buildJobId;
    }

    public void setBuildJobId(String buildJobId) {
        this.buildJobId = buildJobId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getBapComponentName() {
        return bapComponentName;
    }

    public void setBapComponentName(String bapComponentName) {
        this.bapComponentName = bapComponentName;
    }

    public void setPerformanceMetrics(TestPerformance.PerformanceMetrics performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestAgentType() {
        return testAgentType;
    }

    public void setTestAgentType(String testAgentType) {
        this.testAgentType = testAgentType;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTestRequestId() {
        return testRequestId;
    }

    public void setTestRequestId(String testRequestId) {
        this.testRequestId = testRequestId;
    }
}
